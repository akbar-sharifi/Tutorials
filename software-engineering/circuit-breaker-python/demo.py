"""Simulates a downstream "inventory service" that goes healthy -> degraded -> recovered
and shows how the circuit breaker reacts. Run: python3 demo.py
"""

import random
import time

from circuit_breaker import (
    CallNotPermittedError,
    CallTimeoutError,
    CircuitBreaker,
    CircuitBreakerConfig,
)


class InventoryService:
    """A fake remote service whose behaviour we can switch at runtime."""

    def __init__(self) -> None:
        self.mode = "HEALTHY"
        self._random = random.Random(7)  # fixed seed -> reproducible demo output

    def stock_for(self, sku: str) -> int:
        if self.mode == "DEGRADED":
            if self._random.randrange(100) < 60:
                raise ConnectionError("503 Service Unavailable")
            time.sleep(0.4)  # the calls that do "succeed" are very slow
        else:
            time.sleep(0.02)
        return 42


def run(breaker: CircuitBreaker, service: InventoryService, calls: int) -> None:
    for i in range(1, calls + 1):
        before = breaker.state
        start = time.perf_counter()
        try:
            outcome = f"OK stock={breaker.call(service.stock_for, 'SKU-123')}"
        except CallNotPermittedError:
            outcome = "REJECTED (fail fast) -> serving cached value"
        except CallTimeoutError:
            outcome = "TIMEOUT"
        except Exception as exc:  # noqa: BLE001
            outcome = f"FAILED: {exc}"
        ms = (time.perf_counter() - start) * 1000
        print(f"  call {i:2d} [{before.value:<9}] {ms:4.0f} ms  {outcome}")


def main() -> None:
    config = CircuitBreakerConfig(
        sliding_window_size=10,
        minimum_number_of_calls=5,
        failure_rate_threshold=50,          # >= 50% failures -> OPEN
        slow_call_duration_threshold=0.2,   # a call >= 200 ms is "slow"
        slow_call_rate_threshold=50,        # >= 50% slow calls -> OPEN
        call_timeout=0.3,                   # give up on a call after 300 ms
        wait_duration_in_open_state=1.0,    # stay OPEN for 1 s before probing
        permitted_calls_in_half_open_state=2,  # 2 good probes -> CLOSED
    )
    service = InventoryService()
    breaker = CircuitBreaker("inventory", config)
    breaker.on_state_transition(lambda t: print(f"    *** {t} ***"))

    print("== Phase 1: downstream healthy ==")
    run(breaker, service, 5)

    print("\n== Phase 2: downstream degraded (failing + slow) ==")
    service.mode = "DEGRADED"
    run(breaker, service, 12)

    print("\n== Phase 3: downstream recovers; wait for OPEN -> HALF_OPEN ==")
    service.mode = "RECOVERED"
    time.sleep(1.1)
    run(breaker, service, 5)

    print(f"\nFinal state: {breaker.state.value}, metrics: {breaker.metrics}")


if __name__ == "__main__":
    main()
