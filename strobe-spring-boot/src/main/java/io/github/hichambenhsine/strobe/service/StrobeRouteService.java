package io.github.hichambenhsine.strobe.service;

import io.github.hichambenhsine.strobe.model.StrobeRouteDto;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class StrobeRouteService {
    private static final java.util.regex.Pattern PATH_PATTERN =
            java.util.regex.Pattern.compile("Paths:\\s*\\[([^\\]]+)\\]");

    private final Map<String, StrobeRouteDto> routesRegistry = new ConcurrentHashMap<>();
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectProvider<RouteLocator> routeLocatorProvider;

    public StrobeRouteService(ApplicationEventPublisher eventPublisher, ObjectProvider<RouteLocator> routeLocatorProvider) {
        this.eventPublisher = eventPublisher;
        this.routeLocatorProvider = routeLocatorProvider;
    }

    public Collection<StrobeRouteDto> getCustomRoutes(){
        return routesRegistry.values();
    }

    public Flux<StrobeRouteDto> getAllRoutes() {
        RouteLocator locator = routeLocatorProvider.getIfAvailable();
        if(locator == null) {
            return Flux.fromIterable(routesRegistry.values());
        }

        return locator.getRoutes()
                .map(this::mapNativeRouteToDto)
                .collectList()
                .flatMapMany(nativeRoutes -> {
                    List<StrobeRouteDto> combined = new ArrayList<>(nativeRoutes);
                    for (StrobeRouteDto custom : routesRegistry.values()) {
                        if (combined.stream().noneMatch(r -> r.id().equals(custom.id()))) {
                            combined.add(custom);
                        }
                    }
                    return Flux.fromIterable(combined);
                });
    }

    public StrobeRouteDto saveRoute(StrobeRouteDto route){
        routesRegistry.put(route.id(), route);
        publishRefreshEvent();
        return route;
    }

    public boolean deleteRoute(String routeId){
        boolean removed = routesRegistry.remove(routeId) != null;
        if(removed){
           publishRefreshEvent();
        }
        return removed;
    }

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
                existing.metadata(),
                !existing.enabled());

        routesRegistry.put(routeId, updated);
        publishRefreshEvent();
        return Optional.of(updated);
    }

    private void publishRefreshEvent(){
        eventPublisher.publishEvent(new RefreshRoutesEvent(this));
    }

    private StrobeRouteDto mapNativeRouteToDto(Route route) {
        if (routesRegistry.containsKey(route.getId())) {
            return routesRegistry.get(route.getId());
        }

        String rawPredicate = route.getPredicate() != null ? route.getPredicate().toString() : "";
        String extractedPath = "";

        // 1. Extract clean path pattern for the UI column
        java.util.regex.Matcher matcher = PATH_PATTERN.matcher(rawPredicate);
        if (matcher.find()) {
            extractedPath = matcher.group(1).trim();
        }

        // 2. Clean up JVM lambda leakage from predicate display
        String cleanPredicate = rawPredicate
                .replaceAll("RouteDefinitionRouteLocator\\$\\$Lambda/[^& ]+", "")
                .replaceAll("[()]", "")
                .replaceAll("^\\s*&&\\s*", "")
                .trim();

        // 3. Extract real gateway filters (e.g. RewritePath, StripPrefix)
        List<String> cleanFilters = route.getFilters() != null
                ? route.getFilters().stream()
                .map(Object::toString)
                .map(f -> f.replaceAll("^\\[+", "").replaceAll("\\]+$", "").trim())
                .filter(f -> !f.isBlank())
                .toList()
                : List.of();

        return new StrobeRouteDto(
                route.getId(),
                route.getUri().toString(),
                extractedPath,
                cleanPredicate.isBlank() ? List.of() : List.of(cleanPredicate),
                cleanFilters,
                route.getMetadata() != null ? route.getMetadata() : Map.of(),
                true
        );
    }
}
