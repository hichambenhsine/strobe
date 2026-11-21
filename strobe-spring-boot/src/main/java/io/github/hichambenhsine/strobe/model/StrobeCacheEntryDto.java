package io.github.hichambenhsine.strobe.model;

/**
 * Detailed inspection view of a single cache entry with encoding metadata and safe truncation flags.
 *
 * @param key Cache key identifier.
 * @param type Underlying data structure (STRING, HASH, LIST, SET, ZSET).
 * @param ttlSeconds Remaining time-to-live in seconds (-1 if persistent).
 * @param encoding Detected payload encoding (JSON, UTF8_TEXT, JDK_SERIALIZED, GZIP_BINARY, BINARY, STRUCTURED_*).
 * @param sizeBytes Total payload size in bytes prior to truncation.
 * @param truncated True if the returned value was truncated due to memory safety caps.
 * @param value Safe printable value (String, Map, or List depending on Redis type).
 */
public record StrobeCacheEntryDto(
        String key,
        String type,
        long ttlSeconds,
        String encoding,
        long sizeBytes,
        boolean truncated,
        Object value
) {}