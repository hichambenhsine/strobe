package io.github.hichambenhsine.strobe.controller;

import io.github.hichambenhsine.strobe.model.StrobeJvmMetricsDto;
import io.github.hichambenhsine.strobe.service.StrobeReactiveMetricsStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive REST and SSE controller exposing live JVM telemetry for Spring WebFlux runtimes.
 */
@RestController
@RequestMapping("/strobe/api/metrics")
public class StrobeReactiveMetricsController {

    private final StrobeReactiveMetricsStreamService reactiveMetricsStreamService;

    public StrobeReactiveMetricsController(StrobeReactiveMetricsStreamService reactiveMetricsStreamService) {
        this.reactiveMetricsStreamService = reactiveMetricsStreamService;
    }

    @GetMapping("/snapshot")
    public Mono<StrobeJvmMetricsDto> getSnapshot() {
        return reactiveMetricsStreamService.getSnapshot();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<StrobeJvmMetricsDto>> streamMetrics() {
        return reactiveMetricsStreamService.streamMetrics()
                .map(dto -> ServerSentEvent.<StrobeJvmMetricsDto>builder()
                        .event("jvm-metrics")
                        .data(dto)
                        .build());
    }
}