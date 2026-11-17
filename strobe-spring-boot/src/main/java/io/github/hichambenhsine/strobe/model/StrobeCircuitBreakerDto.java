package io.github.hichambenhsine.strobe.model;

import java.util.Map;

/**
 * Extensible data transfer object representing a circuit breaker state and its runtime telemetry.
 * Adheres to the Open/Closed Principle (OCP) by exposing extensible metric payloads.
 *
 * @param serviceName Originating microservice name.
 * @param name Unique identifier of the circuit breaker.
 * @param state Current operational state (CLOSED, OPEN, HALF_OPEN, FORCED_OPEN, DISABLED).
 * @param metrics Extensible map containing sliding window metrics, allowing schema extension without code breakage.
 */
public record StrobeCircuitBreakerDto(
        String serviceName,
        String name,
        String state,
        Map<String, Object> metrics
) {}