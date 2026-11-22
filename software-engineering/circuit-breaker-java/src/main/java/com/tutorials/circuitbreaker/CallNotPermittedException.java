package com.tutorials.circuitbreaker;

/**
 * Thrown when the circuit breaker rejects a call without contacting the downstream service
 * (the breaker is OPEN, or HALF_OPEN with all trial slots taken).
 */
public class CallNotPermittedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CircuitBreaker.State state;

    public CallNotPermittedException(String breakerName, CircuitBreaker.State state) {
        super("CircuitBreaker '" + breakerName + "' is " + state + " and does not permit calls");
        this.state = state;
    }

    public CircuitBreaker.State getState() {
        return state;
    }
}
