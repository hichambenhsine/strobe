package io.github.hichambenhsine.strobe.service;


import io.github.hichambenhsine.strobe.model.StrobeRouteDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing dynamic route definitions in memory.
 * Provides thread-safe CRUD operations for gateway routes without server restarts.
 */
@Service
public class StrobeRouteService {

    private final Map<String, StrobeRouteDto> routesRegistry = new ConcurrentHashMap<>();

    /**
     * Retrieves all dynamic routes currently registered in the system.
     *
     * @return List of active and inactive StrobeRouteDto instances.
     */
    public List<StrobeRouteDto> getAllRoutes() {
        return new ArrayList<>(routesRegistry.values());
    }

    /**
     * Registers or updates a dynamic route.
     *
     * @param route Route configuration payload.
     * @return The persisted StrobeRouteDto.
     */
    public StrobeRouteDto saveRoute(StrobeRouteDto route) {
        routesRegistry.put(route.id(), route);
        return route;
    }

    /**
     * Deletes a dynamic route by its unique identifier.
     *
     * @param routeId Unique route identifier.
     * @return true if the route existed and was removed, false otherwise.
     */
    public boolean deleteRoute(String routeId) {
        return routesRegistry.remove(routeId) != null;
    }

    /**
     * Toggles the active status of an existing route.
     *
     * @param routeId Unique route identifier.
     * @return Optional containing the updated route, or empty if not found.
     */
    public Optional<StrobeRouteDto> toggleRouteStatus(String routeId) {
        StrobeRouteDto existing = routesRegistry.get(routeId);
        if (existing == null) {
            return Optional.empty();
        }

        StrobeRouteDto updated = new StrobeRouteDto(
                existing.id(),
                existing.uri(),
                existing.path(),
                existing.predicates(),
                existing.filters(),
                !existing.enabled()
        );

        routesRegistry.put(routeId, updated);
        return Optional.of(updated);
    }
}