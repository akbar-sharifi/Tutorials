package com.tutorials.circuitbreaker;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A basic, thread-safe circuit breaker that protects callers from a downstream service that is
 * failing or getting slow.
 *
 * <pre>
 *            failure rate or slow-call rate >= threshold
 *   CLOSED ─────────────────────────────────────────────► OPEN
 *     ▲                                                   │  │
 *     │ all trial calls succeed          wait duration    │  │
 *     │                                  elapsed          ▼  │
 *     └──────────────────────────────────────────── HALF_OPEN │
 *                       any trial call fails or is slow ──────┘ (back to OPEN)
 * </pre>
 *
 * <ul>
 *   <li><b>CLOSED</b> – calls flow through; outcomes are recorded in a count-based sliding window.</li>
 *   <li><b>OPEN</b> – calls are rejected immediately with {@link CallNotPermittedException}
 *       (fail fast, give the downstream time to recover).</li>
 *   <li><b>HALF_OPEN</b> – a limited number of trial calls are let through to probe recovery.</li>
 * </ul>
 */
public final class CircuitBreaker implements AutoCloseable {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    /** Emitted to listeners whenever the breaker changes state. */
    public record StateTransition(String breakerName, State from, State to, Instant at) {
        @Override
        public String toString() {
            return "CircuitBreaker '" + breakerName + "' " + from + " -> " + to;
        }
    }

    private final String name;
    private final CircuitBreakerConfig config;
    private final List<Consumer<StateTransition>> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService timeoutExecutor;

    // ---- mutable state, guarded by "this" ----
    private State state = State.CLOSED;
    /**
     * Incremented on every state transition. A call remembers the generation it started in;
     * its result is ignored if the breaker has transitioned since (a stale result from a call
     * that began while CLOSED must not count as a HALF_OPEN trial, for example).
     */
    private long generation = 0;
    private final SlidingWindow window;
    private Instant openedAt;
    private int halfOpenPermitsIssued;
    private int halfOpenSuccesses;

    public CircuitBreaker(String name, CircuitBreakerConfig config) {
        this.name = Objects.requireNonNull(name, "name");
        this.config = Objects.requireNonNull(config, "config");
        this.window = new SlidingWindow(config.slidingWindowSize);
        this.timeoutExecutor = config.callTimeout == null ? null : newDaemonExecutor(name);
    }

    public static CircuitBreaker ofDefaults(String name) {
        return new CircuitBreaker(name, CircuitBreakerConfig.ofDefaults());
    }

    // ------------------------------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------------------------------

    /**
     * Runs {@code callable} through the breaker.
     *
     * @throws CallNotPermittedException if the breaker is rejecting calls
     * @throws CallTimeoutException      if {@code callTimeout} is configured and exceeded
     * @throws Exception                 whatever the callable itself throws
     */
    public <T> T call(Callable<T> callable) throws Exception {
        Objects.requireNonNull(callable, "callable");
        long permitGeneration = acquirePermission();

        Instant start = config.clock.instant();
        try {
            T result = invoke(callable);
            onResult(permitGeneration, false, elapsedSince(start));
            return result;
        } catch (CallTimeoutException e) {
            onResult(permitGeneration, true, config.callTimeout);
            throw e;
        } catch (Exception | Error e) {
            onResult(permitGeneration, true, elapsedSince(start));
            throw e;
        }
    }

    /** Like {@link #call(Callable)} but for code that throws only unchecked exceptions. */
    public <T> T execute(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        try {
            return call(supplier::get);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // Cannot happen: a Supplier cannot throw a checked exception.
            throw new IllegalStateException(e);
        }
    }

    /** Runs {@code supplier}; on any failure (including rejection by the breaker) returns the fallback. */
    public <T> T executeWithFallback(Supplier<T> supplier, Function<Throwable, T> fallback) {
        Objects.requireNonNull(fallback, "fallback");
        try {
            return execute(supplier);
        } catch (RuntimeException e) {
            return fallback.apply(e);
        }
    }

    /** Returns the current state, first moving OPEN -> HALF_OPEN if the wait duration has elapsed. */
    public synchronized State getState() {
        maybeTransitionFromOpenToHalfOpen();
        return state;
    }

    public synchronized Metrics getMetrics() {
        return new Metrics(window.size(), window.failures(), window.slowCalls(),
                window.failureRate(), window.slowCallRate());
    }

    public String getName() {
        return name;
    }

