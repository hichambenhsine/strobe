package io.github.hichambenhsine.strobe.model;

/**
 * Strongly-typed contract representing gateway routing status and underlying provider.
 *
 * @param enabled Indicates whether the gateway routing engine is active in the runtime context.
 * @param provider Concrete gateway implementation identifier (e.g., "spring-cloud-gateway").
 */
public record StrobeGatewayStatus(
        boolean enabled,
        String provider
) {}