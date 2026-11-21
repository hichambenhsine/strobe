package io.github.hichambenhsine.strobe.model;

/**
 * Lightweight summary of a cache key discovered during cursor-based scanning.
 *
 * @param key Raw key identifier.
 * @param type Underlying datastore structure (e.g., STRING, HASH, LIST, SET, ZSET).
 * @param ttlSeconds Remaining time-to-live in seconds (-1 if persistent, -2 if expired/missing).
 */
public record StrobeCacheKeySummaryDto(
        String key,
        String type,
        long ttlSeconds
) {}