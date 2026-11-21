package io.github.hichambenhsine.strobe.config;

import io.github.hichambenhsine.strobe.capability.SpringCloudGatewayCapabilityContributor;
import io.github.hichambenhsine.strobe.controller.StrobeRouteController;
import io.github.hichambenhsine.strobe.gateway.StrobeRouteDefinitionLocator;
import io.github.hichambenhsine.strobe.service.StrobeRouteService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * StrobeSpringCloudGatewayAutoConfiguration
 * <p>
 * Dedicated auto-configuration provisioning dynamic route management and capability discovery
 * for reactive Spring Cloud Gateway runtimes.
 * </p>
 * <p>
 * Guarded by string-based {@link ConditionalOnClass} on {@code RefreshRoutesEvent} and restricted
 * exclusively to {@link ConditionalOnWebApplication.Type#REACTIVE} containers to ensure zero
 * classpath or ClassLoader interference within traditional Servlet microservices.
 * </p>
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.springframework.cloud.gateway.event.RefreshRoutesEvent")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class StrobeSpringCloudGatewayAutoConfiguration {

    /**
     * Registers the thread-safe in-memory route registry and runtime route discovery service.
     *
     * @param applicationEventPublisher Spring event publisher used to broadcast {@code RefreshRoutesEvent}.
     * @param routeLocatorProvider Safe provider for the native Gateway {@link RouteLocator}.
     * @return Configured {@link StrobeRouteService} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public StrobeRouteService strobeRouteService(
            ApplicationEventPublisher applicationEventPublisher,
            ObjectProvider<RouteLocator> routeLocatorProvider) {
        return new StrobeRouteService(applicationEventPublisher, routeLocatorProvider);
    }

    /**
     * Registers the reactive REST controller exposing CRUD operations for dynamic gateway routes.
     *
     * @param strobeRouteService The backing route management service.
     * @return Configured {@link StrobeRouteController} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public StrobeRouteController strobeRouteController(StrobeRouteService strobeRouteService) {
        return new StrobeRouteController(strobeRouteService);
    }

    /**
     * Registers the reactive {@code RouteDefinitionLocator} streaming Strobe custom routes
     * into the underlying Spring Cloud Gateway Netty routing engine.
     *
     * @param strobeRouteService The backing route management service.
     * @return Configured {@link StrobeRouteDefinitionLocator} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public StrobeRouteDefinitionLocator strobeRouteDefinitionLocator(StrobeRouteService strobeRouteService) {
        return new StrobeRouteDefinitionLocator(strobeRouteService);
    }

    /**
     * Registers the concrete capability contributor reporting gateway status and provider metadata.
     *
     * @param routeLocatorProvider Safe provider verifying active {@link RouteLocator} instantiation.
     * @return Concrete {@link SpringCloudGatewayCapabilityContributor} instance.
     */
    @Bean
    @ConditionalOnMissingBean
    public SpringCloudGatewayCapabilityContributor springCloudGatewayCapabilityContributor(
            ObjectProvider<RouteLocator> routeLocatorProvider) {
        return new SpringCloudGatewayCapabilityContributor(routeLocatorProvider);
    }
}