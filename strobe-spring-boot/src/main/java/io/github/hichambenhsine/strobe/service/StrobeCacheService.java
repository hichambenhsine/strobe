package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeCacheEntryDto;
import io.github.hichambenhsine.strobe.model.StrobeCachePurgeResultDto;
import io.github.hichambenhsine.strobe.model.StrobeCacheScanResultDto;

import java.util.Optional;

/**
 * Domain Port defining cache inspection operations decoupled from concrete cache providers.
 */
public interface StrobeCacheService {

    /**
     * Performs a stateless cursor-based scan of cache keys matching the requested pattern.
     *
     * @param pattern Glob-style key pattern (defaults to "*").
     * @param cursor Redis scan cursor ("0" to initiate a new scan).
     * @param limit Target number of keys to collect per request.
     * @return Paginated scan result containing key summaries and the next cursor.
     */
    StrobeCacheScanResultDto scanKeys(String pattern, String cursor, int limit);

    /**
     * Retrieves and safely decodes a single cache entry without triggering unsafe deserialization.
     *
     * @param key Exact cache key identifier.
     * @return Optional containing entry details, or empty if the key does not exist.
     */
    Optional<StrobeCacheEntryDto> getEntry(String key);

    /**
     * Asynchronously removes a single cache entry.
     *
     * @param key Exact cache key identifier.
     * @return true if the key existed and was unlinked; false otherwise.
     */
    boolean unlinkKey(String key);

    /**
     * Asynchronously purges cache entries matching a strict non-global pattern in bounded batches.
     *
     * @param pattern Prefix/glob pattern containing at least 3 non-wildcard characters.
     * @param cursor Current scan cursor ("0" to initiate purge).
     * @return Purge outcome including deleted count, next cursor, and completion flag.
     */
    StrobeCachePurgeResultDto purgeByPattern(String pattern, String cursor);
}