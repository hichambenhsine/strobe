package io.github.hichambenhsine.strobe.model;

import java.util.Map;

/**
 * Data transfer object encapsulating aggregated health metrics from the host application.
 *
 * @param status Overall operational status (e.g., UP, DOWN).
 * @param components Subsystem health breakdown (e.g., diskSpace, ping).
 */
public record StrobeHealthResponse(
        String status,
        Map<String, Object> components
) {}
