package io.github.hichambenhsine.strobe.config;

import io.github.hichambenhsine.strobe.controller.StrobeMetadataController;
import io.github.hichambenhsine.strobe.controller.StrobePingController;
import io.github.hichambenhsine.strobe.model.StrobeActuatorStatus;
import io.github.hichambenhsine.strobe.service.StrobeMetadataService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;
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
    public StrobeMetadataService strobeMetadataService(Environment environment, ApplicationContext context){
        return new StrobeMetadataService(environment, context);
    }

    @Bean
    @ConditionalOnMissingBean
    public StrobeMetadataController strobeMetadataController(StrobeMetadataService strobeMetadataService) {
        return new StrobeMetadataController(strobeMetadataService);
    }
}
