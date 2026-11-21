package io.github.hichambenhsine.strobe.config;

import io.github.hichambenhsine.strobe.capability.ActuatorCapabilityContributor;
import io.github.hichambenhsine.strobe.capability.Resilience4jCapabilityContributor;
import io.github.hichambenhsine.strobe.controller.StrobeMetadataController;
import io.github.hichambenhsine.strobe.controller.StrobePingController;
import io.github.hichambenhsine.strobe.controller.StrobeResilienceController;
import io.github.hichambenhsine.strobe.service.StrobeMetadataService;
import io.github.hichambenhsine.strobe.service.StrobeResilienceService;
import io.github.hichambenhsine.strobe.spi.StrobeCapabilityContributor;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;

/**
 * Core auto-configuration entry point.
 * Employs explicit @Bean factory methods to guarantee safe classloading.
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(RestController.class)
public class StrobeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StrobePingController strobePingController() {
        return new StrobePingController();
    }

    @Bean
    @ConditionalOnMissingBean
    public ActuatorCapabilityContributor actuatorCapabilityContributor(
            Environment environment,
            ApplicationContext context) {
        return new ActuatorCapabilityContributor(environment, context);
    }

    @Bean
    @ConditionalOnMissingBean
    public StrobeMetadataService strobeMetadataService(
            Environment environment,
            ObjectProvider<StrobeCapabilityContributor> contributorsProvider) {
        return new StrobeMetadataService(environment, contributorsProvider.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public StrobeMetadataController strobeMetadataController(StrobeMetadataService strobeMetadataService) {
        return new StrobeMetadataController(strobeMetadataService);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry")
    public static class StrobeResilience4jConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public StrobeResilienceService strobeResilienceService(
                ObjectProvider<CircuitBreakerRegistry> registryProvider,
                Environment environment) {
            return new StrobeResilienceService(registryProvider, environment);
        }

        @Bean
        @ConditionalOnMissingBean
        public StrobeResilienceController strobeResilienceController(StrobeResilienceService resilienceService){
            return new StrobeResilienceController(resilienceService);
        }

        @Bean
        @ConditionalOnMissingBean
        public Resilience4jCapabilityContributor resilience4jCapabilityContributor(
                ObjectProvider<CircuitBreakerRegistry> registryProvider) {
            return new Resilience4jCapabilityContributor(registryProvider);
        }
    }
}
