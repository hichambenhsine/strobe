package io.github.hichambenhsine.strobe.model;

import java.util.List;

/**
 * Stateless cursor-paginated scan response encapsulating key discovery results.
 *
 * @param cursor Redis cursor for the next page ("0" indicates scan completion).
 * @param keys Deduplicated list of discovered key summaries.
 * @param hasMore True if additional hash slots remain to be scanned.
 */
public record StrobeCacheScanResultDto(
        String cursor,
        List<StrobeCacheKeySummaryDto> keys,
        boolean hasMore
) {}