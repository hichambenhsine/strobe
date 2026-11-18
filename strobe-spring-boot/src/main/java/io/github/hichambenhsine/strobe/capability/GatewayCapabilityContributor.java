package io.github.hichambenhsine.strobe.capability;

import io.github.hichambenhsine.strobe.model.StrobeGatewayStatus;
import io.github.hichambenhsine.strobe.spi.StrobeCapabilityContributor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.route.RouteLocator;

/**
 * Capability contributor evaluating Spring Cloud Gateway availability at runtime.
 * Verifies actual RouteLocator bean instantiation rather than relying solely on classpath presence.
 */
public class GatewayCapabilityContributor implements StrobeCapabilityContributor {

    private final ObjectProvider<RouteLocator> routeLocatorProvider;

    public GatewayCapabilityContributor(ObjectProvider<RouteLocator> routeLocatorProvider) {
        this.routeLocatorProvider = routeLocatorProvider;
    }

    @Override
    public String getCapabilityName() {
        return "gateway";
    }

    @Override
    public Object getCapabilityStatus() {
        boolean isActive = routeLocatorProvider.getIfAvailable() != null;
        return new StrobeGatewayStatus(isActive);
    }
}