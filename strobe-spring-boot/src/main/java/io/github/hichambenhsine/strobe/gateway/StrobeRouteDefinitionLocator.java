package io.github.hichambenhsine.strobe.gateway;

import io.github.hichambenhsine.strobe.model.StrobeRouteDto;
import io.github.hichambenhsine.strobe.service.StrobeRouteService;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import reactor.core.publisher.Flux;

import java.net.URI;

public class StrobeRouteDefinitionLocator implements RouteDefinitionLocator {

    private final StrobeRouteService routeService;

    public StrobeRouteDefinitionLocator(StrobeRouteService routeService) {
        this.routeService = routeService;
    }

    @Override
    public Flux<RouteDefinition> getRouteDefinitions() {
        return Flux.fromIterable(routeService.getCustomRoutes())
                .filter(StrobeRouteDto::enabled)
                .map(this::mapToRouteDefinition);
    }

    private RouteDefinition mapToRouteDefinition(StrobeRouteDto dto) {
        RouteDefinition rd = new RouteDefinition();
        rd.setId(dto.id());
        rd.setUri(URI.create(dto.uri()));

        // Check if an explicit Path predicate is already present in predicates list
        boolean hasExplicitPathPredicate = dto.predicates() != null &&
                dto.predicates().stream().anyMatch(pred -> pred != null && pred.startsWith("Path="));

        // Inject shortcut path only if not already covered by explicit predicates
        if (!hasExplicitPathPredicate && dto.path() != null && !dto.path().isBlank()) {
            rd.getPredicates().add(new PredicateDefinition("Path=" + dto.path()));
        }

        // Map additional predicates (e.g., Method=GET, Header=X-Tenant,tenantA)
        if (dto.predicates() != null) {
            for (String pred : dto.predicates()) {
                if (pred != null && !pred.isBlank()) {
                    rd.getPredicates().add(new PredicateDefinition(pred));
                }
            }
        }

        // Map gateway filters (e.g., StripPrefix=1, RewritePath=...)
        if (dto.filters() != null) {
            for (String filter : dto.filters()) {
                if (filter != null && !filter.isBlank()) {
                    rd.getFilters().add(new FilterDefinition(filter));
                }
            }
        }

        // Forward metadata (e.g., connect-timeout, management.port)
        if (dto.metadata() != null && !dto.metadata().isEmpty()) {
            rd.setMetadata(dto.metadata());
        }

        return rd;
    }
}
