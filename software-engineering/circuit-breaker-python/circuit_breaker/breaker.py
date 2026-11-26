"""A basic, thread-safe circuit breaker for downstream services that fail or get slow.

::

               failure rate or slow-call rate >= threshold
      CLOSED ─────────────────────────────────────────────► OPEN
        ▲                                                   │  ▲
        │ all trial calls succeed            wait duration  │  │
        │                                    elapsed        ▼  │
        └───────────────────────────────────────────── HALF_OPEN
                         any trial call fails or is slow ──────┘

* **CLOSED**: calls flow through; outcomes go into a count-based sliding window.
* **OPEN**: calls are rejected immediately with :class:`CallNotPermittedError`.
* **HALF_OPEN**: a limited number of trial calls probe whether the service recovered.
"""

from __future__ import annotations

import functools
import threading
from dataclasses import dataclass
from enum import Enum
from typing import Any, Callable, List, Optional, TypeVar

from .config import CircuitBreakerConfig
from .exceptions import CallNotPermittedError, CallTimeoutError

T = TypeVar("T")


class State(Enum):
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"


@dataclass(frozen=True)
class StateTransition:
    """Passed to listeners whenever the breaker changes state."""

    breaker_name: str
    from_state: State
    to_state: State
    at: float  # value of config.clock() at the moment of the transition

    def __str__(self) -> str:
        return (
            f"CircuitBreaker '{self.breaker_name}' "
            f"{self.from_state.value} -> {self.to_state.value}"
        )


@dataclass(frozen=True)
class Metrics:
    """Snapshot of the sliding window. Rates are -1 until minimum_number_of_calls is reached."""

    buffered_calls: int
    failed_calls: int
    slow_calls: int
    failure_rate: float
    slow_call_rate: float


class _SlidingWindow:
    """Fixed-size ring buffer of the last N outcomes with running totals.

    Recording a call and reading the rates are both O(1).
    """

    _FAILED = 1  # bit 0
    _SLOW = 2  # bit 1

    def __init__(self, capacity: int, minimum_number_of_calls: int) -> None:
        self._outcomes = bytearray(capacity)
        self._minimum = minimum_number_of_calls
        self.clear()

    def record(self, failed: bool, slow: bool) -> None:
        if self.size == len(self._outcomes):  # full: evict the oldest outcome
            evicted = self._outcomes[self._next]
            if evicted & self._FAILED:
                self.failures -= 1
            if evicted & self._SLOW:
                self.slow_calls -= 1
        else:
            self.size += 1
        self._outcomes[self._next] = (self._FAILED if failed else 0) | (self._SLOW if slow else 0)
        if failed:
            self.failures += 1
        if slow:
            self.slow_calls += 1
        self._next = (self._next + 1) % len(self._outcomes)

    def clear(self) -> None:
        self._next = 0
        self.size = 0
        self.failures = 0
        self.slow_calls = 0

    def failure_rate(self) -> float:
        return -1.0 if self.size < self._minimum else self.failures * 100.0 / self.size

    def slow_call_rate(self) -> float:
        return -1.0 if self.size < self._minimum else self.slow_calls * 100.0 / self.size


