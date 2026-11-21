package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.exception.StrobeCacheUnavailableException;
import io.github.hichambenhsine.strobe.model.StrobeCacheEntryDto;
import io.github.hichambenhsine.strobe.model.StrobeCacheKeySummaryDto;
import io.github.hichambenhsine.strobe.model.StrobeCachePurgeResultDto;
import io.github.hichambenhsine.strobe.model.StrobeCacheScanResultDto;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.api.async.RedisKeyAsyncCommands;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ScanOptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * Pure non-blocking Redis adapter implementing StrobeReactiveCacheService for WebFlux runtimes.
 * Uses only public Spring Data Redis and Lettuce async CompletionStage APIs with zero thread blocking.
 */
public class StrobeReactiveRedisService implements StrobeReactiveCacheService {

    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(3);
    private static final int ENRICH_CONCURRENCY = 32;

    private final ObjectProvider<ReactiveRedisConnectionFactory> reactiveFactoryProvider;
    private final RedisPayloadSanitizer payloadSanitizer;
    private final StrobeCacheValidator validator;

    public StrobeReactiveRedisService(
            ObjectProvider<ReactiveRedisConnectionFactory> reactiveFactoryProvider,
            RedisPayloadSanitizer payloadSanitizer,
            StrobeCacheValidator validator) {
        this.reactiveFactoryProvider = reactiveFactoryProvider;
        this.payloadSanitizer = payloadSanitizer;
        this.validator = validator;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<StrobeCacheScanResultDto> scanKeys(String pattern, String cursor, int limit) {
        String safePattern;
        String startCursor;
        int safeLimit;
        try {
            safePattern = validator.normalizePattern(pattern);
            startCursor = validator.validateCursor(cursor);
            safeLimit = validator.clampScanLimit(limit);
        } catch (IllegalArgumentException ex) {
            return Mono.error(ex);
        }

        ReactiveRedisConnectionFactory reactiveFactory = requireReactiveFactory();
        if (!(reactiveFactory instanceof RedisConnectionFactory syncCapableFactory)) {
            return Mono.error(new StrobeCacheUnavailableException(
                    "Active ReactiveRedisConnectionFactory does not expose native Lettuce async commands"));
        }

        ScanArgs scanArgs = ScanArgs.Builder.matches(safePattern).limit(safeLimit);

        return Mono.usingWhen(
                Mono.fromSupplier(syncCapableFactory::getConnection),
                conn -> {
                    Object nativeConn = conn.getNativeConnection();
                    if (!(nativeConn instanceof RedisKeyAsyncCommands<?, ?> rawCommands)) {
                        return Mono.error(new StrobeCacheUnavailableException(
                                "Active Redis driver does not support Lettuce RedisKeyAsyncCommands"));
                    }
                    RedisKeyAsyncCommands<byte[], byte[]> asyncCommands =
                            (RedisKeyAsyncCommands<byte[], byte[]>) rawCommands;
                    Map<String, byte[]> deduplicatedKeys = new LinkedHashMap<>();
                    return runScanSteps(asyncCommands, startCursor, scanArgs, safeLimit, 0, deduplicatedKeys);
                },
                conn -> Mono.fromRunnable(conn::close)
        ).flatMap(acc -> enrichKeysReactively(reactiveFactory, acc.keys())
                .map(summaries -> new StrobeCacheScanResultDto(acc.cursor(), summaries, !acc.finished())));
    }

    private Mono<ScanAccumulator> runScanSteps(
            RedisKeyAsyncCommands<byte[], byte[]> asyncCommands,
            String currentCursor,
            ScanArgs scanArgs,
            int safeLimit,
            int step,
            Map<String, byte[]> deduplicatedKeys) {

        return Mono.fromCompletionStage(() -> asyncCommands.scan(ScanCursor.of(currentCursor), scanArgs))
                .timeout(COMMAND_TIMEOUT)
                .onErrorMap(TimeoutException.class,
                        ex -> new StrobeCacheUnavailableException("Redis reactive SCAN timed out", ex))
                .flatMap(scanResult -> {
                    if (scanResult.getKeys() != null) {
                        for (byte[] keyBytes : scanResult.getKeys()) {
                            String keyStr = new String(keyBytes, StandardCharsets.UTF_8);
                            deduplicatedKeys.putIfAbsent(keyStr, keyBytes);
                        }
                    }

                    String nextCursor = scanResult.getCursor();
                    boolean finished = scanResult.isFinished();
                    int nextStep = step + 1;

                    if (finished || deduplicatedKeys.size() >= safeLimit
                            || nextStep >= StrobeCacheValidator.MAX_SCAN_STEPS) {
                        return Mono.just(new ScanAccumulator(
                                finished ? "0" : nextCursor, finished, deduplicatedKeys));
                    }

                    return runScanSteps(asyncCommands, nextCursor, scanArgs, safeLimit, nextStep, deduplicatedKeys);
                });
    }

    private Mono<List<StrobeCacheKeySummaryDto>> enrichKeysReactively(
            ReactiveRedisConnectionFactory factory,
            Map<String, byte[]> keysMap) {

        if (keysMap.isEmpty()) {
            return Mono.just(List.of());
        }

        return Mono.usingWhen(
                Mono.fromSupplier(factory::getReactiveConnection),
                conn -> Flux.fromIterable(keysMap.entrySet())
                        .flatMapSequential(entry -> {
                            ByteBuffer keyBuf = ByteBuffer.wrap(entry.getValue());
                            return Mono.zip(
                                    conn.keyCommands().type(keyBuf.duplicate()).defaultIfEmpty(DataType.NONE),
                                    conn.keyCommands().ttl(keyBuf.duplicate()).defaultIfEmpty(-1L)
                            ).map(tuple -> {
                                DataType type = tuple.getT1();
                                long ttl = tuple.getT2() != null ? tuple.getT2() : -1L;
                                if (type == null || type == DataType.NONE || ttl == -2L) {
                                    return null;
                                }
                                return new StrobeCacheKeySummaryDto(entry.getKey(), type.name(), ttl);
                            });
                        }, ENRICH_CONCURRENCY)
                        .filter(Objects::nonNull)
                        .collectList(),
                ReactiveRedisConnection::closeLater
        );
    }

    @Override
    public Mono<StrobeCacheEntryDto> getEntry(String key) {
        try {
            validator.validateKey(key);
        } catch (IllegalArgumentException ex) {
            return Mono.error(ex);
        }

        ReactiveRedisConnectionFactory factory = requireReactiveFactory();
        ByteBuffer keyBuffer = ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8));

