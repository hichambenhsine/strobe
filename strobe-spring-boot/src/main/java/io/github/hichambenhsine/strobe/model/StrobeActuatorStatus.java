package io.github.hichambenhsine.strobe.model;

/**
 * Strongly-typed contract representing Spring Boot Actuator web availability and base path.
 *
 * @param enabled Indicates whether Actuator Web is active on the same origin port as Strobe.
 * @param basePath Configured web base path for Actuator endpoints (e.g., "/actuator").
 */
public record StrobeActuatorStatus(
        boolean enabled,
        String basePath
) {}