class CircuitBreaker:
    """Protects callers from a failing or slow downstream service.

    Usage::

        breaker = CircuitBreaker("inventory", CircuitBreakerConfig(call_timeout=2.0))

        stock = breaker.call(client.get_stock, "SKU-123")          # plain call
        stock = breaker.call_with_fallback(                          # with fallback
            client.get_stock, lambda exc: cache.get("SKU-123"), "SKU-123")

        @breaker                                                     # as a decorator
        def get_stock(sku): ...
    """

    def __init__(self, name: str, config: Optional[CircuitBreakerConfig] = None) -> None:
        self.name = name
        self.config = config or CircuitBreakerConfig()
        self._listeners: List[Callable[[StateTransition], None]] = []
        # Re-entrant so a listener may safely call breaker.state / breaker.metrics.
        self._lock = threading.RLock()

        # ---- mutable state, guarded by self._lock ----
        self._state = State.CLOSED
        # Incremented on every transition. A call remembers the generation it was
        # granted a permit in; its result is ignored if the breaker has moved on.
        self._generation = 0
        self._window = _SlidingWindow(
            self.config.sliding_window_size, self.config.minimum_number_of_calls
        )
        self._opened_at = 0.0
        self._half_open_permits_issued = 0
        self._half_open_successes = 0

    # ------------------------------------------------------------------ public API

    def call(self, fn: Callable[..., T], *args: Any, **kwargs: Any) -> T:
        """Run ``fn(*args, **kwargs)`` through the breaker.

        :raises CallNotPermittedError: the breaker is rejecting calls.
        :raises CallTimeoutError: ``call_timeout`` is configured and was exceeded.
        :raises Exception: whatever ``fn`` itself raises.
        """
        permit_generation = self._acquire_permission()
        start = self.config.clock()
        try:
            result = self._invoke(fn, args, kwargs)
        except CallTimeoutError:
            # Count the full timeout as the duration, so a timed-out call is also "slow".
            # (Falls back to elapsed time if fn raised a CallTimeoutError of its own,
            # e.g. from a nested breaker, while this breaker has no timeout configured.)
            duration = self.config.call_timeout or (self.config.clock() - start)
            self._on_result(permit_generation, failed=True, duration=duration)
            raise
        except BaseException:
            # BaseException too: a HALF_OPEN permit must never be leaked without a result.
            self._on_result(permit_generation, failed=True, duration=self.config.clock() - start)
            raise
        self._on_result(permit_generation, failed=False, duration=self.config.clock() - start)
        return result

    def call_with_fallback(
        self,
        fn: Callable[..., T],
        fallback: Callable[[Exception], T],
        *args: Any,
        **kwargs: Any,
    ) -> T:
        """Like :meth:`call`, but returns ``fallback(exc)`` on any failure or rejection."""
        try:
            return self.call(fn, *args, **kwargs)
        except Exception as exc:  # noqa: BLE001 - that's the point of a fallback
            return fallback(exc)

    def __call__(self, fn: Callable[..., T]) -> Callable[..., T]:
        """Use the breaker as a decorator: ``@breaker``."""

        @functools.wraps(fn)
        def wrapper(*args: Any, **kwargs: Any) -> T:
            return self.call(fn, *args, **kwargs)

        return wrapper

    @property
    def state(self) -> State:
        """Current state, first moving OPEN -> HALF_OPEN if the wait has elapsed."""
        with self._lock:
            self._maybe_transition_from_open_to_half_open()
            return self._state

    @property
    def metrics(self) -> Metrics:
        with self._lock:
            w = self._window
            return Metrics(w.size, w.failures, w.slow_calls, w.failure_rate(), w.slow_call_rate())

    def on_state_transition(self, listener: Callable[[StateTransition], None]) -> None:
        self._listeners.append(listener)

    def force_open(self) -> None:
        """Manually open the breaker (e.g. from an admin endpoint during an incident)."""
        with self._lock:
            self._transition_to(State.OPEN)

    def reset(self) -> None:
        """Manually close the breaker with an empty window."""
        with self._lock:
            self._transition_to(State.CLOSED)

    # ------------------------------------------------------------------ internals

    def _invoke(self, fn: Callable[..., T], args: tuple, kwargs: dict) -> T:
        timeout = self.config.call_timeout
        if timeout is None:
            return fn(*args, **kwargs)  # no timeout: run on the caller's thread

        # Run on a daemon thread and wait at most `timeout`. Python cannot kill a
        # thread, so a timed-out call keeps running in the background; we just stop
        # waiting for it. Daemon threads never block interpreter shutdown.
        outcome: dict = {}
        done = threading.Event()

        def target() -> None:
            try:
                outcome["value"] = fn(*args, **kwargs)
            except BaseException as exc:  # noqa: BLE001 - re-raised in the caller
                outcome["error"] = exc
            finally:
                done.set()

        threading.Thread(target=target, name=f"circuit-breaker-{self.name}", daemon=True).start()
        if not done.wait(timeout):
            raise CallTimeoutError(self.name, timeout)
        if "error" in outcome:
            raise outcome["error"]
        return outcome["value"]

    def _acquire_permission(self) -> int:
        """Return the generation the permit was granted in, or raise if none is available."""
        with self._lock:
            self._maybe_transition_from_open_to_half_open()
            if self._state is State.CLOSED:
                return self._generation
            if self._state is State.HALF_OPEN:
                if self._half_open_permits_issued < self.config.permitted_calls_in_half_open_state:
                    self._half_open_permits_issued += 1
                    return self._generation
                raise CallNotPermittedError(self.name, State.HALF_OPEN)
            raise CallNotPermittedError(self.name, State.OPEN)

    def _on_result(self, permit_generation: int, failed: bool, duration: float) -> None:
        with self._lock:
            if permit_generation != self._generation:
                return  # stale result from before the last transition: ignore it
            slow = duration >= self.config.slow_call_duration_threshold

            if self._state is State.CLOSED:
                self._window.record(failed, slow)
                if self._window.size >= self.config.minimum_number_of_calls and (
                    self._window.failure_rate() >= self.config.failure_rate_threshold
                    or self._window.slow_call_rate() >= self.config.slow_call_rate_threshold
                ):
                    self._transition_to(State.OPEN)

            elif self._state is State.HALF_OPEN:
                if failed or slow:
                    self._transition_to(State.OPEN)  # downstream still unhealthy
                else:
                    self._half_open_successes += 1
                    if self._half_open_successes >= self.config.permitted_calls_in_half_open_state:
                        self._transition_to(State.CLOSED)  # downstream recovered

            # OPEN: unreachable, since entering OPEN bumps the generation.

    def _maybe_transition_from_open_to_half_open(self) -> None:
        """Caller must hold the lock. No timer thread: checked lazily on each access."""
        if (
            self._state is State.OPEN
            and self.config.clock() >= self._opened_at + self.config.wait_duration_in_open_state
        ):
            self._transition_to(State.HALF_OPEN)

    def _transition_to(self, new_state: State) -> None:
        """Caller must hold the lock."""
        old_state = self._state
        self._state = new_state
        self._generation += 1
        if new_state is State.CLOSED:
            self._window.clear()
        elif new_state is State.OPEN:
            self._opened_at = self.config.clock()
        else:  # HALF_OPEN
            self._half_open_permits_issued = 0
            self._half_open_successes = 0

        if old_state is not new_state:
            event = StateTransition(self.name, old_state, new_state, self.config.clock())
            for listener in list(self._listeners):
                try:
                    listener(event)
                except Exception:  # noqa: BLE001 - a bad listener must not break the breaker
                    pass
