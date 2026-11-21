package io.github.hichambenhsine.strobe.model;

/**
 * Outcome of a bounded cache eviction operation.
 *
 * @param deletedCount Number of keys unlinked during this execution batch.
 * @param pattern Target eviction pattern.
 * @param nextCursor Redis scan cursor to resume if eviction was bounded ("0" if completed).
 * @param completed True if the entire keyspace was scanned for the pattern; false if capped.
 */
public record StrobeCachePurgeResultDto(
        long deletedCount,
        String pattern,
        String nextCursor,
        boolean completed
) {}