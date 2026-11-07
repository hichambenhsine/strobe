package io.github.hichambenhsine.strobe.config;

import io.github.hichambenhsine.strobe.controller.StrobeMetadataController;
import io.github.hichambenhsine.strobe.controller.StrobePingController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
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
    public StrobeMetadataController strobeMetadataController(Environment environment) {
        return new StrobeMetadataController(environment);
    }
}
