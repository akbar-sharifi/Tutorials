"""Exceptions raised by the circuit breaker itself (not by the protected call)."""

from __future__ import annotations

from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from .breaker import State


class CircuitBreakerError(Exception):
    """Base class for errors raised by the breaker."""


class CallNotPermittedError(CircuitBreakerError):
    """The breaker rejected the call without contacting the downstream service.

    Raised when the breaker is OPEN, or HALF_OPEN with all trial slots taken.
    """

    def __init__(self, breaker_name: str, state: "State") -> None:
        super().__init__(
            f"CircuitBreaker '{breaker_name}' is {state.value} and does not permit calls"
        )
        self.state = state


class CallTimeoutError(CircuitBreakerError, TimeoutError):
    """The protected call exceeded ``call_timeout``. Recorded as failed and slow."""

    def __init__(self, breaker_name: str, timeout: float) -> None:
        super().__init__(
            f"Call through CircuitBreaker '{breaker_name}' timed out after "
            f"{timeout * 1000:.0f} ms"
        )
        self.timeout = timeout
