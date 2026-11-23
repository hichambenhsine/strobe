package io.github.hichambenhsine.strobe.config;

import io.github.hichambenhsine.strobe.capability.MetricsCapabilityContributor;
import io.github.hichambenhsine.strobe.controller.StrobeMetricsController;
import io.github.hichambenhsine.strobe.controller.StrobeReactiveMetricsController;
import io.github.hichambenhsine.strobe.service.StrobeMetricsCollector;
import io.github.hichambenhsine.strobe.service.StrobeReactiveMetricsStreamService;
import io.github.hichambenhsine.strobe.service.StrobeServletMetricsStreamService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dedicated auto-configuration for Strobe live JVM metrics telemetry and SSE streaming.
 * Strictly isolates Servlet (SseEmitter) and Reactive (Flux) transport implementations.
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class StrobeMetricsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public StrobeMetricsCollector strobeMetricsCollector() {
        return new StrobeMetricsCollector();
    }

    @Bean
    @ConditionalOnMissingBean
    public MetricsCapabilityContributor metricsCapabilityContributor(
            ObjectProvider<StrobeServletMetricsStreamService> servletStreamProvider,
            ObjectProvider<StrobeReactiveMetricsStreamService> reactiveStreamProvider) {
        return new MetricsCapabilityContributor(servletStreamProvider, reactiveStreamProvider);
    }

    /**
     * Synchronous Servlet SSE configuration for Spring MVC applications.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.servlet.mvc.method.annotation.SseEmitter")
    public static class StrobeServletMetricsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public StrobeServletMetricsStreamService strobeServletMetricsStreamService(
                StrobeMetricsCollector metricsCollector) {
            return new StrobeServletMetricsStreamService(metricsCollector);
        }

        @Bean
        @ConditionalOnMissingBean
        public StrobeMetricsController strobeMetricsController(
                StrobeServletMetricsStreamService metricsStreamService) {
            return new StrobeMetricsController(metricsStreamService);
        }
    }

    /**
     * Non-blocking Reactive SSE configuration for Spring WebFlux and API Gateways.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnClass(name = "reactor.core.publisher.Flux")
    public static class StrobeReactiveMetricsConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public StrobeReactiveMetricsStreamService strobeReactiveMetricsStreamService(
                StrobeMetricsCollector metricsCollector) {
            return new StrobeReactiveMetricsStreamService(metricsCollector);
        }

        @Bean
        @ConditionalOnMissingBean
        public StrobeReactiveMetricsController strobeReactiveMetricsController(
                StrobeReactiveMetricsStreamService reactiveMetricsStreamService) {
            return new StrobeReactiveMetricsController(reactiveMetricsStreamService);
        }
    }
}