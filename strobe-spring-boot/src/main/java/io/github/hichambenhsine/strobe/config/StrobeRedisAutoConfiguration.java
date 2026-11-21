package io.github.hichambenhsine.strobe.config;

import io.github.hichambenhsine.strobe.capability.RedisCapabilityContributor;
import io.github.hichambenhsine.strobe.controller.StrobeCacheController;
import io.github.hichambenhsine.strobe.controller.StrobeReactiveCacheController;
import io.github.hichambenhsine.strobe.service.RedisPayloadSanitizer;
import io.github.hichambenhsine.strobe.service.StrobeCacheService;
import io.github.hichambenhsine.strobe.service.StrobeCacheValidator;
import io.github.hichambenhsine.strobe.service.StrobeReactiveCacheService;
import io.github.hichambenhsine.strobe.service.StrobeReactiveRedisService;
import io.github.hichambenhsine.strobe.service.StrobeRedisService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Dedicated auto-configuration for the Strobe Redis Cache Inspector.
 * Activates strictly when both Spring Data Redis and the Lettuce driver (ScanCursor) are present,
 * preventing ClassLoader crashes on non-Redis applications or Jedis-only runtimes.
 */
@AutoConfiguration
@ConditionalOnClass(name = {
        "org.springframework.data.redis.connection.RedisConnectionFactory",
        "io.lettuce.core.ScanCursor"
})
public class StrobeRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisPayloadSanitizer redisPayloadSanitizer() {
        return new RedisPayloadSanitizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public StrobeCacheValidator strobeCacheValidator() {
        return new StrobeCacheValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisCapabilityContributor redisCapabilityContributor(
            ObjectProvider<RedisConnectionFactory> syncFactoryProvider,
            ObjectProvider<ReactiveRedisConnectionFactory> reactiveFactoryProvider,
            ObjectProvider<StrobeCacheService> syncServiceProvider,
            ObjectProvider<StrobeReactiveCacheService> reactiveServiceProvider) {
        return new RedisCapabilityContributor(
                syncFactoryProvider,
                reactiveFactoryProvider,
                syncServiceProvider,
                reactiveServiceProvider
        );
    }

    /**
     * Synchronous Servlet cache configuration for Spring MVC applications.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class StrobeServletRedisConfiguration {

        @Bean
        @ConditionalOnMissingBean(StrobeCacheService.class)
        public StrobeRedisService strobeRedisService(
                ObjectProvider<RedisConnectionFactory> connectionFactoryProvider,
                RedisPayloadSanitizer payloadSanitizer,
                StrobeCacheValidator validator) {
            return new StrobeRedisService(connectionFactoryProvider, payloadSanitizer, validator);
        }

        @Bean
        @ConditionalOnMissingBean
        public StrobeCacheController strobeCacheController(StrobeCacheService cacheService) {
            return new StrobeCacheController(cacheService);
        }
    }

    /**
     * Non-blocking Reactive cache configuration for Spring WebFlux and API Gateways.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public static class StrobeReactiveRedisConfiguration {

        @Bean
        @ConditionalOnMissingBean(StrobeReactiveCacheService.class)
        public StrobeReactiveRedisService strobeReactiveRedisService(
                ObjectProvider<ReactiveRedisConnectionFactory> reactiveFactoryProvider,
                RedisPayloadSanitizer payloadSanitizer,
                StrobeCacheValidator validator) {
            return new StrobeReactiveRedisService(reactiveFactoryProvider, payloadSanitizer, validator);
        }

        @Bean
        @ConditionalOnMissingBean
        public StrobeReactiveCacheController strobeReactiveCacheController(
                StrobeReactiveCacheService reactiveCacheService) {
            return new StrobeReactiveCacheController(reactiveCacheService);
        }
    }
}