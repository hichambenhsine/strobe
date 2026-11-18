package io.github.hichambenhsine.strobe.model;

import java.util.Map;

/**
 * Extensible root metadata DTO aggregating application info and registered module capabilities.
 *
 * @param applicationName Host Spring Boot application name.
 * @param capabilities Map of registered module statuses keyed by capability name.
 */
public record StrobeMetadataDto(
        String applicationName,
        Map<String, Object> capabilities
) {}
