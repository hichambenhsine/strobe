package io.github.hichambenhsine.strobe.config;

import io.github.hichambenhsine.strobe.capability.Resilience4jCapabilityContributor;
import io.github.hichambenhsine.strobe.controller.StrobeResilienceController;
import io.github.hichambenhsine.strobe.service.StrobeResilienceService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * StrobeResilience4jAutoConfiguration
 * <p>
 * Dedicated auto-configuration provisioning Resilience4j circuit breaker inspection,
 * runtime lifecycle control endpoints, and capability discovery.
 * </p>
 * <p>
 * Guarded by string-based {@link ConditionalOnClass} on {@code CircuitBreakerRegistry} and
 * restricted to web applications ({@link ConditionalOnWebApplication}) to protect non-resilience
 * and non-web runtimes from unnecessary controller instantiation or ClassLoader failures.
 * </p>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(name = "io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry")
public class StrobeResilience4jAutoConfiguration {

    /**
     * Registers the in-memory Resilience4j inspection and runtime state transition service.
     *
     * @param registryProvider Safe provider for the host {@link CircuitBreakerRegistry}.
     * @param environment Spring environment used to resolve the originating service name.
     * @return Configured {@link StrobeResilienceService} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public StrobeResilienceService strobeResilienceService(
            ObjectProvider<CircuitBreakerRegistry> registryProvider,
            Environment environment) {
        return new StrobeResilienceService(registryProvider, environment);
    }

    /**
     * Registers the REST controller exposing circuit breaker telemetry and reset/transition endpoints.
     *
     * @param resilienceService The backing resilience inspection service.
     * @return Configured {@link StrobeResilienceController} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public StrobeResilienceController strobeResilienceController(StrobeResilienceService resilienceService) {
        return new StrobeResilienceController(resilienceService);
    }

    /**
     * Registers the concrete capability contributor reporting resilience domain status and provider metadata.
     *
     * @param registryProvider Safe provider verifying active {@link CircuitBreakerRegistry} instantiation.
     * @return Concrete {@link Resilience4jCapabilityContributor} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public Resilience4jCapabilityContributor resilience4jCapabilityContributor(
            ObjectProvider<CircuitBreakerRegistry> registryProvider) {
        return new Resilience4jCapabilityContributor(registryProvider);
    }
}