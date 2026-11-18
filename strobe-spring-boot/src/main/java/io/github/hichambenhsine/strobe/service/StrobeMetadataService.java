package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeActuatorStatus;
import io.github.hichambenhsine.strobe.model.StrobeMetadataDto;
import io.github.hichambenhsine.strobe.spi.StrobeCapabilityContributor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StrobeMetadataService {

    private final Environment environment;
    private final List<StrobeCapabilityContributor> contributors;

    public StrobeMetadataService(Environment environment, List<StrobeCapabilityContributor> contributors) {
        this.environment = environment;
        this.contributors = contributors != null ? contributors : List.of();
    }

    public StrobeMetadataDto getMetadata() {
        String appName = environment.getProperty("spring.application.name", "spring-boot-app");
        Map<String, Object> capabilities = new LinkedHashMap<>();

        for (StrobeCapabilityContributor contributor : contributors) {
            capabilities.put(contributor.getCapabilityName(), contributor.getCapabilityStatus());
        }

        return new StrobeMetadataDto(appName, capabilities);
    }
}
