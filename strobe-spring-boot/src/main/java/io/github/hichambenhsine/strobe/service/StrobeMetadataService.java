package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeActuatorStatus;
import io.github.hichambenhsine.strobe.model.StrobeMetadataDto;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

public class StrobeMetadataService {

    private final Environment environment;
    private final ApplicationContext context;

    public StrobeMetadataService(Environment environment, ApplicationContext context) {
        this.environment = environment;
        this.context = context;
    }

    public StrobeMetadataDto getMetadata(){
        return new StrobeMetadataDto(
                    appName(),
                    actuatorStatus()
                );
    }

    private String appName() {
        return environment.getProperty("spring.application.name", "spring-boot-app");
    }

    private StrobeActuatorStatus actuatorStatus() {
        boolean isActuatorEnabled = context.containsBean("healthEndpoint");
        return new StrobeActuatorStatus(isActuatorEnabled);
    }
}
