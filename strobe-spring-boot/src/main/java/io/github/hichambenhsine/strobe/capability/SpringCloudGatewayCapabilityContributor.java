package io.github.hichambenhsine.strobe.capability;

import io.github.hichambenhsine.strobe.model.StrobeGatewayStatus;
import io.github.hichambenhsine.strobe.spi.StrobeCapabilityContributor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.route.RouteLocator;

/**
 * Capability contributor evaluating Spring Cloud Gateway availability at runtime.
 * Contributes under the generic "gateway" domain port while explicitly identifying the concrete provider.
 */
public class SpringCloudGatewayCapabilityContributor implements StrobeCapabilityContributor {

    private static final String PROVIDER_NAME = "spring-cloud-gateway";

    private final ObjectProvider<RouteLocator> routeLocatorProvider;

    public SpringCloudGatewayCapabilityContributor(ObjectProvider<RouteLocator> routeLocatorProvider) {
        this.routeLocatorProvider = routeLocatorProvider;
    }

    @Override
    public String getCapabilityName() {
        return "gateway";
    }

    @Override
    public Object getCapabilityStatus() {
        boolean isActive = routeLocatorProvider.getIfAvailable() != null;
        return new StrobeGatewayStatus(isActive, PROVIDER_NAME);
    }
}