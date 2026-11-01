package io.github.hichambenhsine.strobe.model;

import java.util.List;

/**
 * Data transfer object representing a dynamic routing definition managed by Strobe.
 *
 * @param id Unique route identifier.
 * @param uri Target destination URI (e.g., https://httpbin.org).
 * @param path Inbound path pattern to match (e.g., /api/v1/orders/**).
 * @param predicates List of routing predicates applied to the route.
 * @param filters List of gateway filters applied to the route.
 * @param enabled Operational flag indicating if the route is active.
 */
public record StrobeRouteDto(
        String id,
        String uri,
        String path,
        List<String> predicates,
        List<String> filters,
        boolean enabled
) {}