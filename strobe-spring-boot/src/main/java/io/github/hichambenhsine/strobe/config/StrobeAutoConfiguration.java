package io.github.hichambenhsine.strobe.config;

import io.github.hichambenhsine.strobe.capability.ActuatorCapabilityContributor;
import io.github.hichambenhsine.strobe.controller.StrobeMetadataController;
import io.github.hichambenhsine.strobe.controller.StrobePingController;
import io.github.hichambenhsine.strobe.service.StrobeMetadataService;
import io.github.hichambenhsine.strobe.spi.StrobeCapabilityContributor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * StrobeAutoConfiguration
 * <p>
 * Core auto-configuration entry point for the Strobe platform.
 * Provisions baseline diagnostic endpoints, extensible capability metadata aggregation,
 * and Spring Boot Actuator capability detection.
 * </p>
 * <p>
 * Optional ecosystem modules (Gateway, Resilience4j, Redis) are strictly isolated in dedicated
 * top-level {@code @AutoConfiguration} classes registered independently in {@code AutoConfiguration.imports}.
 * </p>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(RestController.class)
public class StrobeAutoConfiguration {

    /**
     * Registers the baseline HTTP connectivity probe controller.
     *
     * @return Configured {@link StrobePingController} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public StrobePingController strobePingController() {
        return new StrobePingController();
    }

    /**
     * Registers the capability contributor evaluating Spring Boot Actuator web infrastructure
     * availability on the same origin port.
     *
     * @param environment Spring environment properties accessor.
     * @param context Spring application context for safe bean existence lookups.
     * @return Concrete {@link ActuatorCapabilityContributor} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public ActuatorCapabilityContributor actuatorCapabilityContributor(
            Environment environment,
            ApplicationContext context) {
        return new ActuatorCapabilityContributor(environment, context);
    }

    /**
     * Registers the decoupled metadata aggregation service consuming all active
     * {@link StrobeCapabilityContributor} beans via lazy ordered streaming.
     *
     * @param environment Spring environment properties accessor.
     * @param contributorsProvider Safe provider yielding all registered capability contributors.
     * @return Configured {@link StrobeMetadataService} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public StrobeMetadataService strobeMetadataService(
            Environment environment,
            ObjectProvider<StrobeCapabilityContributor> contributorsProvider) {
        List<StrobeCapabilityContributor> contributors = contributorsProvider.orderedStream().toList();
        return new StrobeMetadataService(environment, contributors);
    }

    /**
     * Registers the REST controller exposing runtime metadata and active capability flags.
     *
     * @param strobeMetadataService The backing metadata aggregation service.
     * @return Configured {@link StrobeMetadataController} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public StrobeMetadataController strobeMetadataController(StrobeMetadataService strobeMetadataService) {
        return new StrobeMetadataController(strobeMetadataService);
    }
}