package io.github.hichambenhsine.strobe.exception;

/**
 * Domain exception thrown when the underlying cache datastore is unreachable or misconfigured.
 */
public class StrobeCacheUnavailableException extends RuntimeException {

    public StrobeCacheUnavailableException(String message) {
        super(message);
    }

    public StrobeCacheUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}