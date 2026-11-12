package io.github.hichambenhsine.strobe.config;

import io.github.hichambenhsine.strobe.controller.StrobeMetadataController;
import io.github.hichambenhsine.strobe.controller.StrobePingController;
import io.github.hichambenhsine.strobe.controller.StrobeRouteController;
import io.github.hichambenhsine.strobe.gateway.StrobeRouteDefinitionLocator;
import io.github.hichambenhsine.strobe.service.StrobeMetadataService;
import io.github.hichambenhsine.strobe.service.StrobeRouteService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
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
    public StrobeMetadataService strobeMetadataService(
            Environment environment,
            ApplicationContext context,
            ObjectProvider<StrobeRouteService> routeServiceObjectProvider){
        return new StrobeMetadataService(environment, context, routeServiceObjectProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public StrobeMetadataController strobeMetadataController(StrobeMetadataService strobeMetadataService) {
        return new StrobeMetadataController(strobeMetadataService);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.cloud.gateway.event.RefreshRoutesEvent")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class StrobeGatewayConfiguration {
        @Bean
        @ConditionalOnMissingBean
        public StrobeRouteService strobeRouteService(ApplicationEventPublisher applicationEventPublisher, ObjectProvider<RouteLocator> routeLocatorProvider){
            return new StrobeRouteService(applicationEventPublisher, routeLocatorProvider);
        }

        @Bean
        @ConditionalOnMissingBean
        public StrobeRouteController strobeRouteController(StrobeRouteService strobeRouteService){
            return new StrobeRouteController(strobeRouteService);
        }

        @Bean
        @ConditionalOnMissingBean
        public StrobeRouteDefinitionLocator strobeRouteDefinitionLocator(StrobeRouteService strobeRouteService){
            return new StrobeRouteDefinitionLocator(strobeRouteService);
        }
    }
}
