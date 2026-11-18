package io.github.hichambenhsine.strobe.capability;

import io.github.hichambenhsine.strobe.model.StrobeResilienceStatus;
import io.github.hichambenhsine.strobe.spi.StrobeCapabilityContributor;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Capability contributor evaluating Resilience4j circuit breaker activation at runtime.
 * Contributes under the generic "resilience" domain port while explicitly identifying the concrete provider.
 */
public class Resilience4jCapabilityContributor implements StrobeCapabilityContributor {

    private static final String PROVIDER_NAME = "resilience4j";

    private final ObjectProvider<CircuitBreakerRegistry> registryProvider;

    public Resilience4jCapabilityContributor(ObjectProvider<CircuitBreakerRegistry> registryProvider) {
        this.registryProvider = registryProvider;
    }

    @Override
    public String getCapabilityName() {
        return "resilience";
    }

    @Override
    public Object getCapabilityStatus() {
        boolean isActive = registryProvider.getIfAvailable() != null;
        return new StrobeResilienceStatus(isActive, PROVIDER_NAME);
    }
}