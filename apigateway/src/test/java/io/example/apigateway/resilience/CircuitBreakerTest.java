package io.example.apigateway.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CircuitBreakerTest {

  @Test
  void allowsCallsWhileClosed() {
    CircuitBreaker cb = new CircuitBreaker("user", 3, 10_000);
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    assertThat(cb.isCallAllowed()).isTrue();
  }

  @Test
  void opensAfterFailureThreshold() {
    CircuitBreaker cb = new CircuitBreaker("role", 3, 10_000);
    cb.recordFailure();
    cb.recordFailure();
    assertThat(cb.isCallAllowed()).isTrue(); // threshold not reached yet
    cb.recordFailure();
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(cb.isCallAllowed()).isFalse();
  }

  @Test
  void recoversThroughHalfOpenAfterCooldown() throws InterruptedException {
    CircuitBreaker cb = new CircuitBreaker("order", 1, 50);
    cb.recordFailure();
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(cb.isCallAllowed()).isFalse();

    Thread.sleep(60);
    // First call after cooldown transitions to HALF_OPEN and is admitted (probe).
    assertThat(cb.isCallAllowed()).isTrue();
    // No second concurrent probe while half-open.
    assertThat(cb.isCallAllowed()).isFalse();

    // Successful probe closes the breaker again.
    cb.recordSuccess();
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    assertThat(cb.isCallAllowed()).isTrue();
  }

  @Test
  void failedProbeReopensBreaker() throws InterruptedException {
    CircuitBreaker cb = new CircuitBreaker("tx", 1, 50);
    cb.recordFailure();
    Thread.sleep(60);
    assertThat(cb.isCallAllowed()).isTrue(); // probe admitted
    cb.recordFailure();
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    assertThat(cb.isCallAllowed()).isFalse();
  }

  @Test
  void successesResetConsecutiveFailures() {
    CircuitBreaker cb = new CircuitBreaker("cashier", 5, 10_000);
    cb.recordFailure();
    cb.recordFailure();
    cb.recordSuccess();
    cb.recordFailure();
    cb.recordFailure();
    cb.recordFailure();
    cb.recordFailure();
    assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void unavailableFailureIsMappedToGrpcUnavailable() {
    CircuitBreaker cb = new CircuitBreaker("merchant");
    assertThat(cb.unavailable().getStatus().getCode().name()).isEqualTo("UNAVAILABLE");
  }
}
