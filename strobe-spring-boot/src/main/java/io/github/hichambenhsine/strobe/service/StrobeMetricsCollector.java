package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeJvmMetricsDto;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.List;

/**
 * Zero-dependency JVM telemetry collector relying strictly on standard JDK MXBeans.
 * Thread-safe, O(1) execution complexity, and sanitized against uninitialized OS/JVM metrics.
 */
public class StrobeMetricsCollector {

    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;
    private final RuntimeMXBean runtimeBean;
    private final OperatingSystemMXBean osBean;
    private final List<GarbageCollectorMXBean> gcBeans;

    public StrobeMetricsCollector() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    /**
     * Captures an atomic point-in-time snapshot of JVM heap, non-heap, CPU, threads, and GC metrics.
     *
     * @return Immutable {@link StrobeJvmMetricsDto} snapshot.
     */
    public StrobeJvmMetricsDto collectSnapshot() {
        return new StrobeJvmMetricsDto(
                Instant.now(),
                runtimeBean.getUptime(),
                collectHeapMetrics(),
                collectNonHeapMetrics(),
                collectCpuMetrics(),
                collectThreadMetrics(),
                collectGcMetrics()
        );
    }

    private StrobeJvmMetricsDto.HeapMetrics collectHeapMetrics() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        long used = Math.max(0L, heap.getUsed());
        long committed = Math.max(0L, heap.getCommitted());
        long max = heap.getMax();

        double usagePercent = (max > 0L)
                ? roundTwoDecimals((used * 100.0) / max)
                : 0.0;

        return new StrobeJvmMetricsDto.HeapMetrics(used, committed, max, usagePercent);
    }

    private StrobeJvmMetricsDto.NonHeapMetrics collectNonHeapMetrics() {
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        return new StrobeJvmMetricsDto.NonHeapMetrics(
                Math.max(0L, nonHeap.getUsed()),
                Math.max(0L, nonHeap.getCommitted())
        );
    }

    private StrobeJvmMetricsDto.CpuMetrics collectCpuMetrics() {
        double processCpu = 0.0;
        double systemCpu = 0.0;

        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            processCpu = sanitizeCpuRatio(sunOsBean.getProcessCpuLoad());
            systemCpu = sanitizeCpuRatio(sunOsBean.getCpuLoad());
        }

        return new StrobeJvmMetricsDto.CpuMetrics(
                processCpu,
                systemCpu,
                osBean.getAvailableProcessors()
        );
    }

    private StrobeJvmMetricsDto.ThreadMetrics collectThreadMetrics() {
        return new StrobeJvmMetricsDto.ThreadMetrics(
                threadBean.getThreadCount(),
                threadBean.getDaemonThreadCount(),
                threadBean.getPeakThreadCount()
        );
    }

    private StrobeJvmMetricsDto.GcMetrics collectGcMetrics() {
        long totalCollections = 0L;
        long totalTimeMs = 0L;

        for (GarbageCollectorMXBean gc : gcBeans) {
            long count = gc.getCollectionCount();
            if (count > 0L) {
                totalCollections += count;
            }
            long time = gc.getCollectionTime();
            if (time > 0L) {
                totalTimeMs += time;
            }
        }

        return new StrobeJvmMetricsDto.GcMetrics(totalCollections, totalTimeMs);
    }

    private double sanitizeCpuRatio(double rawRatio) {
        if (Double.isNaN(rawRatio) || rawRatio < 0.0) {
            return 0.0;
        }
        return roundTwoDecimals(rawRatio * 100.0);
    }

    private double roundTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}