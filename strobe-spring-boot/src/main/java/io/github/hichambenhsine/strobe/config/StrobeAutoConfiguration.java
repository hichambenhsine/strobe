package io.github.hichambenhsine.strobe.config;

import io.github.hichambenhsine.strobe.controller.StrobeMetadataController;
import io.github.hichambenhsine.strobe.controller.StrobePingController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Core auto-configuration entry point.
 * Employs explicit @Bean factory methods to guarantee safe classloading.
 */
@AutoConfiguration
public class StrobeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StrobePingController strobePingController() {
        return new StrobePingController();
    }

    @Bean
    @ConditionalOnMissingBean
    public StrobeMetadataController strobeMetadataController(Environment environment) {
        return new StrobeMetadataController(environment);
    }
}
