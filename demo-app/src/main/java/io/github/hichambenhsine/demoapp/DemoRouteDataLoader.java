package io.github.hichambenhsine.demoapp;

import io.github.hichambenhsine.strobe.model.StrobeRouteDto;
import io.github.hichambenhsine.strobe.service.StrobeRouteService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Testbed data loader initializing sample microservice routes at application startup.
 */
@Component
public class DemoRouteDataLoader implements CommandLineRunner {

    private final StrobeRouteService routeService;

    public DemoRouteDataLoader(StrobeRouteService routeService) {
        this.routeService = routeService;
    }

    @Override
    public void run(String... args) {
        // Sample Route 1: Order Service
        routeService.saveRoute(new StrobeRouteDto(
                "order-service-route",
                "http://order-service:8081",
                "/api/v1/orders/**",
                List.of("Path=/api/v1/orders/**", "Method=GET,POST"),
                List.of("StripPrefix=1"),
                true
        ));

        // Sample Route 2: Inventory Service
        routeService.saveRoute(new StrobeRouteDto(
                "inventory-service-route",
                "http://inventory-service:8082",
                "/api/v1/inventory/**",
                List.of("Path=/api/v1/inventory/**"),
                List.of("AddResponseHeader=X-Gateway-Proxy,Strobe"),
                true
        ));

        // Sample Route 3: Legacy Payment Gateway (Disabled)
        routeService.saveRoute(new StrobeRouteDto(
                "legacy-payment-route",
                "http://payment-service:8083",
                "/api/v1/payments/**",
                List.of("Path=/api/v1/payments/**"),
                List.of("CircuitBreaker=paymentCircuitBreaker"),
                false
        ));
    }
}