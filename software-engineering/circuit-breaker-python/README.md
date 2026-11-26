# Circuit Breaker Pattern in Python

A small, dependency-free, thread-safe **Circuit Breaker** for protecting a service from a downstream dependency that is **failing** or **getting slow**. It's a Python port of the [Java version](../circuit-breaker-java/README.md) with the same design, written in idiomatic Python (dataclasses, properties, a decorator).

Requires Python 3.9+ and only the standard library.

## Why you need it

When a dependency degrades, callers that keep hitting it wait on every timeout, tie up threads and workers, and spread the failure upstream. A circuit breaker watches the outcomes of recent calls. Once too many fail **or are too slow**, it stops calling the dependency for a while, failing fast or serving a fallback, so the dependency has room to recover.

## The state machine

```
             failure rate ≥ threshold  OR  slow-call rate ≥ threshold
   CLOSED ──────────────────────────────────────────────────────────► OPEN
     ▲                                                                │ ▲
     │ all N trial calls succeed (and are fast)     wait duration     │ │
     │                                              elapsed           ▼ │
     └─────────────────────────────────────────────────────── HALF_OPEN │
                              any trial call fails or is slow ──────────┘
```

| State | Behaviour |
|---|---|
| **CLOSED** | Calls pass through. Each outcome (failed? slow?) goes into a sliding window of the last `sliding_window_size` calls. |
| **OPEN** | Every call raises `CallNotPermittedError` immediately. The dependency is not touched. |
| **HALF_OPEN** | After `wait_duration_in_open_state`, up to `permitted_calls_in_half_open_state` trial calls go through. If all succeed quickly → CLOSED. If any fails or is slow → OPEN. |

## Layout

```
circuit-breaker-python/
├── run.sh                      # tests + demo
├── demo.py                     # simulated degrading downstream service
├── circuit_breaker/
│   ├── __init__.py             # public API
│   ├── breaker.py              # CircuitBreaker, state machine, sliding window
│   ├── config.py               # CircuitBreakerConfig (frozen, validated dataclass)
│   └── exceptions.py           # CallNotPermittedError, CallTimeoutError
└── tests/
    └── test_circuit_breaker.py # 20 unittest cases, uses a fake clock
```

```bash
./run.sh                                   # or:
python3 -m unittest discover -v -s tests -t .
python3 demo.py
```

## Usage

```python
from circuit_breaker import (
    CircuitBreaker, CircuitBreakerConfig, CallNotPermittedError, CallTimeoutError,
)

config = CircuitBreakerConfig(
    sliding_window_size=20,               # judge the last 20 calls
    minimum_number_of_calls=10,           # ...but only once there are 10
    failure_rate_threshold=50,            # ≥ 50% failures   -> OPEN
    slow_call_duration_threshold=1.0,     # ≥ 1 s is "slow"
    slow_call_rate_threshold=50,          # ≥ 50% slow calls -> OPEN
    call_timeout=2.0,                     # stop waiting after 2 s
    wait_duration_in_open_state=30,       # fail fast for 30 s
    permitted_calls_in_half_open_state=3, # 3 good probes -> CLOSED
)
breaker = CircuitBreaker("inventory-service", config)
breaker.on_state_transition(lambda t: log.warning("%s", t))

# 1. Plain call: positional and keyword args are passed through
try:
    stock = breaker.call(client.get_stock, "SKU-123", region="eu")
except CallNotPermittedError:
    ...  # breaker is open: fail fast
except CallTimeoutError:
    ...  # downstream too slow

# 2. With a fallback: used on failure, timeout, *and* rejection
stock = breaker.call_with_fallback(
    client.get_stock, lambda exc: cache.get("SKU-123", 0), "SKU-123")

# 3. As a decorator
@breaker
def get_stock(sku: str) -> int:
    return client.get_stock(sku)
```

All durations are **seconds as floats** (`0.2` = 200 ms).

## Demo output