    public void onStateTransition(Consumer<StateTransition> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    /** Manually force the breaker OPEN (e.g. from an admin endpoint during an incident). */
    public synchronized void forceOpen() {
        transitionTo(State.OPEN);
    }

    /** Manually reset the breaker to CLOSED with an empty window. */
    public synchronized void reset() {
        transitionTo(State.CLOSED);
    }

    @Override
    public void close() {
        if (timeoutExecutor != null) timeoutExecutor.shutdownNow();
    }

    /** Snapshot of the sliding-window statistics. Rates are -1 until minimumNumberOfCalls is reached. */
    public record Metrics(int bufferedCalls, int failedCalls, int slowCalls,
                          float failureRate, float slowCallRate) {
    }

    // ------------------------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------------------------

    private <T> T invoke(Callable<T> callable) throws Exception {
        if (timeoutExecutor == null) {
            return callable.call();
        }
        Future<T> future = timeoutExecutor.submit(callable);
        try {
            return future.get(config.callTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            future.cancel(true); // interrupt the worker; the downstream call is abandoned
            throw new CallTimeoutException(name, config.callTimeout);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) throw ex;
            if (cause instanceof Error err) throw err;
            throw e;
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    /** Returns the generation in which the permit was granted, or throws if no permit is available. */
    private synchronized long acquirePermission() {
        maybeTransitionFromOpenToHalfOpen();
        switch (state) {
            case CLOSED:
                return generation;
            case OPEN:
                throw new CallNotPermittedException(name, State.OPEN);
            case HALF_OPEN:
                if (halfOpenPermitsIssued < config.permittedCallsInHalfOpenState) {
                    halfOpenPermitsIssued++;
                    return generation;
                }
                throw new CallNotPermittedException(name, State.HALF_OPEN);
            default:
                throw new IllegalStateException("Unknown state " + state);
        }
    }

    private synchronized void onResult(long permitGeneration, boolean failed, Duration duration) {
        if (permitGeneration != generation) {
            return; // stale result from before the last transition – ignore it
        }
        boolean slow = duration.compareTo(config.slowCallDurationThreshold) >= 0;

        switch (state) {
            case CLOSED -> {
                window.record(failed, slow);
                if (window.size() >= config.minimumNumberOfCalls
                        && (window.failureRate() >= config.failureRateThreshold
                        || window.slowCallRate() >= config.slowCallRateThreshold)) {
                    transitionTo(State.OPEN);
                }
            }
            case HALF_OPEN -> {
                if (failed || slow) {
                    transitionTo(State.OPEN); // downstream still unhealthy
                } else if (++halfOpenSuccesses >= config.permittedCallsInHalfOpenState) {
                    transitionTo(State.CLOSED); // downstream recovered
                }
            }
            case OPEN -> {
                // Only reachable via forceOpen() racing an in-flight call with the same generation,
                // which cannot happen because forceOpen() bumps the generation. Nothing to do.
            }
        }
    }

    private void maybeTransitionFromOpenToHalfOpen() {
        if (state == State.OPEN
                && !config.clock.instant().isBefore(openedAt.plus(config.waitDurationInOpenState))) {
            transitionTo(State.HALF_OPEN);
        }
    }

    /** Caller must hold the lock. */
    private void transitionTo(State newState) {
        State old = state;
        state = newState;
        generation++;
        switch (newState) {
            case CLOSED -> window.clear();
            case OPEN -> openedAt = config.clock.instant();
            case HALF_OPEN -> {
                halfOpenPermitsIssued = 0;
                halfOpenSuccesses = 0;
            }
        }
        if (old != newState) {
            StateTransition event = new StateTransition(name, old, newState, config.clock.instant());
            for (Consumer<StateTransition> l : listeners) {
                try {
                    l.accept(event);
                } catch (RuntimeException ignored) {
                    // a misbehaving listener must never break the breaker
                }
            }
        }
    }

    private Duration elapsedSince(Instant start) {
        return Duration.between(start, config.clock.instant());
    }

    private static ExecutorService newDaemonExecutor(String name) {
        AtomicInteger n = new AtomicInteger();
        return Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "circuit-breaker-" + name + "-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Fixed-size ring buffer of the last N call outcomes with running totals, so recording a call
     * and computing the rates are both O(1).
     */
    private final class SlidingWindow {
        private static final byte FAILED = 1;
        private static final byte SLOW = 2;

        private final byte[] outcomes;
        private int next;
        private int size;
        private int failures;
        private int slowCalls;

        SlidingWindow(int capacity) {
            this.outcomes = new byte[capacity];
        }

        void record(boolean failed, boolean slow) {
            if (size == outcomes.length) { // evict the oldest outcome
                byte evicted = outcomes[next];
                if ((evicted & FAILED) != 0) failures--;
                if ((evicted & SLOW) != 0) slowCalls--;
            } else {
                size++;
            }
            byte outcome = (byte) ((failed ? FAILED : 0) | (slow ? SLOW : 0));
            outcomes[next] = outcome;
            if (failed) failures++;
            if (slow) slowCalls++;
            next = (next + 1) % outcomes.length;
        }

        void clear() {
            next = size = failures = slowCalls = 0;
        }

        int size() {
            return size;
        }

        int failures() {
            return failures;
        }

        int slowCalls() {
            return slowCalls;
        }

        float failureRate() {
            return size < config.minimumNumberOfCalls ? -1f : failures * 100f / size;
        }

        float slowCallRate() {
            return size < config.minimumNumberOfCalls ? -1f : slowCalls * 100f / size;
        }
    }
}
