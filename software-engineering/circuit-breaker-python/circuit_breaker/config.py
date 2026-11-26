"""Immutable configuration for a :class:`~circuit_breaker.CircuitBreaker`."""

from __future__ import annotations

import time
from dataclasses import dataclass, field
from typing import Callable, Optional


@dataclass(frozen=True)
class CircuitBreakerConfig:
    """All durations are in **seconds** (floats are fine, e.g. ``0.2`` for 200 ms).

    Values are validated on construction, so a breaker can never run with a
    nonsensical configuration.
    """

    #: Percentage (0-100] of failed calls in the window that trips the breaker.
    failure_rate_threshold: float = 50.0
    #: Percentage (0-100] of slow calls in the window that trips the breaker.
    slow_call_rate_threshold: float = 100.0
    #: A call taking at least this long counts as "slow", even if it succeeds.
    slow_call_duration_threshold: float = 2.0
    #: Hard limit for a call; when exceeded the caller is released and the call
    #: counts as a failure. ``None`` means no limit.
    call_timeout: Optional[float] = None
    #: Number of most recent calls the rates are computed over.
    sliding_window_size: int = 20
    #: Rates are not evaluated until at least this many calls are recorded.
    minimum_number_of_calls: int = 10
    #: How long the breaker stays OPEN before allowing trial calls (HALF_OPEN).
    wait_duration_in_open_state: float = 30.0
    #: Trial calls allowed in HALF_OPEN; all must succeed (and be fast) to close.
    permitted_calls_in_half_open_state: int = 3
    #: Time source returning seconds. Injectable so tests don't need to sleep.
    clock: Callable[[], float] = field(default=time.monotonic, compare=False)

    def __post_init__(self) -> None:
        _require_percent(self.failure_rate_threshold, "failure_rate_threshold")
        _require_percent(self.slow_call_rate_threshold, "slow_call_rate_threshold")
        _require_positive(self.slow_call_duration_threshold, "slow_call_duration_threshold")
        if self.call_timeout is not None:
            _require_positive(self.call_timeout, "call_timeout")
        _require_positive(self.wait_duration_in_open_state, "wait_duration_in_open_state")
        _require_at_least_one(self.sliding_window_size, "sliding_window_size")
        _require_at_least_one(self.minimum_number_of_calls, "minimum_number_of_calls")
        _require_at_least_one(
            self.permitted_calls_in_half_open_state, "permitted_calls_in_half_open_state"
        )
        if self.minimum_number_of_calls > self.sliding_window_size:
            raise ValueError(
                f"minimum_number_of_calls ({self.minimum_number_of_calls}) must not exceed "
                f"sliding_window_size ({self.sliding_window_size})"
            )
        if not callable(self.clock):
            raise TypeError("clock must be callable")


def _require_percent(value: float, name: str) -> None:
    if not 0 < value <= 100:
        raise ValueError(f"{name} must be in (0, 100], got {value}")


def _require_positive(value: float, name: str) -> None:
    if value <= 0:
        raise ValueError(f"{name} must be > 0, got {value}")


def _require_at_least_one(value: int, name: str) -> None:
    if value < 1:
        raise ValueError(f"{name} must be >= 1, got {value}")
