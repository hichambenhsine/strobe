package io.github.hichambenhsine.strobe.controller;

import io.github.hichambenhsine.strobe.model.StrobeRouteDto;
import io.github.hichambenhsine.strobe.service.StrobeRouteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing CRUD management endpoints for dynamic gateway routes.
 */
@RestController
@RequestMapping("/strobe/api/routes")
public class StrobeRouteController {

    private final StrobeRouteService routeService;

    public StrobeRouteController(StrobeRouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<StrobeRouteDto> getAllRoutes() {
        return routeService.getAllRoutes();
    }

    @PostMapping
    public ResponseEntity<StrobeRouteDto> createRoute(@RequestBody StrobeRouteDto route) {
        StrobeRouteDto created = routeService.saveRoute(route);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable String id) {
        boolean removed = routeService.deleteRoute(id);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<StrobeRouteDto> toggleRoute(@PathVariable String id) {
        return routeService.toggleRouteStatus(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
