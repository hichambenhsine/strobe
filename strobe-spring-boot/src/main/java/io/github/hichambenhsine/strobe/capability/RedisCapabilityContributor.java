package io.github.hichambenhsine.strobe.capability;

import io.github.hichambenhsine.strobe.model.StrobeCacheStatus;
import io.github.hichambenhsine.strobe.service.StrobeCacheService;
import io.github.hichambenhsine.strobe.service.StrobeReactiveCacheService;
import io.github.hichambenhsine.strobe.spi.StrobeCapabilityContributor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Capability contributor evaluating Redis cache inspection availability across both Servlet and Reactive runtimes.
 */
public class RedisCapabilityContributor implements StrobeCapabilityContributor {

    private static final String PROVIDER_NAME = "redis";

    private final ObjectProvider<RedisConnectionFactory> syncFactoryProvider;
    private final ObjectProvider<ReactiveRedisConnectionFactory> reactiveFactoryProvider;
    private final ObjectProvider<StrobeCacheService> syncServiceProvider;
    private final ObjectProvider<StrobeReactiveCacheService> reactiveServiceProvider;

    public RedisCapabilityContributor(
            ObjectProvider<RedisConnectionFactory> syncFactoryProvider,
            ObjectProvider<ReactiveRedisConnectionFactory> reactiveFactoryProvider,
            ObjectProvider<StrobeCacheService> syncServiceProvider,
            ObjectProvider<StrobeReactiveCacheService> reactiveServiceProvider) {
        this.syncFactoryProvider = syncFactoryProvider;
        this.reactiveFactoryProvider = reactiveFactoryProvider;
        this.syncServiceProvider = syncServiceProvider;
        this.reactiveServiceProvider = reactiveServiceProvider;
    }

    @Override
    public String getCapabilityName() {
        return "cache";
    }

    @Override
    public Object getCapabilityStatus() {
        boolean hasFactory = syncFactoryProvider.getIfAvailable() != null
                || reactiveFactoryProvider.getIfAvailable() != null;
        boolean hasService = syncServiceProvider.getIfAvailable() != null
                || reactiveServiceProvider.getIfAvailable() != null;

        return new StrobeCacheStatus(hasFactory && hasService, PROVIDER_NAME);
    }
}