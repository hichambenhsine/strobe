package io.github.hichambenhsine.strobe.controller;

import io.github.hichambenhsine.strobe.model.StrobeJvmMetricsDto;
import io.github.hichambenhsine.strobe.service.StrobeServletMetricsStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Synchronous REST and SSE controller exposing live JVM telemetry for Spring MVC (Servlet) runtimes.
 */
@RestController
@RequestMapping("/strobe/api/metrics")
public class StrobeMetricsController {

    private final StrobeServletMetricsStreamService metricsStreamService;

    public StrobeMetricsController(StrobeServletMetricsStreamService metricsStreamService) {
        this.metricsStreamService = metricsStreamService;
    }

    @GetMapping("/snapshot")
    public StrobeJvmMetricsDto getSnapshot() {
        return metricsStreamService.getSnapshot();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMetrics() {
        return metricsStreamService.subscribe();
    }
}