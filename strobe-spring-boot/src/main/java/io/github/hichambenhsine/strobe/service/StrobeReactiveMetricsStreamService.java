package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeJvmMetricsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Non-blocking hot-stream metrics broadcaster for Spring WebFlux runtimes.
 * Uses publish().refCount(1) so JVM sampling only runs while active subscribers exist.
 */
public class StrobeReactiveMetricsStreamService {

    private static final Logger log = LoggerFactory.getLogger(StrobeReactiveMetricsStreamService.class);
    private static final Duration SAMPLE_INTERVAL = Duration.ofSeconds(2);

    private final StrobeMetricsCollector metricsCollector;
    private final Flux<StrobeJvmMetricsDto> sharedStream;

    public StrobeReactiveMetricsStreamService(StrobeMetricsCollector metricsCollector) {
        this.metricsCollector = metricsCollector;
        this.sharedStream = Flux.interval(SAMPLE_INTERVAL)
                .flatMap(tick -> collectSafely())
                .publish()
                .refCount(1);
    }

    /**
     * Captures a single point-in-time metrics snapshot non-blockingly.
     *
     * @return Mono emitting the current {@link StrobeJvmMetricsDto}.
     */
    public Mono<StrobeJvmMetricsDto> getSnapshot() {
        return Mono.fromSupplier(metricsCollector::collectSnapshot);
    }

    /**
     * Subscribes to the shared live metrics stream, delivering an immediate fresh snapshot first.
     *
     * @return Hot {@link Flux} of JVM metrics snapshots.
     */
    public Flux<StrobeJvmMetricsDto> streamMetrics() {
        return Flux.concat(collectSafely(), sharedStream);
    }

    private Mono<StrobeJvmMetricsDto> collectSafely() {
        return Mono.fromCallable(metricsCollector::collectSnapshot)
                .onErrorResume(ex -> {
                    log.warn("Failed to collect JVM metrics sample in reactive stream", ex);
                    return Mono.empty();
                });
    }
}