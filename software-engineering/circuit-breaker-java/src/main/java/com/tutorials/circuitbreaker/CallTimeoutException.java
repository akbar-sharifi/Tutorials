package com.tutorials.circuitbreaker;

import java.time.Duration;

/**
 * Thrown when a protected call exceeds the configured {@code callTimeout}.
 * The breaker records such a call as both failed and slow.
 */
public class CallTimeoutException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CallTimeoutException(String breakerName, Duration timeout) {
        super("Call through CircuitBreaker '" + breakerName + "' timed out after " + timeout.toMillis() + " ms");
    }
}
