package io.github.hichambenhsine.strobe.model;

/**
 * Strongly-typed contract representing Dynamic Gateway Route Manager runtime status.
 *
 * @param enabled Indicates whether the Spring Cloud Gateway routing engine is active.
 */
public record StrobeGatewayStatus(
        boolean enabled
) {}