"""A basic, dependency-free circuit breaker for downstream services that fail or get slow."""

from .breaker import CircuitBreaker, Metrics, State, StateTransition
from .config import CircuitBreakerConfig
from .exceptions import CallNotPermittedError, CallTimeoutError, CircuitBreakerError

__all__ = [
    "CircuitBreaker",
    "CircuitBreakerConfig",
    "CircuitBreakerError",
    "CallNotPermittedError",
    "CallTimeoutError",
    "Metrics",
    "State",
    "StateTransition",
]
