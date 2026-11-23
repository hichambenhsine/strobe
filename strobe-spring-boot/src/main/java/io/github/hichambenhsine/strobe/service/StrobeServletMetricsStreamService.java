package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeJvmMetricsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Shared single-publisher SSE broadcaster for Spring MVC (Servlet) runtimes.
 * <p>
 * Samples JVM metrics at a fixed 2-second interval ONLY when at least one SseEmitter client
 * is actively subscribed, preventing CPU overhead and eliminating zombie emitter memory leaks.
 * </p>
 */
public class StrobeServletMetricsStreamService implements DisposableBean {

    private static final long SAMPLE_INTERVAL_SECONDS = 2L;

    private static final Logger log = LoggerFactory.getLogger(StrobeServletMetricsStreamService.class);

    private final StrobeMetricsCollector metricsCollector;
    private final Set<SseEmitter> activeEmitters = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    public StrobeServletMetricsStreamService(StrobeMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "strobe-mvc-metrics-sampler");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Captures an immediate point-in-time metrics snapshot.
     *
     * @return Current {@link StrobeJvmMetricsDto} snapshot.
     */
    public StrobeJvmMetricsDto getSnapshot() {
        return metricsCollector.collectSnapshot();
    }

    /**
     * Registers a new SSE client and initiates shared background sampling if dormant.
     *
     * @return Configured {@link SseEmitter} with lifecycle cleanup hooks.
     */
    public SseEmitter subscribe() {
        // Infinite timeout (0L) prevents default 30s AsyncRequestTimeoutException log spam
        SseEmitter emitter = new SseEmitter(0L);

        emitter.onCompletion(() -> removeEmitter(emitter));
        emitter.onTimeout(() -> removeEmitter(emitter));
        emitter.onError(ex -> removeEmitter(emitter));

        activeEmitters.add(emitter);
        ensureSamplerStarted();

        // Push initial event immediately so HTTP stream headers flush without waiting 2 seconds
        sendSafe(emitter, metricsCollector.collectSnapshot());
        return emitter;
    }

    private synchronized void ensureSamplerStarted() {
        if ((scheduledTask == null || scheduledTask.isCancelled()) && !activeEmitters.isEmpty()) {
            scheduledTask = scheduler.scheduleAtFixedRate(
                    this::broadcastTick,
                    SAMPLE_INTERVAL_SECONDS,
                    SAMPLE_INTERVAL_SECONDS,
                    TimeUnit.SECONDS
            );
        }
    }

    private synchronized void removeEmitter(SseEmitter emitter) {
        activeEmitters.remove(emitter);
        if (activeEmitters.isEmpty() && scheduledTask != null) {
            scheduledTask.cancel(false);
            scheduledTask = null;
        }
    }

    private void broadcastTick() {
        if (activeEmitters.isEmpty()) {
            return;
        }

        try {
            StrobeJvmMetricsDto snapshot = metricsCollector.collectSnapshot();
            for (SseEmitter emitter : activeEmitters) {
                sendSafe(emitter, snapshot);
            }
        } catch (Exception ex) {
            log.warn("Failed to collect or broadcast JVM metrics tick", ex);
        }
    }

    private void sendSafe(SseEmitter emitter, StrobeJvmMetricsDto snapshot) {
        try {
            emitter.send(SseEmitter.event()
                    .name("jvm-metrics")
                    .data(snapshot));
        } catch (IOException | IllegalStateException ex) {
            log.debug("Removing disconnected SSE metrics client", ex);
            removeEmitter(emitter);
        }
    }

    @Override
    public synchronized void destroy() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        for (SseEmitter emitter : activeEmitters) {
            try {
                emitter.complete();
            } catch (Exception ex) {
                log.debug("Ignoring error while completing SSE emitter during shutdown", ex);
            }
        }
        activeEmitters.clear();
        scheduler.shutdownNow();
    }
}