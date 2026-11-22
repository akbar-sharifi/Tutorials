# Circuit Breaker Pattern in Java

A small, dependency-free, thread-safe implementation of the **Circuit Breaker** pattern, built to protect a service from a downstream dependency that is **failing** or **getting slow**.

## Why you need it

When a downstream service degrades, naive callers keep hammering it. Every request waits for a timeout, threads pile up, your own service runs out of resources, and the failure cascades upstream. A circuit breaker sits between the caller and the dependency and, once it sees too many bad calls, **stops calling it for a while** — failing fast (or serving a fallback) and giving the dependency room to recover.

Two signals matter:

| Signal | Example | How the breaker sees it |
|---|---|---|
| **Failures** | HTTP 503, connection refused, exceptions | Exception thrown by the call |
| **Slowness** | p99 jumps from 50 ms to 4 s | Call took ≥ `slowCallDurationThreshold` (even if it eventually succeeded) |

Slow calls are often the more dangerous signal: a dependency that answers in 10 s ties up your threads far longer than one that fails instantly.

## The state machine

```
             failure rate ≥ threshold  OR  slow-call rate ≥ threshold
   CLOSED ──────────────────────────────────────────────────────────► OPEN
     ▲                                                                │ ▲
     │ all N trial calls succeed (and are fast)     waitDuration      │ │
     │                                              elapsed           ▼ │
     └─────────────────────────────────────────────────────── HALF_OPEN │
                              any trial call fails or is slow ──────────┘
```

- **CLOSED** — normal operation. Every outcome (failed? slow?) goes into a sliding window of the last `slidingWindowSize` calls. Once at least `minimumNumberOfCalls` are recorded, the breaker trips if either rate reaches its threshold.
- **OPEN** — every call is rejected immediately with `CallNotPermittedException`. The dependency is not touched at all.
- **HALF_OPEN** — after `waitDurationInOpenState`, a limited number of trial calls (`permittedCallsInHalfOpenState`) go through. If they all succeed quickly, the breaker closes; if any fails or is slow, it opens again and the timer restarts. Extra callers beyond the trial slots are rejected.

## Project layout

```
circuit-breaker-java/
├── run.sh                                   # compile + test + demo (plain javac, Java 17+)
└── src
    ├── main/java/com/tutorials/circuitbreaker
    │   ├── CircuitBreaker.java              # the state machine + sliding window
    │   ├── CircuitBreakerConfig.java        # immutable config with a builder
    │   ├── CallNotPermittedException.java   # thrown when OPEN / HALF_OPEN is full
    │   ├── CallTimeoutException.java        # thrown when callTimeout is exceeded
    │   └── demo/CircuitBreakerDemo.java     # simulated degrading downstream service
    └── test/java/com/tutorials/circuitbreaker
        └── CircuitBreakerTest.java          # 16 tests, no JUnit needed
```

Run everything:

```bash
./run.sh
```

## Usage

```java
CircuitBreakerConfig config = CircuitBreakerConfig.builder()
        .slidingWindowSize(20)                              // look at the last 20 calls
        .minimumNumberOfCalls(10)                           // don't judge on fewer than 10
        .failureRateThreshold(50)                           // ≥ 50% failures   -> OPEN
        .slowCallDurationThreshold(Duration.ofSeconds(1))   // ≥ 1 s counts as "slow"
        .slowCallRateThreshold(50)                          // ≥ 50% slow calls -> OPEN
        .callTimeout(Duration.ofSeconds(2))                 // abandon calls after 2 s
        .waitDurationInOpenState(Duration.ofSeconds(30))    // fail fast for 30 s
        .permittedCallsInHalfOpenState(3)                   // 3 good probes -> CLOSED
        .build();

CircuitBreaker breaker = new CircuitBreaker("inventory-service", config);
breaker.onStateTransition(t -> log.warn("{}", t));          // alert / metrics hook

// 1. Plain call – you handle the exceptions
try {
    Stock s = breaker.call(() -> inventoryClient.getStock(sku));
} catch (CallNotPermittedException e) {
    // breaker is open: fail fast
} catch (CallTimeoutException e) {
    // downstream too slow
}

// 2. With a fallback – used on failure, timeout, *and* rejection
Stock s = breaker.executeWithFallback(
        () -> inventoryClient.getStock(sku),
        error -> stockCache.getOrDefault(sku, Stock.UNKNOWN));
```

## Demo output

