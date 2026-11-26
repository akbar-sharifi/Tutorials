"""Tests for the circuit breaker. Run from the project folder: python3 -m unittest -v

A controllable FakeClock lets us simulate time passing and slow calls without sleeping.
"""

import threading
import time
import unittest
from concurrent.futures import ThreadPoolExecutor

from circuit_breaker import (
    CallNotPermittedError,
    CallTimeoutError,
    CircuitBreaker,
    CircuitBreakerConfig,
    State,
)


class FakeClock:
    def __init__(self) -> None:
        self.now = 1_000.0

    def advance(self, seconds: float) -> None:
        self.now += seconds

    def __call__(self) -> float:
        return self.now


def base_config(clock, **overrides) -> CircuitBreakerConfig:
    """window 10, min 5 calls, 50% thresholds, slow >= 0.5s, open 10s, 2 half-open trials."""
    settings = dict(
        sliding_window_size=10,
        minimum_number_of_calls=5,
        failure_rate_threshold=50,
        slow_call_rate_threshold=50,
        slow_call_duration_threshold=0.5,
        wait_duration_in_open_state=10,
        permitted_calls_in_half_open_state=2,
        clock=clock,
    )
    settings.update(overrides)
    return CircuitBreakerConfig(**settings)


def ok() -> str:
    return "ok"


def boom() -> None:
    raise RuntimeError("downstream error")


def succeed(cb: CircuitBreaker) -> None:
    cb.call(ok)


def fail(cb: CircuitBreaker) -> None:
    try:
        cb.call(boom)
    except RuntimeError:
        pass


