package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeHealthResponse;
import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Low-level health inspection service leveraging Spring Boot's contributor registry.
 * Inspects in-memory health indicators directly to provide zero-configuration telemetry.
 */
@Service
public class StrobeHealthService {

    private final HealthContributorRegistry healthContributorRegistry;

    public StrobeHealthService(HealthContributorRegistry healthContributorRegistry) {
        this.healthContributorRegistry = healthContributorRegistry;
    }

    /**
     * Resolves health telemetry by evaluating all active contributors in memory.
     *
     * @return StrobeHealthResponse with aggregated status and unmasked subsystem metrics.
     */
    public StrobeHealthResponse getSystemHealth() {
        Map<String, Object> componentsData = new HashMap<>();
        boolean isSystemUp = true;

        for (NamedContributor<HealthContributor> namedContributor : healthContributorRegistry) {
            String name = namedContributor.getName();
            HealthContributor contributor = namedContributor.getContributor();

            if (contributor instanceof HealthIndicator indicator) {
                Health health = indicator.health();
                Map<String, Object> details = new HashMap<>();
                details.put("status", health.getStatus().getCode());
                details.put("details", health.getDetails());
                componentsData.put(name, details);

                if (!Status.UP.equals(health.getStatus())) {
                    isSystemUp = false;
                }
            }
        }

        String overallStatus = isSystemUp ? "UP" : "DOWN";
        return new StrobeHealthResponse(overallStatus, componentsData);
    }
}