package com.tutorials.circuitbreaker;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Immutable configuration for a {@link CircuitBreaker}. Build one with {@link #builder()}.
 */
public final class CircuitBreakerConfig {

    /** Percentage (0-100] of failed calls in the window that trips the breaker. */
    final float failureRateThreshold;
    /** Percentage (0-100] of slow calls in the window that trips the breaker. */
    final float slowCallRateThreshold;
    /** A call taking at least this long is counted as "slow" (even if it succeeds). */
    final Duration slowCallDurationThreshold;
    /** Hard limit for a call; when exceeded the call is abandoned and counted as a failure. Null = no limit. */
    final Duration callTimeout;
    /** Number of most recent calls the rates are computed over (count-based sliding window). */
    final int slidingWindowSize;
    /** Rates are not evaluated until at least this many calls have been recorded. */
    final int minimumNumberOfCalls;
    /** How long the breaker stays OPEN before allowing trial calls (HALF_OPEN). */
    final Duration waitDurationInOpenState;
    /** Number of trial calls allowed in HALF_OPEN; all must succeed (and be fast) to close again. */
    final int permittedCallsInHalfOpenState;
    /** Time source; injectable so tests don't need to sleep. */
    final Clock clock;

    private CircuitBreakerConfig(Builder b) {
        this.failureRateThreshold = b.failureRateThreshold;
        this.slowCallRateThreshold = b.slowCallRateThreshold;
        this.slowCallDurationThreshold = b.slowCallDurationThreshold;
        this.callTimeout = b.callTimeout;
        this.slidingWindowSize = b.slidingWindowSize;
        this.minimumNumberOfCalls = b.minimumNumberOfCalls;
        this.waitDurationInOpenState = b.waitDurationInOpenState;
        this.permittedCallsInHalfOpenState = b.permittedCallsInHalfOpenState;
        this.clock = b.clock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CircuitBreakerConfig ofDefaults() {
        return builder().build();
    }

    public static final class Builder {
        private float failureRateThreshold = 50f;
        private float slowCallRateThreshold = 100f;
        private Duration slowCallDurationThreshold = Duration.ofSeconds(2);
        private Duration callTimeout = null;
        private int slidingWindowSize = 20;
        private int minimumNumberOfCalls = 10;
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        private int permittedCallsInHalfOpenState = 3;
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        public Builder failureRateThreshold(float percent) {
            this.failureRateThreshold = requirePercent(percent, "failureRateThreshold");
            return this;
        }

        public Builder slowCallRateThreshold(float percent) {
            this.slowCallRateThreshold = requirePercent(percent, "slowCallRateThreshold");
            return this;
        }

        public Builder slowCallDurationThreshold(Duration d) {
            this.slowCallDurationThreshold = requirePositive(d, "slowCallDurationThreshold");
            return this;
        }

        public Builder callTimeout(Duration d) {
            this.callTimeout = d == null ? null : requirePositive(d, "callTimeout");
            return this;
        }

        public Builder slidingWindowSize(int size) {
            if (size < 1) throw new IllegalArgumentException("slidingWindowSize must be >= 1");
            this.slidingWindowSize = size;
            return this;
        }

        public Builder minimumNumberOfCalls(int n) {
            if (n < 1) throw new IllegalArgumentException("minimumNumberOfCalls must be >= 1");
            this.minimumNumberOfCalls = n;
            return this;
        }

        public Builder waitDurationInOpenState(Duration d) {
            this.waitDurationInOpenState = requirePositive(d, "waitDurationInOpenState");
            return this;
        }

        public Builder permittedCallsInHalfOpenState(int n) {
            if (n < 1) throw new IllegalArgumentException("permittedCallsInHalfOpenState must be >= 1");
            this.permittedCallsInHalfOpenState = n;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        public CircuitBreakerConfig build() {
            if (minimumNumberOfCalls > slidingWindowSize) {
                throw new IllegalArgumentException(
                        "minimumNumberOfCalls (" + minimumNumberOfCalls
                                + ") must not exceed slidingWindowSize (" + slidingWindowSize + ")");
            }
            return new CircuitBreakerConfig(this);
        }

        private static float requirePercent(float p, String name) {
            if (p <= 0 || p > 100) throw new IllegalArgumentException(name + " must be in (0, 100]");
            return p;
        }

        private static Duration requirePositive(Duration d, String name) {
            Objects.requireNonNull(d, name);
            if (d.isNegative() || d.isZero()) throw new IllegalArgumentException(name + " must be > 0");
            return d;
        }
    }
}
