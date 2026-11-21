package io.github.hichambenhsine.strobe.controller;

import io.github.hichambenhsine.strobe.exception.StrobeCacheUnavailableException;
import io.github.hichambenhsine.strobe.model.StrobeCacheEntryDto;
import io.github.hichambenhsine.strobe.model.StrobeCachePurgeResultDto;
import io.github.hichambenhsine.strobe.model.StrobeCacheScanResultDto;
import io.github.hichambenhsine.strobe.service.StrobeCacheService;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Synchronous REST controller exposing cache inspection operations for Servlet runtimes.
 * Depends strictly on the StrobeCacheService domain port (DIP compliance).
 */
@RestController
@RequestMapping("/strobe/api/cache")
public class StrobeCacheController {

    private final StrobeCacheService cacheService;

    public StrobeCacheController(StrobeCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/keys")
    public StrobeCacheScanResultDto scanKeys(
            @RequestParam(value = "pattern", defaultValue = "*") String pattern,
            @RequestParam(value = "cursor", defaultValue = "0") String cursor,
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return cacheService.scanKeys(pattern, cursor, limit);
    }

    @GetMapping("/entry")
    public ResponseEntity<StrobeCacheEntryDto> getEntry(@RequestParam("key") String key) {
        return cacheService.getEntry(key)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/entry")
    public ResponseEntity<Void> unlinkKey(@RequestParam("key") String key) {
        boolean removed = cacheService.unlinkKey(key);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/keys")
    public StrobeCachePurgeResultDto purgeByPattern(
            @RequestParam("pattern") String pattern,
            @RequestParam(value = "cursor", defaultValue = "0") String cursor) {
        return cacheService.purgeByPattern(pattern, cursor);
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