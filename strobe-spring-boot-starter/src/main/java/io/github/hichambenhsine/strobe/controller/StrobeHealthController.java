package io.github.hichambenhsine.strobe.controller;

import io.github.hichambenhsine.strobe.model.StrobeHealthResponse;
import io.github.hichambenhsine.strobe.service.StrobeHealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing aggregated system health telemetry to the Strobe console.
 */
@RestController
@RequestMapping("/strobe/api")
public class StrobeHealthController {

    private final StrobeHealthService healthService;

    public StrobeHealthController(StrobeHealthService healthService) {
        this.healthService = healthService;
    }

    /**
     * Retrieves aggregated health diagnostics from the host application.
     *
     * @return StrobeHealthResponse containing overall status and subsystem breakdowns.
     */
    @GetMapping("/health")
    public StrobeHealthResponse getHealth() {
        return healthService.getSystemHealth();
    }
}