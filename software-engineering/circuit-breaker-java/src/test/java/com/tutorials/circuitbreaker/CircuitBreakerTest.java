package com.tutorials.circuitbreaker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dependency-free tests (no JUnit needed). Run with {@code java -ea}.
 * A controllable {@link MutableClock} lets us simulate time passing and slow calls without sleeping.
 */
public class CircuitBreakerTest {

    static final class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private static int passed = 0;
    private static final List<String> failures = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        test("starts CLOSED and passes calls through", CircuitBreakerTest::startsClosed);
        test("does not trip before minimumNumberOfCalls", CircuitBreakerTest::respectsMinimumCalls);
        test("trips OPEN when failure rate reaches threshold", CircuitBreakerTest::tripsOnFailureRate);
        test("OPEN rejects calls without invoking downstream", CircuitBreakerTest::openFailsFast);
        test("trips OPEN when slow-call rate reaches threshold", CircuitBreakerTest::tripsOnSlowCalls);
        test("OPEN -> HALF_OPEN after wait duration", CircuitBreakerTest::openToHalfOpen);
        test("HALF_OPEN -> CLOSED after all trial calls succeed", CircuitBreakerTest::halfOpenToClosed);
        test("HALF_OPEN -> OPEN on a failed trial call", CircuitBreakerTest::halfOpenToOpenOnFailure);
        test("HALF_OPEN -> OPEN on a slow trial call", CircuitBreakerTest::halfOpenToOpenOnSlow);
        test("HALF_OPEN limits number of trial calls", CircuitBreakerTest::halfOpenLimitsTrials);
        test("sliding window evicts old outcomes", CircuitBreakerTest::slidingWindowEvicts);
        test("call timeout aborts call and counts as failure", CircuitBreakerTest::timeoutCountsAsFailure);
        test("fallback is used on failure and on rejection", CircuitBreakerTest::fallback);
        test("forceOpen and reset", CircuitBreakerTest::forceOpenAndReset);
        test("thread-safe under concurrent load", CircuitBreakerTest::concurrentLoad);
        test("config validation", CircuitBreakerTest::configValidation);

        System.out.println();
        System.out.println(passed + " passed, " + failures.size() + " failed");
        if (!failures.isEmpty()) {
            failures.forEach(f -> System.out.println("  FAILED: " + f));
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ tests

    static void startsClosed() throws Exception {
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(new MutableClock()).build());
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertEquals("ok", cb.call(() -> "ok"));
    }