        return Mono.usingWhen(
                Mono.fromSupplier(factory::getReactiveConnection),
                conn -> Mono.zip(
                        conn.keyCommands().type(keyBuffer.duplicate()).defaultIfEmpty(DataType.NONE),
                        conn.keyCommands().ttl(keyBuffer.duplicate()).defaultIfEmpty(-1L)
                ).flatMap(tuple -> {
                    DataType type = tuple.getT1();
                    long ttl = tuple.getT2() != null ? tuple.getT2() : -1L;

                    if (type == null || type == DataType.NONE || ttl == -2L) {
                        return Mono.empty();
                    }

                    return switch (type) {
                        case STRING -> readStringEntry(conn, key, keyBuffer.duplicate(), ttl);
                        case HASH -> readHashEntry(conn, key, keyBuffer.duplicate(), ttl);
                        case LIST -> readListEntry(conn, key, keyBuffer.duplicate(), ttl);
                        case SET -> readSetEntry(conn, key, keyBuffer.duplicate(), ttl);
                        case ZSET -> readZSetEntry(conn, key, keyBuffer.duplicate(), ttl);
                        default -> Mono.just(new StrobeCacheEntryDto(
                                key, type.name(), ttl, "UNSUPPORTED", 0L, false,
                                "[Unsupported Redis data structure: " + type.name() + "]"
                        ));
                    };
                }),
                ReactiveRedisConnection::closeLater
        );
    }

    @Override
    public Mono<Boolean> unlinkKey(String key) {
        try {
            validator.validateKey(key);
        } catch (IllegalArgumentException ex) {
            return Mono.error(ex);
        }

        ReactiveRedisConnectionFactory factory = requireReactiveFactory();
        ByteBuffer keyBuffer = ByteBuffer.wrap(key.getBytes(StandardCharsets.UTF_8));

        return Mono.usingWhen(
                Mono.fromSupplier(factory::getReactiveConnection),
                conn -> conn.keyCommands().unlink(keyBuffer)
                        .map(count -> count != null && count > 0),
                ReactiveRedisConnection::closeLater
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<StrobeCachePurgeResultDto> purgeByPattern(String pattern, String cursor) {
        String startCursor;
        try {
            validator.validatePurgePattern(pattern);
            startCursor = validator.validateCursor(cursor);
        } catch (IllegalArgumentException ex) {
            return Mono.error(ex);
        }

        String cleanPattern = pattern.trim();
        ReactiveRedisConnectionFactory reactiveFactory = requireReactiveFactory();
        if (!(reactiveFactory instanceof RedisConnectionFactory syncCapableFactory)) {
            return Mono.error(new StrobeCacheUnavailableException(
                    "Active ReactiveRedisConnectionFactory does not expose native Lettuce async commands"));
        }

        ScanArgs scanArgs = ScanArgs.Builder.matches(cleanPattern)
                .limit(StrobeCacheValidator.PURGE_SCAN_BATCH_SIZE);

        return Mono.usingWhen(
                Mono.fromSupplier(syncCapableFactory::getConnection),
                conn -> {
                    Object nativeConn = conn.getNativeConnection();
                    if (!(nativeConn instanceof RedisKeyAsyncCommands<?, ?> rawCommands)) {
                        return Mono.error(new StrobeCacheUnavailableException(
                                "Active Redis driver does not support Lettuce RedisKeyAsyncCommands"));
                    }
                    RedisKeyAsyncCommands<byte[], byte[]> asyncCommands =
                            (RedisKeyAsyncCommands<byte[], byte[]>) rawCommands;
                    return runPurgeSteps(asyncCommands, cleanPattern, startCursor, scanArgs, 0, 0L);
                },
                conn -> Mono.fromRunnable(conn::close)
        );
    }

    private Mono<StrobeCachePurgeResultDto> runPurgeSteps(
            RedisKeyAsyncCommands<byte[], byte[]> asyncCommands,
            String cleanPattern,
            String currentCursor,
            ScanArgs scanArgs,
            int step,
            long accumulatedDeleted) {

        return Mono.fromCompletionStage(() -> asyncCommands.scan(ScanCursor.of(currentCursor), scanArgs))
                .timeout(COMMAND_TIMEOUT)
                .onErrorMap(TimeoutException.class,
                        ex -> new StrobeCacheUnavailableException("Redis reactive purge SCAN timed out", ex))
                .flatMap(scanResult -> {
                    List<byte[]> keys = scanResult.getKeys();
                    Mono<Long> unlinkMono;

                    if (keys != null && !keys.isEmpty()) {
                        byte[][] batch = keys.toArray(new byte[0][]);
                        unlinkMono = Mono.fromCompletionStage(() -> asyncCommands.unlink(batch))
                                .timeout(COMMAND_TIMEOUT)
                                .onErrorMap(TimeoutException.class,
                                        ex -> new StrobeCacheUnavailableException("Redis reactive UNLINK timed out", ex))
                                .defaultIfEmpty(0L);
                    } else {
                        unlinkMono = Mono.just(0L);
                    }

                    return unlinkMono.flatMap(deletedInStep -> {
                        long totalDeleted = accumulatedDeleted + deletedInStep;
                        String nextCursor = scanResult.getCursor();
                        boolean finished = scanResult.isFinished();
                        int nextStep = step + 1;

                        if (finished || totalDeleted >= StrobeCacheValidator.MAX_PURGE_LIMIT
                                || nextStep >= StrobeCacheValidator.MAX_PURGE_STEPS) {
                            return Mono.just(new StrobeCachePurgeResultDto(
                                    totalDeleted,
                                    cleanPattern,
                                    finished ? "0" : nextCursor,
                                    finished
                            ));
                        }

                        return runPurgeSteps(asyncCommands, cleanPattern, nextCursor, scanArgs, nextStep, totalDeleted);
                    });
                });
    }

    private Mono<StrobeCacheEntryDto> readStringEntry(ReactiveRedisConnection conn, String key, ByteBuffer keyBuf, long ttl) {
        return conn.stringCommands().get(keyBuf)
                .map(this::toBytes)
                .defaultIfEmpty(new byte[0])
                .map(rawBytes -> {
                    RedisPayloadSanitizer.SanitizedPayload sanitized = payloadSanitizer.sanitize(rawBytes);
                    return new StrobeCacheEntryDto(
                            key, DataType.STRING.name(), ttl,
                            sanitized.encoding(), sanitized.sizeBytes(), sanitized.truncated(), sanitized.content()
                    );
                });
    }

    private Mono<StrobeCacheEntryDto> readHashEntry(ReactiveRedisConnection conn, String key, ByteBuffer keyBuf, long ttl) {
        int sampleCap = StrobeCacheValidator.MAX_COLLECTION_SAMPLE;
        return conn.hashCommands()
                .hScan(keyBuf, ScanOptions.scanOptions().count(sampleCap).build())
                .take(sampleCap + 1)
                .collectList()
                .map(entries -> {
                    Map<String, String> sample = new LinkedHashMap<>();
                    long totalBytes = 0;
                    boolean truncated = entries.size() > sampleCap;

                    int limit = Math.min(entries.size(), sampleCap);
                    for (int i = 0; i < limit; i++) {
                        Map.Entry<ByteBuffer, ByteBuffer> entry = entries.get(i);
                        String field = new String(toBytes(entry.getKey()), StandardCharsets.UTF_8);
                        RedisPayloadSanitizer.SanitizedPayload val = payloadSanitizer.sanitize(toBytes(entry.getValue()));
                        totalBytes += val.sizeBytes();
                        if (val.truncated()) {
                            truncated = true;
                        }
                        sample.put(field, val.content());
                    }

                    return new StrobeCacheEntryDto(key, DataType.HASH.name(), ttl, "STRUCTURED_HASH", totalBytes, truncated, sample);
                });
    }

    private Mono<StrobeCacheEntryDto> readListEntry(ReactiveRedisConnection conn, String key, ByteBuffer keyBuf, long ttl) {
        int sampleCap = StrobeCacheValidator.MAX_COLLECTION_SAMPLE;
        return conn.listCommands()
                .lRange(keyBuf, 0, sampleCap)
                .collectList()
                .map(rawItems -> {
                    List<String> items = new ArrayList<>();
                    long totalBytes = 0;
                    boolean truncated = rawItems.size() > sampleCap;

                    int limit = Math.min(rawItems.size(), sampleCap);
                    for (int i = 0; i < limit; i++) {
                        RedisPayloadSanitizer.SanitizedPayload val = payloadSanitizer.sanitize(toBytes(rawItems.get(i)));
                        totalBytes += val.sizeBytes();
                        if (val.truncated()) {
                            truncated = true;
                        }
                        items.add(val.content());
                    }

                    return new StrobeCacheEntryDto(key, DataType.LIST.name(), ttl, "STRUCTURED_LIST", totalBytes, truncated, items);
                });
    }

    private Mono<StrobeCacheEntryDto> readSetEntry(ReactiveRedisConnection conn, String key, ByteBuffer keyBuf, long ttl) {
        int sampleCap = StrobeCacheValidator.MAX_COLLECTION_SAMPLE;
        return conn.setCommands()
                .sScan(keyBuf, ScanOptions.scanOptions().count(sampleCap).build())
                .take(sampleCap + 1)
                .collectList()
                .map(rawItems -> {
                    List<String> items = new ArrayList<>();
                    long totalBytes = 0;
                    boolean truncated = rawItems.size() > sampleCap;

                    int limit = Math.min(rawItems.size(), sampleCap);
                    for (int i = 0; i < limit; i++) {
                        RedisPayloadSanitizer.SanitizedPayload val = payloadSanitizer.sanitize(toBytes(rawItems.get(i)));
                        totalBytes += val.sizeBytes();
                        if (val.truncated()) {
                            truncated = true;
                        }
                        items.add(val.content());
                    }

                    return new StrobeCacheEntryDto(key, DataType.SET.name(), ttl, "STRUCTURED_SET", totalBytes, truncated, items);
                });
    }

    private Mono<StrobeCacheEntryDto> readZSetEntry(ReactiveRedisConnection conn, String key, ByteBuffer keyBuf, long ttl) {
        int sampleCap = StrobeCacheValidator.MAX_COLLECTION_SAMPLE;
        return conn.zSetCommands()
                .zRangeWithScores(keyBuf, Range.closed(0L, (long) sampleCap))
                .collectList()
                .map(rawTuples -> {
                    List<Map<String, Object>> items = new ArrayList<>();
                    long totalBytes = 0;
                    boolean truncated = rawTuples.size() > sampleCap;

                    int limit = Math.min(rawTuples.size(), sampleCap);
                    for (int i = 0; i < limit; i++) {
                        var tuple = rawTuples.get(i);
                        RedisPayloadSanitizer.SanitizedPayload val = payloadSanitizer.sanitize(tuple.getValue());
                        totalBytes += val.sizeBytes();
                        if (val.truncated()) {
                            truncated = true;
                        }
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("score", tuple.getScore() != null ? tuple.getScore() : 0.0);
                        entry.put("value", val.content());
                        items.add(entry);
                    }

                    return new StrobeCacheEntryDto(key, DataType.ZSET.name(), ttl, "STRUCTURED_ZSET", totalBytes, truncated, items);
                });
    }

    private ReactiveRedisConnectionFactory requireReactiveFactory() {
        ReactiveRedisConnectionFactory factory = reactiveFactoryProvider.getIfAvailable();
        if (factory == null) {
            throw new StrobeCacheUnavailableException("No active ReactiveRedisConnectionFactory available in application context");
        }
        return factory;
    }

    private byte[] toBytes(ByteBuffer buffer) {
        if (buffer == null) {
            return new byte[0];
        }
        ByteBuffer duplicate = buffer.duplicate();
        byte[] bytes = new byte[duplicate.remaining()];
        duplicate.get(bytes);
        return bytes;
    }

    private record ScanAccumulator(String cursor, boolean finished, Map<String, byte[]> keys) {}
}