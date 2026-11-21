package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeCacheEntryDto;
import io.github.hichambenhsine.strobe.model.StrobeCachePurgeResultDto;
import io.github.hichambenhsine.strobe.model.StrobeCacheScanResultDto;
import reactor.core.publisher.Mono;

/**
 * Reactive Domain Port defining non-blocking cache inspection and eviction operations.
 */
public interface StrobeReactiveCacheService {

    Mono<StrobeCacheScanResultDto> scanKeys(String pattern, String cursor, int limit);

    Mono<StrobeCacheEntryDto> getEntry(String key);

    Mono<Boolean> unlinkKey(String key);

    Mono<StrobeCachePurgeResultDto> purgeByPattern(String pattern, String cursor);
}