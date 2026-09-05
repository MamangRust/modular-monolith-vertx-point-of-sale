package io.example.common.grpc;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.vertx.core.MultiMap;

/**
 * Extracts the W3C {@code traceparent} metadata header sent by the API gateway
 * on every gRPC call so service-side spans become children of the gateway span
 * (one end-to-end trace in Jaeger — closes gap #24).
 *
 * <p>Services read the header in {@link GrpcServerBinder} before invoking the
 * handler; {@link #extract} returns a {@link Context} that, once made current,
 * parents any span started afterwards (e.g. {@code TracingMetrics.startSpan}).
 */
public final class TraceContextExtractor {

  public static final String TRACEPARENT_HEADER = "traceparent";

  private TraceContextExtractor() {
  }

  /**
   * Returns a {@link Context} carrying the remote span encoded in the request's
   * {@code traceparent} header, or {@link Context#root()} when absent/invalid.
   */
  public static Context extract(MultiMap headers) {
    String traceparent = headers == null ? null : headers.get(TRACEPARENT_HEADER);
    if (traceparent == null || traceparent.isBlank()) {
      return Context.root();
    }
    try {
      SpanContext spanContext = parseTraceparent(traceparent.trim());
      if (spanContext.isValid()) {
        return Context.root().with(Span.wrap(spanContext));
      }
    } catch (RuntimeException e) {
      // Malformed header — fall through to root context.
    }
    return Context.root();
  }

  /**
   * Parses {@code version-traceid-spanid-flags} (W3C trace context). Exposed as
   * package-private for unit tests.
   */
  static SpanContext parseTraceparent(String traceparent) {
    String[] parts = traceparent.split("-");
    if (parts.length != 4 || !"00".equals(parts[0])) {
      return SpanContext.getInvalid();
    }
    String traceId = parts[1];
    String spanId = parts[2];
    boolean sampled = "01".equals(parts[3]);
    if (traceId.length() != 32 || spanId.length() != 16
        || !traceId.matches("[0-9a-f]{32}") || !spanId.matches("[0-9a-f]{16}")) {
      return SpanContext.getInvalid();
    }
    return SpanContext.create(traceId, spanId,
        sampled ? TraceFlags.getSampled() : TraceFlags.getDefault(),
        TraceState.getDefault());
  }
}