    static void respectsMinimumCalls() {
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(new MutableClock()).build());
        for (int i = 0; i < 4; i++) fail(cb); // minimumNumberOfCalls = 5
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertEquals(-1f, cb.getMetrics().failureRate());
    }

    static void tripsOnFailureRate() {
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(new MutableClock()).build());
        List<CircuitBreaker.StateTransition> events = new ArrayList<>();
        cb.onStateTransition(events::add);
        succeed(cb);
        succeed(cb);
        succeed(cb);
        fail(cb);
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        fail(cb); // 2/5 = 40% < 50%
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        fail(cb); // 3/6 = 50% -> trip
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        assertEquals(1, events.size());
        assertEquals(CircuitBreaker.State.OPEN, events.get(0).to());
    }

    static void openFailsFast() {
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(new MutableClock()).build());
        tripOpen(cb);
        AtomicInteger invocations = new AtomicInteger();
        assertThrows(CallNotPermittedException.class, () -> cb.call(invocations::incrementAndGet));
        assertEquals(0, invocations.get());
    }

    static void tripsOnSlowCalls() throws Exception {
        MutableClock clock = new MutableClock();
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(clock).build());
        for (int i = 0; i < 5; i++) {
            // each call "takes" 1s (threshold is 500ms) but succeeds
            cb.call(() -> {
                clock.advance(Duration.ofSeconds(1));
                return "slow but ok";
            });
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        assertEquals(0, cb.getMetrics().failedCalls());
    }

    static void openToHalfOpen() {
        MutableClock clock = new MutableClock();
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(clock).build());
        tripOpen(cb);
        clock.advance(Duration.ofSeconds(9));
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        clock.advance(Duration.ofSeconds(1));
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
    }

    static void halfOpenToClosed() {
        MutableClock clock = new MutableClock();
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(clock).build());
        tripOpen(cb);
        clock.advance(Duration.ofSeconds(10));
        succeed(cb);
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
        succeed(cb);
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertEquals(0, cb.getMetrics().bufferedCalls()); // window reset on close
    }

    static void halfOpenToOpenOnFailure() {
        MutableClock clock = new MutableClock();
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(clock).build());
        tripOpen(cb);
        clock.advance(Duration.ofSeconds(10));
        succeed(cb);
        fail(cb);
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        // and the OPEN timer restarts
        clock.advance(Duration.ofSeconds(5));
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }

    static void halfOpenToOpenOnSlow() throws Exception {
        MutableClock clock = new MutableClock();
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(clock).build());
        tripOpen(cb);
        clock.advance(Duration.ofSeconds(10));
        cb.call(() -> {
            clock.advance(Duration.ofMillis(600));
            return "slow";
        });
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }

    static void halfOpenLimitsTrials() throws Exception {
        MutableClock clock = new MutableClock();
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(clock).build());
        tripOpen(cb);
        clock.advance(Duration.ofSeconds(10));

        // Two in-flight trial calls hold both permits; a third caller must be rejected.
        CountDownLatch inFlight = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 2; i++) {
                pool.submit(() -> cb.call(() -> {
                    inFlight.countDown();
                    release.await();
                    return "ok";
                }));
            }
            assertTrue(inFlight.await(2, TimeUnit.SECONDS), "trial calls started");
            CallNotPermittedException e = assertThrows(CallNotPermittedException.class, () -> cb.call(() -> "x"));
            assertEquals(CircuitBreaker.State.HALF_OPEN, e.getState());
            release.countDown();
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(2, TimeUnit.SECONDS), "pool terminated");
        }
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }

    static void slidingWindowEvicts() {
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(new MutableClock()).slidingWindowSize(5).build());
        fail(cb);
        fail(cb);
        for (int i = 0; i < 5; i++) succeed(cb); // pushes both failures out of the window
        CircuitBreaker.Metrics m = cb.getMetrics();
        assertEquals(5, m.bufferedCalls());
        assertEquals(0, m.failedCalls());
        assertEquals(0f, m.failureRate());
    }

    static void timeoutCountsAsFailure() {
        CircuitBreaker cb = new CircuitBreaker("t", CircuitBreakerConfig.builder()
                .slidingWindowSize(2).minimumNumberOfCalls(2).failureRateThreshold(100)
                .callTimeout(Duration.ofMillis(50)).build());
        try (cb) {
            AtomicInteger interrupted = new AtomicInteger();
            for (int i = 0; i < 2; i++) {
                long start = System.nanoTime();
                assertThrows(CallTimeoutException.class, () -> cb.call(() -> {
                    try {
                        Thread.sleep(5_000);
                    } catch (InterruptedException e) {
                        interrupted.incrementAndGet();
                    }
                    return "never";
                }));
                long ms = (System.nanoTime() - start) / 1_000_000;
                assertTrue(ms < 1_000, "caller released promptly (took " + ms + "ms)");
            }
            assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        }
    }

    static void fallback() {
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(new MutableClock()).build());
        String v = cb.executeWithFallback(() -> { throw new RuntimeException("boom"); }, t -> "fallback:" + t.getMessage());
        assertEquals("fallback:boom", v);
        tripOpen(cb);
        String v2 = cb.executeWithFallback(() -> "live", t -> "cached");
        assertEquals("cached", v2);
    }

    static void forceOpenAndReset() {
        CircuitBreaker cb = new CircuitBreaker("t", baseConfig(new MutableClock()).build());
        cb.forceOpen();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        cb.reset();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertEquals("ok", cb.execute(() -> "ok"));
    }

    static void concurrentLoad() throws Exception {
        CircuitBreaker cb = new CircuitBreaker("t", CircuitBreakerConfig.builder()
                .slidingWindowSize(100).minimumNumberOfCalls(100).failureRateThreshold(100).build());
        ExecutorService pool = Executors.newFixedThreadPool(8);
        AtomicInteger ok = new AtomicInteger();
        for (int i = 0; i < 10_000; i++) {
            pool.submit(() -> { if ("ok".equals(cb.execute(() -> "ok"))) ok.incrementAndGet(); });
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS), "pool terminated");
        assertEquals(10_000, ok.get());
        assertEquals(100, cb.getMetrics().bufferedCalls());
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }

    static void configValidation() {
        assertThrows(IllegalArgumentException.class, () -> CircuitBreakerConfig.builder().failureRateThreshold(0));
        assertThrows(IllegalArgumentException.class, () -> CircuitBreakerConfig.builder().failureRateThreshold(101));
        assertThrows(IllegalArgumentException.class,
                () -> CircuitBreakerConfig.builder().slidingWindowSize(5).minimumNumberOfCalls(6).build());
        assertThrows(IllegalArgumentException.class,
                () -> CircuitBreakerConfig.builder().waitDurationInOpenState(Duration.ZERO));
    }

    // ------------------------------------------------------------------ helpers

    /** window 10, min 5 calls, 50% thresholds, slow >= 500ms, open 10s, 2 half-open trials. */
    static CircuitBreakerConfig.Builder baseConfig(Clock clock) {
        return CircuitBreakerConfig.builder()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .slowCallRateThreshold(50)
                .slowCallDurationThreshold(Duration.ofMillis(500))
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedCallsInHalfOpenState(2)
                .clock(clock);
    }

    static void succeed(CircuitBreaker cb) {
        cb.execute(() -> "ok");
    }

    static void fail(CircuitBreaker cb) {
        try {
            cb.execute(() -> { throw new RuntimeException("downstream error"); });
        } catch (RuntimeException expected) {
            // expected
        }
    }

    static void tripOpen(CircuitBreaker cb) {
        for (int i = 0; i < 5; i++) fail(cb);
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }

    interface ThrowingRunnable {
        void run() throws Exception;
    }

    static void test(String name, ThrowingRunnable body) {
        try {
            body.run();
            passed++;
            System.out.println("  PASS  " + name);
        } catch (Throwable t) {
            failures.add(name + " -> " + t);
            System.out.println("  FAIL  " + name + " -> " + t);
        }
    }

    static <T extends Throwable> T assertThrows(Class<T> type, ThrowingRunnable body) {
        try {
            body.run();
        } catch (Throwable t) {
            if (type.isInstance(t)) return type.cast(t);
            throw new AssertionError("expected " + type.getSimpleName() + " but got " + t, t);
        }
        throw new AssertionError("expected " + type.getSimpleName() + " but nothing was thrown");
    }

    static void assertEquals(Object expected, Object actual) {
        if (!java.util.Objects.equals(expected, actual)) {
            throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
        }
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
