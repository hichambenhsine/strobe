package io.github.hichambenhsine.strobe.model;

/**
 * Strongly-typed contract representing live JVM metrics capability status and transport protocol.
 *
 * @param enabled Indicates whether live metrics collection and streaming endpoints are active.
 * @param provider Underlying metrics extraction provider (e.g., "jvm-mxbeans").
 * @param transport Streaming protocol exposed by the active runtime (e.g., "sse").
 */
public record StrobeMetricsStatus(
        boolean enabled,
        String provider,
        String transport
) {}