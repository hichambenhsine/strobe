package io.github.hichambenhsine.strobe.model;

import java.time.Instant;

/**
 * Immutable telemetry snapshot representing live JVM resource utilization.
 *
 * @param timestamp UTC instant when the metrics sample was captured.
 * @param uptimeMs Total JVM uptime in milliseconds.
 * @param heap JVM heap memory allocations and utilization ratio.
 * @param nonHeap JVM non-heap memory allocations (metaspace, code cache).
 * @param cpu Process and system CPU load metrics.
 * @param threads Live, daemon, and peak thread counts.
 * @param gc Aggregated garbage collection counts and cumulative pause duration.
 */
public record StrobeJvmMetricsDto(
        Instant timestamp,
        long uptimeMs,
        HeapMetrics heap,
        NonHeapMetrics nonHeap,
        CpuMetrics cpu,
        ThreadMetrics threads,
        GcMetrics gc
) {

    public record HeapMetrics(
            long usedBytes,
            long committedBytes,
            long maxBytes,
            double usagePercent
    ) {}

    public record NonHeapMetrics(
            long usedBytes,
            long committedBytes
    ) {}

    public record CpuMetrics(
            double processCpuPercent,
            double systemCpuPercent,
            int availableProcessors
    ) {}

    public record ThreadMetrics(
            int liveCount,
            int daemonCount,
            int peakCount
    ) {}

    public record GcMetrics(
            long totalCollections,
            long totalTimeMs
    ) {}
}