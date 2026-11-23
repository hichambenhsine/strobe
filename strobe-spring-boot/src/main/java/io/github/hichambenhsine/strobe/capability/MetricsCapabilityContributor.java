package io.github.hichambenhsine.strobe.capability;

import io.github.hichambenhsine.strobe.model.StrobeMetricsStatus;
import io.github.hichambenhsine.strobe.service.StrobeReactiveMetricsStreamService;
import io.github.hichambenhsine.strobe.service.StrobeServletMetricsStreamService;
import io.github.hichambenhsine.strobe.spi.StrobeCapabilityContributor;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Capability contributor evaluating live JVM metrics streaming availability across Servlet and Reactive runtimes.
 */
public class MetricsCapabilityContributor implements StrobeCapabilityContributor {

    private static final String PROVIDER_NAME = "jvm-mxbeans";
    private static final String TRANSPORT_NAME = "sse";

    private final ObjectProvider<StrobeServletMetricsStreamService> servletStreamProvider;
    private final ObjectProvider<StrobeReactiveMetricsStreamService> reactiveStreamProvider;

    public MetricsCapabilityContributor(
            ObjectProvider<StrobeServletMetricsStreamService> servletStreamProvider,
            ObjectProvider<StrobeReactiveMetricsStreamService> reactiveStreamProvider) {
        this.servletStreamProvider = servletStreamProvider;
        this.reactiveStreamProvider = reactiveStreamProvider;
    }

    @Override
    public String getCapabilityName() {
        return "metrics";
    }

    @Override
    public Object getCapabilityStatus() {
        boolean isActive = servletStreamProvider.getIfAvailable() != null
                || reactiveStreamProvider.getIfAvailable() != null;
        return new StrobeMetricsStatus(isActive, PROVIDER_NAME, TRANSPORT_NAME);
    }
}