```
== Phase 2: downstream degraded (failing + slow) ==
  call  1 [CLOSED   ]    0 ms  FAILED: 503 Service Unavailable
  call  2 [CLOSED   ]    0 ms  FAILED: 503 Service Unavailable
  call  3 [CLOSED   ]    0 ms  FAILED: 503 Service Unavailable
  call  4 [CLOSED   ]  302 ms  TIMEOUT
    *** CircuitBreaker 'inventory' CLOSED -> OPEN ***
  call  5 [CLOSED   ]    0 ms  FAILED: 503 Service Unavailable
  call  6 [OPEN     ]    0 ms  REJECTED (fail fast) -> serving cached value
  ...
== Phase 3: downstream recovers; wait for OPEN -> HALF_OPEN ==
    *** CircuitBreaker 'inventory' OPEN -> HALF_OPEN ***
  call  1 [HALF_OPEN]   21 ms  OK stock=42
    *** CircuitBreaker 'inventory' HALF_OPEN -> CLOSED ***
  call  2 [HALF_OPEN]   21 ms  OK stock=42
  call  3 [CLOSED   ]   21 ms  OK stock=42
```

## How it works, step by step

1. **Get permission** (`_acquire_permission`). Under the lock, the breaker first checks whether an OPEN breaker's wait has elapsed and moves it to HALF_OPEN if so. Then CLOSED allows the call, OPEN raises `CallNotPermittedError`, and HALF_OPEN hands out one of its limited trial slots or raises. It returns the current **generation**.
2. **Run the call** (`_invoke`) **outside the lock**, timed with `config.clock`. A hanging dependency never blocks other callers from being rejected quickly.
3. **Timeout.** If `call_timeout` is set, the call runs on a daemon thread and the caller waits on a `threading.Event` for at most `call_timeout`. *Python cannot kill a thread*, so a timed-out call keeps running in the background; the caller is simply released and gets `CallTimeoutError`. Daemon threads never block interpreter shutdown. Exceptions from the call are re-raised in the caller with their original traceback.
4. **Record the outcome** (`_on_result`). A call is *failed* if it raised and *slow* if it took ≥ `slow_call_duration_threshold`. A call can be both; a timeout always is. Any `BaseException` counts as a failure, so a HALF_OPEN trial slot is never leaked.
5. **Sliding window** (`_SlidingWindow`). A `bytearray` ring buffer with one byte per call (bit 0 = failed, bit 1 = slow) and running totals. Recording evicts the oldest outcome, so recording and reading the rates are both O(1). Rates are `-1` until `minimum_number_of_calls` is reached, so one failure out of two calls can't trip the breaker.
6. **Decide.** In CLOSED, trip to OPEN when either rate reaches its threshold. In HALF_OPEN, any failed or slow trial → OPEN, and N successes → CLOSED.
7. **Discard stale results.** Every transition increments `_generation`. A result whose permit came from an older generation is ignored. Without this, a slow call started while CLOSED could finish during HALF_OPEN and count as a successful trial, closing the breaker too early. (`test_stale_result_from_before_transition_is_ignored` covers this.)
8. **Lazy OPEN → HALF_OPEN.** No timer thread: the check runs whenever someone reads `.state` or makes a call.
9. **Transition** (`_transition_to`). The one place state changes. It bumps the generation, resets what the new state needs (window on CLOSED, timer on OPEN, trial counters on HALF_OPEN) and notifies listeners. A listener that raises is ignored. The lock is an `RLock`, so a listener may read `breaker.state` or `breaker.metrics`.
10. **Testable time.** The clock is any zero-argument callable returning seconds (default `time.monotonic`). Tests pass a `FakeClock` and advance it by hand to simulate slow calls and the OPEN wait without sleeping.

## Differences from the Java version

| | Java | Python |
|---|---|---|
| Durations | `java.time.Duration` | float seconds |
| Timeout mechanism | cached thread pool, `Future.get(timeout)`, worker interrupted | daemon thread per call, `Event.wait(timeout)`. Worker can't be interrupted. |
| API | `call(Callable)`, `execute(Supplier)`, `executeWithFallback` | `call(fn, *args, **kwargs)`, `call_with_fallback`, `@breaker` decorator |
| Config | builder | frozen dataclass validated in `__post_init__` |
| Lock | `synchronized` (re-entrant) | `threading.RLock` |

## Where to go next

- **Exception filtering**, e.g. don't count `404`s or validation errors as downstream failures. Add an `ignore_exceptions` tuple to the config and check it in `call`.
- **asyncio support**: an `async def acall()` using `asyncio.wait_for` for timeouts. Unlike threads, tasks *can* be cancelled.
- **Time-based windows** ("last 60 seconds") as well as count-based ones.
- Libraries such as **pybreaker** or **tenacity** (for retries) for production use. Never retry into an open breaker: treat `CallNotPermittedError` as final.
