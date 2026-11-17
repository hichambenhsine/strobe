package io.github.hichambenhsine.strobe.controller;

import io.github.hichambenhsine.strobe.model.StrobeCircuitBreakerDto;
import io.github.hichambenhsine.strobe.service.StrobeResilienceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing Resilience4j circuit breaker telemetry and runtime actions.
 * Acts as a pure HTTP transport layer.
 */
@RestController
@RequestMapping("/strobe/api/resilience")
public class StrobeResilienceController {

    private final StrobeResilienceService resilienceService;

    public StrobeResilienceController(StrobeResilienceService resilienceService) {
        this.resilienceService = resilienceService;
    }

    /**
     * Returns runtime metrics and states for all registered circuit breakers.
     *
     * @return List of StrobeCircuitBreakerDto.
     */
    @GetMapping("/circuit-breakers")
    public List<StrobeCircuitBreakerDto> getCircuitBreakers() {
        return resilienceService.getLocalCircuitBreakers();
    }

    /**
     * Resets a target circuit breaker, clearing its metrics and returning it to CLOSED.
     *
     * @param name Unique circuit breaker identifier.
     * @return ResponseEntity with updated StrobeCircuitBreakerDto or 404 if not found.
     */
    @PostMapping("/circuit-breakers/{name}/reset")
    public ResponseEntity<StrobeCircuitBreakerDto> resetCircuitBreaker(@PathVariable("name") String name) {
        return resilienceService.resetCircuitBreaker(name)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Forces a manual state transition on a target circuit breaker.
     *
     * @param name Unique circuit breaker identifier.
     * @param state Target operational state (e.g., CLOSED, FORCED_OPEN, DISABLED).
     * @return ResponseEntity with updated StrobeCircuitBreakerDto or 404 if not found.
     */
    @PostMapping("/circuit-breakers/{name}/transition")
    public ResponseEntity<StrobeCircuitBreakerDto> transitionState(
            @PathVariable("name") String name,
            @RequestParam("state") String state) {
        return resilienceService.transitionCircuitBreakerState(name, state)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}