`CircuitBreakerDemo` simulates an inventory service that is healthy, then degrades (60% errors, the rest take 400 ms against a 300 ms timeout), then recovers:

```
== Phase 2: downstream degraded (failing + slow) ==
  call  1 [CLOSED   ]    1 ms  FAILED: 503 Service Unavailable
  call  2 [CLOSED   ]  307 ms  TIMEOUT
  call  3 [CLOSED   ]  300 ms  TIMEOUT
  call  4 [CLOSED   ]    0 ms  FAILED: 503 Service Unavailable
    *** CircuitBreaker 'inventory' CLOSED -> OPEN ***
  call  5 [CLOSED   ]  309 ms  TIMEOUT
  call  6 [OPEN     ]    9 ms  REJECTED (fail fast) -> serving cached value
  call  7 [OPEN     ]    0 ms  REJECTED (fail fast) -> serving cached value
  ...
== Phase 3: downstream recovers; wait for OPEN -> HALF_OPEN ==
    *** CircuitBreaker 'inventory' OPEN -> HALF_OPEN ***
  call  1 [HALF_OPEN]   20 ms  OK stock=42
    *** CircuitBreaker 'inventory' HALF_OPEN -> CLOSED ***
  call  2 [HALF_OPEN]   20 ms  OK stock=42
  call  3 [CLOSED   ]   20 ms  OK stock=42
```

Notice that once the breaker opens, callers go from waiting ~300 ms per request to **0 ms** — that's the whole point.

## How the implementation works

**Sliding window (O(1)).** A fixed-size ring buffer stores one byte per call (bit 0 = failed, bit 1 = slow) plus running counters. Recording a call evicts the oldest outcome and adjusts the counters, so computing the failure and slow-call rates never scans the buffer.

**Slow-call detection.** Each call is timed using the configured `Clock`. A call taking at least `slowCallDurationThreshold` is marked slow even if it returned successfully.

**Timeouts.** If `callTimeout` is set, the call runs on a daemon thread pool and the caller waits with `Future.get(timeout)`. On timeout the future is cancelled (interrupting the worker), the call is recorded as failed + slow, and `CallTimeoutException` is thrown. Without a timeout the call runs on the caller's thread, so there's no thread-hop overhead.

**Lazy OPEN → HALF_OPEN.** No background timer: the transition happens the next time someone calls `getState()` or tries to make a call after the wait has elapsed.

**Generations to discard stale results.** Every transition increments a `generation` counter, and each call remembers the generation it was granted a permit in. A result arriving after a transition is ignored. Without this, a slow call that started while CLOSED could finish during HALF_OPEN and be wrongly counted as a successful trial, closing the breaker too early.

**Thread safety.** State changes and result recording are `synchronized` on the breaker. The protected call itself runs *outside* the lock, so a slow downstream never blocks other callers from being rejected quickly.

**Testable time.** `Clock` is injectable. The tests use a `MutableClock` that they advance by hand to simulate both the open-state wait and slow calls, so most of the suite runs without sleeping.

## Choosing settings

| Setting | Guidance |
|---|---|
| `slidingWindowSize` | Big enough to smooth noise (20–100). Bigger means slower to react. |
| `minimumNumberOfCalls` | Stops one failure out of two calls from tripping the breaker at low traffic. |
| `failureRateThreshold` | 50% is a common starting point. Lower it for critical dependencies. |
| `slowCallDurationThreshold` | Slightly above the dependency's normal p99 latency. |
| `callTimeout` | Always set one. A circuit breaker without a timeout can't protect you from a hang. |
| `waitDurationInOpenState` | Long enough for the dependency to recover (seconds to a minute). |
| `permittedCallsInHalfOpenState` | A few (1–5). Enough to be confident without flooding a fragile service. |

## Where to go next

This is deliberately basic. Production libraries such as **Resilience4j** add:
- **Exception filtering**, e.g. don't count a `404` or validation error as a downstream failure.
- **Time-based windows** ("last 60 seconds") as well as count-based ones.
- **Async / reactive APIs** (`CompletableFuture`, Reactor).
- **Metrics export** (Micrometer/Prometheus) and a registry for many named breakers.
- Composition with **Retry**, **Bulkhead** (concurrency limits) and **RateLimiter**. A typical order is `Retry(CircuitBreaker(Bulkhead(TimeLimiter(call))))`.

One rule of thumb: **never retry into an open breaker.** Retries should treat `CallNotPermittedException` as final, otherwise they defeat the purpose.
