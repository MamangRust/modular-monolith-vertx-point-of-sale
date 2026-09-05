package io.example.apigateway.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

  @Test
  void rejectsNonPositiveConfiguration() {
    assertThat(org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> new TokenBucket(0, 1))).isNotNull();
    assertThat(org.junit.jupiter.api.Assertions.assertThrows(
        IllegalArgumentException.class, () -> new TokenBucket(1, 0))).isNotNull();
  }

  @Test
  void allowsUpToCapacityTokensBurst() {
    TokenBucket bucket = new TokenBucket(3, 100);
    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isTrue();
    // Burst exhausted before any refill window elapses.
    assertThat(bucket.tryConsume()).isFalse();
  }

  @Test
  void refillsOverTime() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(1, 100); // 100 tokens/sec → 1 token per 10ms
    assertThat(bucket.tryConsume()).isTrue();
    assertThat(bucket.tryConsume()).isFalse();

    Thread.sleep(30);
    assertThat(bucket.tryConsume()).isTrue();
  }

  @Test
  void neverExceedsCapacityAfterLongPause() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(5, 1000);
    Thread.sleep(200);
    assertThat(bucket.availableTokens()).isLessThanOrEqualTo(5);
    for (int i = 0; i < 5; i++) {
      assertThat(bucket.tryConsume()).as("token %d", i).isTrue();
    }
    assertThat(bucket.tryConsume()).isFalse();
  }
}
