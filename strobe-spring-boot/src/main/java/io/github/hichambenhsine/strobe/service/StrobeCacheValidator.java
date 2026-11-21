package io.github.hichambenhsine.strobe.service;

/**
 * Shared validation and safety policy component for cache operations.
 * Eliminates duplication between Servlet and Reactive cache adapters.
 */
public class StrobeCacheValidator {

    public static final int MAX_SCAN_LIMIT = 100;
    public static final int MAX_SCAN_STEPS = 10;
    public static final int MAX_COLLECTION_SAMPLE = 50;
    public static final int MAX_PURGE_STEPS = 20;
    public static final long MAX_PURGE_LIMIT = 1000L;
    public static final int PURGE_SCAN_BATCH_SIZE = 200;
    private static final int MIN_PURGE_PREFIX_LENGTH = 3;

    public String normalizePattern(String pattern) {
        return (pattern == null || pattern.isBlank()) ? "*" : pattern.trim();
    }

    public int clampScanLimit(int limit) {
        return Math.max(1, Math.min(limit, MAX_SCAN_LIMIT));
    }

    public String validateCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return "0";
        }
        String trimmed = cursor.trim();
        if (!trimmed.matches("\\d+")) {
            throw new IllegalArgumentException("Invalid Redis scan cursor format: " + trimmed);
        }
        return trimmed;
    }

    public void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Cache key parameter must not be blank");
        }
    }

    public void validatePurgePattern(String pattern) {
        if (pattern == null || pattern.isBlank()) {
            throw new IllegalArgumentException("Purge pattern must not be empty");
        }
        // Strip bracketed character classes (e.g. [a-z]) and wildcards so [a-z]* cannot bypass prefix length
        String stripped = pattern.replaceAll("\\[[^\\]]*\\]", "")
                .replaceAll("[*?\\[\\]]", "")
                .trim();
        if (stripped.length() < MIN_PURGE_PREFIX_LENGTH) {
            throw new IllegalArgumentException(
                    "Purge pattern is too broad; must contain at least " + MIN_PURGE_PREFIX_LENGTH + " literal non-wildcard characters"
            );
        }
    }
}