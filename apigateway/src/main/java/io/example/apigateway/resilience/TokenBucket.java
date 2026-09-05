package io.example.apigateway.resilience;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple thread-safe token bucket used by {@link RateLimitHandler}.
 * Refills {@code refillPerSecond} tokens per second up to {@code capacity}.
 */
public class TokenBucket {

  private final long capacity;
  private final double refillPerSecond;
  private final AtomicLong tokens;
  private volatile long lastRefillNanos;

  public TokenBucket(long capacity, double refillPerSecond) {
    if (capacity <= 0 || refillPerSecond <= 0) {
      throw new IllegalArgumentException("capacity and refillPerSecond must be positive");
    }
    this.capacity = capacity;
    this.refillPerSecond = refillPerSecond;
    this.tokens = new AtomicLong(capacity);
    this.lastRefillNanos = System.nanoTime();
  }

  /** Tries to consume one token; {@code false} when the bucket is empty. */
  public synchronized boolean tryConsume() {
    long now = System.nanoTime();
    double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
    if (elapsedSeconds > 0) {
      long refilled = (long) Math.floor(refillPerSecond * elapsedSeconds);
      if (refilled > 0) {
        tokens.set(Math.min(capacity, tokens.get() + refilled));
        lastRefillNanos = now;
      }
    }
    if (tokens.get() > 0) {
      tokens.decrementAndGet();
      return true;
    }
    return false;
  }

  long availableTokens() {
    return tokens.get();
  }
}
