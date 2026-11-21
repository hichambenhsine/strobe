package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeCachePurgeResultDto;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.async.RedisKeyAsyncCommands;
import io.github.hichambenhsine.strobe.exception.StrobeCacheUnavailableException;
import io.github.hichambenhsine.strobe.model.StrobeCacheEntryDto;
import io.github.hichambenhsine.strobe.model.StrobeCacheKeySummaryDto;
import io.github.hichambenhsine.strobe.model.StrobeCacheScanResultDto;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Synchronous Redis adapter implementing StrobeCacheService for Servlet runtimes.
 * Enforces O(1) stateless cursor pagination, pipelined metadata, and safe multi-type entry reads.
 */
public class StrobeRedisService implements StrobeCacheService {

    private static final long SCAN_TIMEOUT_SECONDS = 3L;

    private final ObjectProvider<RedisConnectionFactory> connectionFactoryProvider;
    private final RedisPayloadSanitizer payloadSanitizer;
    private final StrobeCacheValidator validator;

    public StrobeRedisService(
            ObjectProvider<RedisConnectionFactory> connectionFactoryProvider,
            RedisPayloadSanitizer payloadSanitizer,
            StrobeCacheValidator validator) {
        this.connectionFactoryProvider = connectionFactoryProvider;
        this.payloadSanitizer = payloadSanitizer;
        this.validator = validator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public StrobeCacheScanResultDto scanKeys(String pattern, String cursor, int limit) {
        RedisConnectionFactory factory = requireConnectionFactory();
        String safePattern = validator.normalizePattern(pattern); //(pattern == null || pattern.isBlank()) ? "*" : pattern.trim();
        String currentCursor =  validator.validateCursor(cursor); //validateCursor(cursor);
        int safeLimit = validator.clampScanLimit(limit); //Math.max(1, Math.min(limit, MAX_SCAN_LIMIT));

        Map<String, byte[]> deduplicatedKeys = new LinkedHashMap<>();
        boolean finished = false;
        int steps = 0;

        try (RedisConnection connection = factory.getConnection()) {
            Object nativeConn = connection.getNativeConnection();
            if (!(nativeConn instanceof RedisKeyAsyncCommands<?, ?> rawCommands)) {
                throw new StrobeCacheUnavailableException("Active Redis driver does not support Lettuce KeyScanCursor");
            }

            RedisKeyAsyncCommands<byte[], byte[]> asyncCommands = (RedisKeyAsyncCommands<byte[], byte[]>) rawCommands;
            ScanArgs scanArgs = ScanArgs.Builder.matches(safePattern).limit(safeLimit);

            do {
                KeyScanCursor<byte[]> scanResult = asyncCommands
                        .scan(ScanCursor.of(currentCursor), scanArgs)
                        .get(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (scanResult.getKeys() != null) {
                    for (byte[] keyBytes : scanResult.getKeys()) {
                        String keyStr = new String(keyBytes, StandardCharsets.UTF_8);
                        deduplicatedKeys.putIfAbsent(keyStr, keyBytes);
                    }
                }

                currentCursor = scanResult.getCursor();
                finished = scanResult.isFinished();
                steps++;
            } while (!finished && deduplicatedKeys.size() < safeLimit && steps < validator.MAX_SCAN_STEPS);

            List<byte[]> keysToEnrich = new ArrayList<>(deduplicatedKeys.values());
            List<StrobeCacheKeySummaryDto> summaries = enrichKeysWithPipeline(connection, keysToEnrich);
            return new StrobeCacheScanResultDto(finished ? "0" : currentCursor, summaries, !finished);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new StrobeCacheUnavailableException("Redis scan interrupted", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new StrobeCacheUnavailableException("Failed to execute Redis SCAN within timeout", ex);
        }
    }

    @Override
    public Optional<StrobeCacheEntryDto> getEntry(String key) {
        /*
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Cache key parameter must not be blank");
        }
        */
        validator.validateKey(key);

        RedisConnectionFactory factory = requireConnectionFactory();
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

        try (RedisConnection connection = factory.getConnection()) {
            // Fetch TYPE and TTL in a single pipeline round-trip
            connection.openPipeline();
            connection.keyCommands().type(keyBytes);
            connection.keyCommands().ttl(keyBytes);
            List<Object> metaResults = connection.closePipeline();

            if (metaResults == null || metaResults.size() < 2) {
                throw new StrobeCacheUnavailableException("Incomplete metadata response from Redis");
            }

            DataType type = (metaResults.get(0) instanceof DataType dt) ? dt : DataType.NONE;
            if (type == DataType.NONE) {
                return Optional.empty();
            }

            long ttl = (metaResults.get(1) instanceof Number num) ? num.longValue() : -1L;
            if (ttl == -2L) {
                return Optional.empty();
            }

            return switch (type) {
                case STRING -> Optional.of(readStringEntry(connection, key, keyBytes, ttl));
                case HASH -> Optional.of(readHashEntry(connection, key, keyBytes, ttl));
                case LIST -> Optional.of(readListEntry(connection, key, keyBytes, ttl));
                case SET -> Optional.of(readSetEntry(connection, key, keyBytes, ttl));
                case ZSET -> Optional.of(readZSetEntry(connection, key, keyBytes, ttl));
                default -> Optional.of(new StrobeCacheEntryDto(
                        key, type.name(), ttl, "UNSUPPORTED", 0L, false,
                        "[Unsupported Redis data structure: " + type.name() + "]"
                ));
            };
        }
    }

    @Override
    public boolean unlinkKey(String key) {
        /*
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Cache key parameter must not be blank");
        }
        */
        validator.validateKey(key);

        RedisConnectionFactory factory = requireConnectionFactory();
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);

        try (RedisConnection connection = factory.getConnection()) {
            Long unlinked = connection.keyCommands().unlink(keyBytes);
            return unlinked != null && unlinked > 0;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public StrobeCachePurgeResultDto purgeByPattern(String pattern, String cursor) {
        validator.validatePurgePattern(pattern); //validatePurgePattern(pattern);
        RedisConnectionFactory factory = requireConnectionFactory();
        String cleanPattern = validator.normalizePattern(pattern);// pattern.trim();
        String currentCursor = validator.validateCursor(cursor); // validateCursor(cursor);

        long totalDeleted = 0L;
        boolean finished = false;
        int steps = 0;

        try (RedisConnection connection = factory.getConnection()) {
            Object nativeConn = connection.getNativeConnection();
            if (!(nativeConn instanceof RedisKeyAsyncCommands<?, ?> rawCommands)) {
                throw new StrobeCacheUnavailableException("Active Redis driver does not support Lettuce KeyScanCursor");
            }

            RedisKeyAsyncCommands<byte[], byte[]> asyncCommands = (RedisKeyAsyncCommands<byte[], byte[]>) rawCommands;
            ScanArgs scanArgs = ScanArgs.Builder.matches(cleanPattern).limit(validator.PURGE_SCAN_BATCH_SIZE);

            do {
                KeyScanCursor<byte[]> scanResult = asyncCommands
                        .scan(ScanCursor.of(currentCursor), scanArgs)
                        .get(SCAN_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                List<byte[]> keys = scanResult.getKeys();
                if (keys != null && !keys.isEmpty()) {
                    byte[][] batch = keys.toArray(new byte[0][]);
                    Long unlinked = connection.keyCommands().unlink(batch);
                    if (unlinked != null) {
                        totalDeleted += unlinked;
                    }
                }

                currentCursor = scanResult.getCursor();
                finished = scanResult.isFinished();
                steps++;
            } while (!finished && totalDeleted < validator.MAX_PURGE_LIMIT && steps < validator.MAX_PURGE_STEPS);

            return new StrobeCachePurgeResultDto(
                    totalDeleted,
                    cleanPattern,
                    finished ? "0" : currentCursor,
                    finished
            );

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new StrobeCacheUnavailableException("Redis pattern purge interrupted", ex);
        } catch (ExecutionException | TimeoutException ex) {
            throw new StrobeCacheUnavailableException("Failed to execute Redis pattern purge within timeout", ex);
        }
    }

    private StrobeCacheEntryDto readStringEntry(RedisConnection conn, String key, byte[] keyBytes, long ttl) {
        byte[] rawValue = conn.stringCommands().get(keyBytes);
        RedisPayloadSanitizer.SanitizedPayload sanitized = payloadSanitizer.sanitize(rawValue);
        return new StrobeCacheEntryDto(
                key, DataType.STRING.name(), ttl,
                sanitized.encoding(), sanitized.sizeBytes(), sanitized.truncated(), sanitized.content()
        );
    }

    private StrobeCacheEntryDto readHashEntry(RedisConnection conn, String key, byte[] keyBytes, long ttl) {
        Map<String, String> sample = new LinkedHashMap<>();
        long totalBytes = 0;
        boolean truncated = false;

        try (Cursor<Map.Entry<byte[], byte[]>> cursor = conn.hashCommands()
                .hScan(keyBytes, ScanOptions.scanOptions().count(validator.MAX_COLLECTION_SAMPLE).build())) {
            while (cursor.hasNext()) {
                if (sample.size() >= validator.MAX_COLLECTION_SAMPLE) {
                    truncated = true;
                    break;
                }
                Map.Entry<byte[], byte[]> entry = cursor.next();
                String field = new String(entry.getKey(), StandardCharsets.UTF_8);
                RedisPayloadSanitizer.SanitizedPayload val = payloadSanitizer.sanitize(entry.getValue());
                totalBytes += val.sizeBytes();
                if (val.truncated()) {
                    truncated = true;
                }
                sample.put(field, val.content());
            }
        }

        return new StrobeCacheEntryDto(key, DataType.HASH.name(), ttl, "STRUCTURED_HASH", totalBytes, truncated, sample);
    }

    private StrobeCacheEntryDto readListEntry(RedisConnection conn, String key, byte[] keyBytes, long ttl) {
        List<byte[]> rawItems = conn.listCommands().lRange(keyBytes, 0, validator.MAX_COLLECTION_SAMPLE);
        List<String> items = new ArrayList<>();
        long totalBytes = 0;
        boolean truncated = false;

        if (rawItems != null) {
            if (rawItems.size() > validator.MAX_COLLECTION_SAMPLE) {
                truncated = true;
            }
            for (int i = 0; i < Math.min(rawItems.size(), validator.MAX_COLLECTION_SAMPLE); i++) {
                RedisPayloadSanitizer.SanitizedPayload val = payloadSanitizer.sanitize(rawItems.get(i));
                totalBytes += val.sizeBytes();
                if (val.truncated()) {
                    truncated = true;
                }
                items.add(val.content());
            }
        }

        return new StrobeCacheEntryDto(key, DataType.LIST.name(), ttl, "STRUCTURED_LIST", totalBytes, truncated, items);
    }

    private StrobeCacheEntryDto readSetEntry(RedisConnection conn, String key, byte[] keyBytes, long ttl) {
        List<String> items = new ArrayList<>();
        long totalBytes = 0;
        boolean truncated = false;

        try (Cursor<byte[]> cursor = conn.setCommands()
                .sScan(keyBytes, ScanOptions.scanOptions().count(validator.MAX_COLLECTION_SAMPLE).build())) {
            while (cursor.hasNext()) {
                if (items.size() >= validator.MAX_COLLECTION_SAMPLE) {
                    truncated = true;
                    break;
                }
                RedisPayloadSanitizer.SanitizedPayload val = payloadSanitizer.sanitize(cursor.next());
                totalBytes += val.sizeBytes();
                if (val.truncated()) {
                    truncated = true;
                }
                items.add(val.content());
            }
        }

        return new StrobeCacheEntryDto(key, DataType.SET.name(), ttl, "STRUCTURED_SET", totalBytes, truncated, items);
    }

    private StrobeCacheEntryDto readZSetEntry(RedisConnection conn, String key, byte[] keyBytes, long ttl) {
        var rawTuples = conn.zSetCommands().zRangeWithScores(keyBytes, 0, validator.MAX_COLLECTION_SAMPLE);
        List<Map<String, Object>> items = new ArrayList<>();
        long totalBytes = 0;
        boolean truncated = false;

        if (rawTuples != null) {
            if (rawTuples.size() > validator.MAX_COLLECTION_SAMPLE) {
                truncated = true;
            }
            int count = 0;
            for (var tuple : rawTuples) {
                if (count >= validator.MAX_COLLECTION_SAMPLE) {
                    break;
                }
                RedisPayloadSanitizer.SanitizedPayload val = payloadSanitizer.sanitize(tuple.getValue());
                totalBytes += val.sizeBytes();
                if (val.truncated()) {
                    truncated = true;
                }
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("score", tuple.getScore() != null ? tuple.getScore() : 0.0);
                entry.put("value", val.content());
                items.add(entry);
                count++;
            }
        }

        return new StrobeCacheEntryDto(key, DataType.ZSET.name(), ttl, "STRUCTURED_ZSET", totalBytes, truncated, items);
    }

    private List<StrobeCacheKeySummaryDto> enrichKeysWithPipeline(RedisConnection connection, List<byte[]> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }

        connection.openPipeline();
        for (byte[] keyBytes : keys) {
            connection.keyCommands().type(keyBytes);
            connection.keyCommands().ttl(keyBytes);
        }
        List<Object> pipelineResults = connection.closePipeline();

        if (pipelineResults == null || pipelineResults.size() < keys.size() * 2) {
            throw new StrobeCacheUnavailableException("Incomplete pipeline response received from Redis");
        }

        List<StrobeCacheKeySummaryDto> summaries = new ArrayList<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            String keyStr = new String(keys.get(i), StandardCharsets.UTF_8);

            Object rawType = pipelineResults.get(2 * i);
            String typeName;
            if (rawType instanceof DataType dt) {
                typeName = dt.name();
            } else if (rawType != null) {
                typeName = String.valueOf(rawType).toUpperCase();
            } else {
                typeName = "NONE";
            }

            Object rawTtl = pipelineResults.get(2 * i + 1);
            long ttl = (rawTtl instanceof Number num) ? num.longValue() : -1L;

            if (!"NONE".equals(typeName) && ttl != -2L) {
                summaries.add(new StrobeCacheKeySummaryDto(keyStr, typeName, ttl));
            }
        }
        return summaries;
    }

    private RedisConnectionFactory requireConnectionFactory() {
        RedisConnectionFactory factory = connectionFactoryProvider.getIfAvailable();
        if (factory == null) {
            throw new StrobeCacheUnavailableException("No active RedisConnectionFactory available in application context");
        }
        return factory;
    }

    /* DELETE THIS
    private void validatePurgePattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("Purge pattern must not be empty");
        }
        // Strip all Redis glob wildcards (*, ?, [, ]) to prevent bypassing minimum prefix length
        String stripped = pattern.replaceAll("[*?\\[\\]]", "").trim();
        if (stripped.length() < MIN_PURGE_PREFIX_LENGTH) {
            throw new IllegalArgumentException(
                    "Purge pattern is too broad; must contain at least " + MIN_PURGE_PREFIX_LENGTH + " non-wildcard characters"
            );
        }
    }
     */
}