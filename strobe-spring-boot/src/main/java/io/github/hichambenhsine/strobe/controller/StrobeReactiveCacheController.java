package io.github.hichambenhsine.strobe.controller;

import io.github.hichambenhsine.strobe.exception.StrobeCacheUnavailableException;
import io.github.hichambenhsine.strobe.model.StrobeCacheEntryDto;
import io.github.hichambenhsine.strobe.model.StrobeCachePurgeResultDto;
import io.github.hichambenhsine.strobe.model.StrobeCacheScanResultDto;
import io.github.hichambenhsine.strobe.service.StrobeReactiveCacheService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Reactive REST controller exposing cache inspection and eviction operations for WebFlux runtimes.
 * Depends strictly on the StrobeReactiveCacheService domain port (DIP compliance).
 */
@RestController
@RequestMapping("/strobe/api/cache")
public class StrobeReactiveCacheController {

    private final StrobeReactiveCacheService reactiveCacheService;

    public StrobeReactiveCacheController(StrobeReactiveCacheService reactiveCacheService) {
        this.reactiveCacheService = reactiveCacheService;
    }

    @GetMapping("/keys")
    public Mono<StrobeCacheScanResultDto> scanKeys(
            @RequestParam(value = "pattern", defaultValue = "*") String pattern,
            @RequestParam(value = "cursor", defaultValue = "0") String cursor,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return reactiveCacheService.scanKeys(pattern, cursor, limit);
    }

    @GetMapping("/entry")
    public Mono<ResponseEntity<StrobeCacheEntryDto>> getEntry(@RequestParam("key") String key) {
        return reactiveCacheService.getEntry(key)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/entry")
    public Mono<ResponseEntity<Void>> unlinkKey(@RequestParam("key") String key) {
        return reactiveCacheService.unlinkKey(key)
                .map(removed -> removed
                        ? ResponseEntity.noContent().<Void>build()
                        : ResponseEntity.notFound().<Void>build());
    }

    @DeleteMapping("/keys")
    public Mono<StrobeCachePurgeResultDto> purgeByPattern(
            @RequestParam("pattern") String pattern,
            @RequestParam(value = "cursor", defaultValue = "0") String cursor) {
        return reactiveCacheService.purgeByPattern(pattern, cursor);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleInvalidArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({DataAccessException.class, StrobeCacheUnavailableException.class})
    public ProblemDetail handleCacheUnavailable(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Cache datastore is currently unreachable"
        );
    }
}