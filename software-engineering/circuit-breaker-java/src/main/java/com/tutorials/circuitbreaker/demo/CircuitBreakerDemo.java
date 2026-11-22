package com.tutorials.circuitbreaker.demo;

import com.tutorials.circuitbreaker.CallNotPermittedException;
import com.tutorials.circuitbreaker.CallTimeoutException;
import com.tutorials.circuitbreaker.CircuitBreaker;
import com.tutorials.circuitbreaker.CircuitBreakerConfig;

import java.time.Duration;
import java.util.Random;

/**
 * Simulates a downstream "inventory service" that goes through three phases:
 * healthy -> degraded (slow + failing) -> recovered, and shows how the breaker reacts.
 *
 * Run: see the README in this folder.
 */
public class CircuitBreakerDemo {

    /** A fake remote service whose behaviour we can switch at runtime. */
    static final class InventoryService {
        enum Mode { HEALTHY, DEGRADED, RECOVERED }

        volatile Mode mode = Mode.HEALTHY;
        private final Random random = new Random(7); // fixed seed -> reproducible demo output

        int stockFor(String sku) throws InterruptedException {
            switch (mode) {
                case HEALTHY, RECOVERED -> Thread.sleep(20);
                case DEGRADED -> {
                    if (random.nextInt(100) < 60) {
                        throw new IllegalStateException("503 Service Unavailable");
                    }
                    Thread.sleep(400); // the calls that do "succeed" are very slow
                }
            }
            return 42;
        }
    }

    public static void main(String[] args) throws Exception {
        CircuitBreakerConfig config = CircuitBreakerConfig.builder()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)                          // >= 50% failures -> OPEN
                .slowCallDurationThreshold(Duration.ofMillis(200)) // a call >= 200ms is "slow"
                .slowCallRateThreshold(50)                         // >= 50% slow calls -> OPEN
                .callTimeout(Duration.ofMillis(300))               // give up on a call after 300ms
                .waitDurationInOpenState(Duration.ofSeconds(1))    // stay OPEN for 1s before probing
                .permittedCallsInHalfOpenState(2)                  // 2 good probes -> CLOSED
                .build();

        InventoryService service = new InventoryService();

        try (CircuitBreaker breaker = new CircuitBreaker("inventory", config)) {
            breaker.onStateTransition(t -> System.out.println("    *** " + t + " ***"));

            System.out.println("== Phase 1: downstream healthy ==");
            run(breaker, service, 5);

            System.out.println("\n== Phase 2: downstream degraded (failing + slow) ==");
            service.mode = InventoryService.Mode.DEGRADED;
            run(breaker, service, 12);

            System.out.println("\n== Phase 3: downstream recovers; wait for OPEN -> HALF_OPEN ==");
            service.mode = InventoryService.Mode.RECOVERED;
            Thread.sleep(1100);
            run(breaker, service, 5);

            System.out.println("\nFinal state: " + breaker.getState() + ", metrics: " + breaker.getMetrics());
        }
    }

    private static void run(CircuitBreaker breaker, InventoryService service, int calls) {
        for (int i = 1; i <= calls; i++) {
            CircuitBreaker.State before = breaker.getState();
            long start = System.nanoTime();
            String outcome;
            try {
                int stock = breaker.call(() -> service.stockFor("SKU-123"));
                outcome = "OK stock=" + stock;
            } catch (CallNotPermittedException e) {
                outcome = "REJECTED (fail fast) -> serving cached value";
            } catch (CallTimeoutException e) {
                outcome = "TIMEOUT";
            } catch (Exception e) {
                outcome = "FAILED: " + e.getMessage();
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.printf("  call %2d [%-9s] %4d ms  %s%n", i, before, ms, outcome);
        }
    }
}