class CircuitBreakerTest(unittest.TestCase):
    def setUp(self) -> None:
        self.clock = FakeClock()
        self.cb = CircuitBreaker("t", base_config(self.clock))

    def trip_open(self) -> None:
        for _ in range(5):
            fail(self.cb)
        self.assertIs(self.cb.state, State.OPEN)

    def slow_call(self, seconds: float):
        def fn():
            self.clock.advance(seconds)
            return "slow but ok"

        return self.cb.call(fn)

    # --------------------------------------------------------------- CLOSED

    def test_starts_closed_and_passes_calls_through(self):
        self.assertIs(self.cb.state, State.CLOSED)
        self.assertEqual(self.cb.call(lambda a, b=0: a + b, 2, b=3), 5)

    def test_does_not_trip_before_minimum_number_of_calls(self):
        for _ in range(4):
            fail(self.cb)
        self.assertIs(self.cb.state, State.CLOSED)
        self.assertEqual(self.cb.metrics.failure_rate, -1.0)

    def test_trips_open_when_failure_rate_reaches_threshold(self):
        events = []
        self.cb.on_state_transition(events.append)
        for _ in range(3):
            succeed(self.cb)
        fail(self.cb)
        fail(self.cb)  # 2/5 = 40%
        self.assertIs(self.cb.state, State.CLOSED)
        fail(self.cb)  # 3/6 = 50% -> trip
        self.assertIs(self.cb.state, State.OPEN)
        self.assertEqual([(e.from_state, e.to_state) for e in events], [(State.CLOSED, State.OPEN)])

    def test_trips_open_when_slow_call_rate_reaches_threshold(self):
        for _ in range(5):
            self.slow_call(1.0)  # succeeds, but 1s >= 0.5s threshold
        self.assertIs(self.cb.state, State.OPEN)
        self.assertEqual(self.cb.metrics.failed_calls, 0)

    def test_sliding_window_evicts_old_outcomes(self):
        cb = CircuitBreaker("t", base_config(self.clock, sliding_window_size=5))
        fail(cb)
        fail(cb)
        for _ in range(5):
            succeed(cb)  # pushes both failures out
        m = cb.metrics
        self.assertEqual((m.buffered_calls, m.failed_calls, m.failure_rate), (5, 0, 0.0))

    # --------------------------------------------------------------- OPEN

    def test_open_rejects_calls_without_invoking_downstream(self):
        self.trip_open()
        invocations = []
        with self.assertRaises(CallNotPermittedError) as ctx:
            self.cb.call(lambda: invocations.append(1))
        self.assertEqual(invocations, [])
        self.assertIs(ctx.exception.state, State.OPEN)

    def test_open_to_half_open_after_wait_duration(self):
        self.trip_open()
        self.clock.advance(9)
        self.assertIs(self.cb.state, State.OPEN)
        self.clock.advance(1)
        self.assertIs(self.cb.state, State.HALF_OPEN)

    # --------------------------------------------------------------- HALF_OPEN

    def test_half_open_to_closed_after_all_trials_succeed(self):
        self.trip_open()
        self.clock.advance(10)
        succeed(self.cb)
        self.assertIs(self.cb.state, State.HALF_OPEN)
        succeed(self.cb)
        self.assertIs(self.cb.state, State.CLOSED)
        self.assertEqual(self.cb.metrics.buffered_calls, 0)  # window reset on close

    def test_half_open_to_open_on_failed_trial(self):
        self.trip_open()
        self.clock.advance(10)
        succeed(self.cb)
        fail(self.cb)
        self.assertIs(self.cb.state, State.OPEN)
        self.clock.advance(5)  # the OPEN timer restarted
        self.assertIs(self.cb.state, State.OPEN)

    def test_half_open_to_open_on_slow_trial(self):
        self.trip_open()
        self.clock.advance(10)
        self.slow_call(0.6)
        self.assertIs(self.cb.state, State.OPEN)

    def test_half_open_limits_number_of_trial_calls(self):
        self.trip_open()
        self.clock.advance(10)
        in_flight = threading.Barrier(3)
        release = threading.Event()

        def trial():
            in_flight.wait(timeout=2)
            release.wait(timeout=2)
            return "ok"

        with ThreadPoolExecutor(max_workers=2) as pool:
            futures = [pool.submit(self.cb.call, trial) for _ in range(2)]
            in_flight.wait(timeout=2)  # both trials hold their permits now
            with self.assertRaises(CallNotPermittedError) as ctx:
                self.cb.call(ok)
            self.assertIs(ctx.exception.state, State.HALF_OPEN)
            release.set()
            for f in futures:
                self.assertEqual(f.result(timeout=2), "ok")
        self.assertIs(self.cb.state, State.CLOSED)

    def test_stale_result_from_before_transition_is_ignored(self):
        # A call starts while CLOSED, the breaker opens and goes HALF_OPEN meanwhile,
        # then the old call succeeds. It must NOT count as a successful trial.
        started, finish = threading.Event(), threading.Event()

        def long_call():
            started.set()
            finish.wait(timeout=2)
            return "late"

        with ThreadPoolExecutor(max_workers=1) as pool:
            future = pool.submit(self.cb.call, long_call)
            started.wait(timeout=2)
            self.trip_open()
            self.clock.advance(10)
            self.assertIs(self.cb.state, State.HALF_OPEN)
            finish.set()
            self.assertEqual(future.result(timeout=2), "late")
        succeed(self.cb)  # one genuine trial
        self.assertIs(self.cb.state, State.HALF_OPEN)  # still needs a second one

    # --------------------------------------------------------------- features

    def test_call_timeout_releases_caller_and_counts_as_failure(self):
        cb = CircuitBreaker(
            "t",
            CircuitBreakerConfig(
                sliding_window_size=2,
                minimum_number_of_calls=2,
                failure_rate_threshold=100,
                call_timeout=0.05,
            ),
        )
        for _ in range(2):
            start = time.perf_counter()
            with self.assertRaises(CallTimeoutError):
                cb.call(time.sleep, 2)
            self.assertLess(time.perf_counter() - start, 1.0)
        self.assertIs(cb.state, State.OPEN)
        self.assertEqual(cb.metrics.failed_calls, 2)

    def test_timeout_preserves_exceptions_from_the_call(self):
        cb = CircuitBreaker("t", CircuitBreakerConfig(call_timeout=1.0))
        with self.assertRaises(KeyError):
            cb.call(lambda: {}["missing"])

    def test_fallback_on_failure_and_on_rejection(self):
        self.assertEqual(
            self.cb.call_with_fallback(boom, lambda exc: f"fallback:{exc}"),
            "fallback:downstream error",
        )
        self.cb.force_open()
        self.assertEqual(self.cb.call_with_fallback(ok, lambda exc: "cached"), "cached")

    def test_decorator(self):
        @self.cb
        def add(a, b):
            """adds"""
            return a + b

        self.assertEqual(add(1, 2), 3)
        self.assertEqual(add.__name__, "add")
        self.assertEqual(self.cb.metrics.buffered_calls, 1)

    def test_force_open_and_reset(self):
        self.cb.force_open()
        self.assertIs(self.cb.state, State.OPEN)
        self.cb.reset()
        self.assertIs(self.cb.state, State.CLOSED)
        self.assertEqual(self.cb.call(ok), "ok")

    def test_bad_listener_does_not_break_breaker_and_listener_may_reenter(self):
        seen = []
        self.cb.on_state_transition(lambda e: 1 / 0)
        self.cb.on_state_transition(lambda e: seen.append(self.cb.metrics))  # re-entrant lock
        self.trip_open()
        self.assertEqual(len(seen), 1)

    def test_thread_safe_under_concurrent_load(self):
        cb = CircuitBreaker(
            "t",
            CircuitBreakerConfig(
                sliding_window_size=100, minimum_number_of_calls=100, failure_rate_threshold=100
            ),
        )
        with ThreadPoolExecutor(max_workers=8) as pool:
            results = list(pool.map(lambda _: cb.call(ok), range(5_000)))
        self.assertEqual(results.count("ok"), 5_000)
        self.assertEqual(cb.metrics.buffered_calls, 100)
        self.assertIs(cb.state, State.CLOSED)

    def test_config_validation(self):
        for bad in (
            dict(failure_rate_threshold=0),
            dict(failure_rate_threshold=101),
            dict(sliding_window_size=5, minimum_number_of_calls=6),
            dict(wait_duration_in_open_state=0),
            dict(call_timeout=-1),
            dict(permitted_calls_in_half_open_state=0),
        ):
            with self.subTest(**bad), self.assertRaises(ValueError):
                CircuitBreakerConfig(**bad)


if __name__ == "__main__":
    unittest.main()
