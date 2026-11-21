package io.github.hichambenhsine.strobe.model;

/**
 * Strongly-typed contract representing cache capability status and underlying provider.
 *
 * @param enabled Indicates whether an active cache connection factory exists in the Spring context.
 * @param provider Concrete cache provider identifier (e.g., "redis").
 */
public record StrobeCacheStatus(
        boolean enabled,
        String provider
) {}