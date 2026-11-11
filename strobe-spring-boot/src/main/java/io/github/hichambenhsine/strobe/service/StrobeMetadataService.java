package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeActuatorStatus;
import io.github.hichambenhsine.strobe.model.StrobeMetadataDto;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

public class StrobeMetadataService {

    private final Environment environment;
    private final ApplicationContext context;
    private final ObjectProvider<StrobeRouteService> routeServiceProvider;

    public StrobeMetadataService(Environment environment, ApplicationContext context, ObjectProvider<StrobeRouteService> routeServiceProvider) {
        this.environment = environment;
        this.context = context;
        this.routeServiceProvider = routeServiceProvider;
    }

    public StrobeMetadataDto getMetadata(){
        return new StrobeMetadataDto(
                appName(),
                actuatorStatus(),
                isGatewayEnabled());
    }

    private String appName() {
        return environment.getProperty("spring.application.name", "spring-boot-app");
    }

    private StrobeActuatorStatus actuatorStatus() {
        boolean isActuatorEnabled = context.containsBean("healthEndpoint");
        return new StrobeActuatorStatus(isActuatorEnabled);
    }

    private boolean isGatewayEnabled() {
        return routeServiceProvider.getIfAvailable() != null;
    }
}
