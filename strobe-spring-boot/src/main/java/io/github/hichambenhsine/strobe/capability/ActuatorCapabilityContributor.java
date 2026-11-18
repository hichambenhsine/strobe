package io.github.hichambenhsine.strobe.capability;

import io.github.hichambenhsine.strobe.model.StrobeActuatorStatus;
import io.github.hichambenhsine.strobe.spi.StrobeCapabilityContributor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Capability contributor evaluating Spring Boot Actuator web availability on the host application origin.
 * Operates strictly via bean infrastructure checks without importing Actuator classes to guarantee cross-version safety.
 */
public class ActuatorCapabilityContributor implements StrobeCapabilityContributor {

    private final Environment environment;
    private final ApplicationContext applicationContext;

    public ActuatorCapabilityContributor(Environment environment, ApplicationContext applicationContext) {
        this.environment = environment;
        this.applicationContext = applicationContext;
    }

    @Override
    public String getCapabilityName() {
        return "actuator";
    }

    @Override
    public Object getCapabilityStatus() {
        boolean hasWebInfrastructure = applicationContext.containsBean("webEndpointDiscoverer")
                || applicationContext.containsBean("webMvcEndpointHandlerMapping")
                || applicationContext.containsBean("webFluxEndpointHandlerMapping");

        boolean isSameOriginPort = isActuatorOnSamePort();
        boolean isEnabled = hasWebInfrastructure && isSameOriginPort;

        String basePath = normalizeBasePath(
                environment.getProperty("management.endpoints.web.base-path", "/actuator")
        );

        return new StrobeActuatorStatus(isEnabled, basePath);
    }

    private boolean isActuatorOnSamePort() {
        String managementPort = environment.getProperty("management.server.port");
        if (managementPort == null || managementPort.isBlank()) {
            return true;
        }
        String serverPort = environment.getProperty("server.port", "8080");
        return managementPort.trim().equals(serverPort.trim());
    }

    private String normalizeBasePath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "/actuator";
        }
        String trimmed = rawPath.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }
}