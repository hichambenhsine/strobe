package io.github.hichambenhsine.strobe.model;

/**
 * Strongly-typed contract representing resilience telemetry status and underlying provider.
 *
 * @param enabled Indicates whether a circuit breaker registry is active in the runtime context.
 * @param provider Concrete resilience engine implementation identifier (e.g., "resilience4j").
 */
public record StrobeResilienceStatus(
        boolean enabled,
        String provider
) {}