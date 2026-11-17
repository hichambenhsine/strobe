package io.github.hichambenhsine.strobe.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.hichambenhsine.strobe.model.StrobeCircuitBreakerDto;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service inspecting Resilience4j CircuitBreakerRegistry instances in memory.
 * Encapsulates sliding window metrics extraction and runtime state control.
 */
public class StrobeResilienceService {

    private final ObjectProvider<CircuitBreakerRegistry> registryProvider;
    private final Environment environment;

    public StrobeResilienceService(ObjectProvider<CircuitBreakerRegistry> registryProvider, Environment environment) {
        this.registryProvider = registryProvider;
        this.environment = environment;
    }

    /**
     * Inspects local in-memory circuit breakers and projects them into extensible DTOs.
     *
     * @return List of StrobeCircuitBreakerDto objects representing local circuit breakers.
     */
    public List<StrobeCircuitBreakerDto> getLocalCircuitBreakers() {
        List<StrobeCircuitBreakerDto> results = new ArrayList<>();
        CircuitBreakerRegistry registry = registryProvider.getIfAvailable();

        if (registry == null) {
            return results;
        }

        for (CircuitBreaker cb : registry.getAllCircuitBreakers()) {
            results.add(mapToDto(cb));
        }

        return results;
    }

    /**
     * Resets a target circuit breaker, clearing its sliding window metrics and restoring CLOSED state.
     *
     * @param name Unique circuit breaker identifier.
     * @return Optional containing updated StrobeCircuitBreakerDto, or empty if breaker does not exist.
     */
    public Optional<StrobeCircuitBreakerDto> resetCircuitBreaker(String name) {
        CircuitBreakerRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return Optional.empty();
        }

        return registry.find(name).map(cb -> {
            cb.reset();
            return mapToDto(cb);
        });
    }

    /**
     * Forces a manual state transition on a target circuit breaker.
     *
     * @param name Unique circuit breaker identifier.
     * @param targetState Explicit state target (CLOSED, FORCED_OPEN, DISABLED).
     * @return Optional containing updated StrobeCircuitBreakerDto, or empty if breaker does not exist.
     */
    public Optional<StrobeCircuitBreakerDto> transitionCircuitBreakerState(String name, String targetState) {
        CircuitBreakerRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return Optional.empty();
        }

        return registry.find(name).map(cb -> {
            switch (targetState.toUpperCase()) {
                case "CLOSED" -> cb.transitionToClosedState();
                case "FORCED_OPEN" -> cb.transitionToForcedOpenState();
                case "DISABLED" -> cb.transitionToDisabledState();
                default -> throw new IllegalArgumentException("Unsupported circuit breaker state: " + targetState);
            }
            return mapToDto(cb);
        });
    }

    /**
     * Maps a native Resilience4j CircuitBreaker instance to the Strobe extensible DTO.
     * Sanitizes floating-point edge cases (such as NaN when sliding windows are empty).
     */
    private StrobeCircuitBreakerDto mapToDto(CircuitBreaker cb) {
        String applicationName = environment.getProperty("spring.application.name", "local-service");
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        // Sanitize edge cases where sliding window has no data yet (returns Float.NaN or -1.0)
        float failureRate = metrics.getFailureRate();
        if (Float.isNaN(failureRate) || failureRate < 0f) {
            failureRate = 0.0f;
        }

        float slowCallRate = metrics.getSlowCallRate();
        if (Float.isNaN(slowCallRate) || slowCallRate < 0f) {
            slowCallRate = 0.0f;
        }

        Map<String, Object> metricsMap = new HashMap<>();
        metricsMap.put("failureRate", failureRate);
        metricsMap.put("slowCallRate", slowCallRate);
        metricsMap.put("bufferedCalls", metrics.getNumberOfBufferedCalls());
        metricsMap.put("failedCalls", metrics.getNumberOfFailedCalls());
        metricsMap.put("slowCalls", metrics.getNumberOfSlowCalls());

        return new StrobeCircuitBreakerDto(
                applicationName,
                cb.getName(),
                cb.getState().name(),
                metricsMap
        );
    }
}