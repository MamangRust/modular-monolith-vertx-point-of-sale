package io.example.apigateway.resilience;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Minimal thread-safe circuit breaker (CLOSED → OPEN → HALF_OPEN → CLOSED)
 * guarding gateway calls to a single upstream service.
 *
 * <p>When enough consecutive connection-level failures accumulate the breaker
 * opens: calls fail fast with {@link Status#UNAVAILABLE} (HTTP 503) instead of
 * piling up against an unreachable service. After the cooldown a single
 * half-open probe is allowed to test recovery.
 *
 * <p>Scope note: the breaker tracks the {@code GrpcClient.request()} future, so
 * only <em>connection-level</em> failures (unreachable service) trip it.
 * RPC status errors and {@code withDeadline} timeouts are handled by the
 * existing error mapping / deadline layer and do NOT open the circuit.
 */
public class CircuitBreaker {

  public enum State {
    CLOSED, OPEN, HALF_OPEN
  }

  private final String name;
  private final int failureThreshold;
  private final long openTimeoutMs;

  private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
  private final AtomicInteger consecutiveFailures = new AtomicInteger();
  private final AtomicInteger halfOpenProbes = new AtomicInteger();
  private final AtomicLong openedAt = new AtomicLong(0);

  public CircuitBreaker(String name) {
    this(name, 5, 10_000);
  }

  public CircuitBreaker(String name, int failureThreshold, long openTimeoutMs) {
    this.name = name;
    this.failureThreshold = Math.max(1, failureThreshold);
    this.openTimeoutMs = Math.max(1, openTimeoutMs);
  }

  public String getName() {
    return name;
  }

  public State getState() {
    return state.get();
  }

  /**
   * Whether a call may proceed. Transitions OPEN → HALF_OPEN after the cooldown
   * and admits a single probe at a time.
   */
  public boolean isCallAllowed() {
    State current = state.get();
    if (current == State.CLOSED) {
      return true;
    }
    if (current == State.OPEN && System.currentTimeMillis() - openedAt.get() >= openTimeoutMs) {
      if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
        halfOpenProbes.set(0);
      }
    }
    return state.get() == State.HALF_OPEN && halfOpenProbes.incrementAndGet() <= 1;
  }

  public void recordSuccess() {
    consecutiveFailures.set(0);
    halfOpenProbes.set(0);
    state.compareAndSet(State.HALF_OPEN, State.CLOSED);
  }

  public void recordFailure() {
    if (state.get() == State.OPEN) {
      return;
    }
    if (state.get() == State.HALF_OPEN) {
      state.set(State.OPEN);
      openedAt.set(System.currentTimeMillis());
      return;
    }
    if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
      state.set(State.OPEN);
      openedAt.set(System.currentTimeMillis());
    }
  }

  /** Failure used when the breaker is open — mapped to HTTP 503 by the gateway. */
  public StatusRuntimeException unavailable() {
    return Status.UNAVAILABLE
        .withDescription("Upstream '" + name + "' is unavailable (circuit open)")
        .asRuntimeException();
  }
}
