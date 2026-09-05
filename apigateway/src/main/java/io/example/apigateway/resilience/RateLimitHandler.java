package io.example.apigateway.resilience;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-client-IP token-bucket rate limiter for the gateway (gap #25). Responds
 * with the standard error envelope + HTTP 429 when a client exceeds the
 * configured rate. Tuning via env: {@code RATE_LIMIT_PER_SECOND} (default 100),
 * {@code RATE_LIMIT_BURST} (default 200).
 */
public class RateLimitHandler implements Handler<RoutingContext> {

  /**
   * Upper bound on distinct client buckets to prevent unbounded memory growth
   * under spoofed/rotating client IPs. Beyond this, new clients share a single
   * fallback bucket (limits still enforced, just coarser-grained).
   */
  static final int MAX_BUCKETS = 10_000;

  private final double perSecond;
  private final long burst;
  private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
  private final TokenBucket fallback;

  public RateLimitHandler() {
    this(envInt("RATE_LIMIT_PER_SECOND", 100), envInt("RATE_LIMIT_BURST", 200));
  }

  public RateLimitHandler(double perSecond, long burst) {
    if (perSecond <= 0 || burst <= 0) {
      throw new IllegalArgumentException("rate and burst must be positive");
    }
    this.perSecond = perSecond;
    this.burst = burst;
    this.fallback = new TokenBucket(burst, perSecond);
  }

  @Override
  public void handle(RoutingContext ctx) {
    String key = clientKey(ctx);
    TokenBucket bucket = buckets.get(key);
    if (bucket == null) {
      // Bound memory: once MAX_BUCKETS distinct clients are tracked, route new
      // clients through the shared fallback bucket instead of growing forever.
      bucket = buckets.size() >= MAX_BUCKETS
          ? fallback
          : buckets.computeIfAbsent(key, k -> new TokenBucket(burst, perSecond));
    }
    if (bucket.tryConsume()) {
      ctx.next();
    } else {
      GrpcGatewayUtils.sendError(ctx, 429, "Rate limit exceeded. Try again later.");
    }
  }

  /**
   * Bucket key per client. NOTE: when the gateway sits behind a trusted reverse
   * proxy (nginx in this repo), nginx must overwrite/append {@code X-Forwarded-For}
   * so clients cannot spoof it to rotate buckets and bypass the limit.
   */
  private static String clientKey(RoutingContext ctx) {
    String forwarded = ctx.request().getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return ctx.request().remoteAddress() != null
        ? ctx.request().remoteAddress().host() : "unknown";
  }

  private static int envInt(String key, int defaultValue) {
    String raw = System.getenv(key);
    try {
      return raw != null ? Integer.parseInt(raw) : defaultValue;
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
