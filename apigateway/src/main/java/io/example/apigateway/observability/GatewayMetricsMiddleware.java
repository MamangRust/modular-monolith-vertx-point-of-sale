package io.example.apigateway.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongUpDownCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Edge observability middleware for the REST gateway.
 *
 * <p>Records per-request OpenTelemetry metrics (request count bucketed by HTTP
 * status class, duration histogram, in-flight gauge) and starts a SERVER span
 * for every request. Because the span is made current, {@code
 * GrpcGatewayUtils.currentTraceId()} (used in error envelopes) returns the
 * real trace id, and any manual span created downstream on the same context
 * becomes part of the same trace.
 */
public class GatewayMetricsMiddleware implements Handler<RoutingContext> {

  private static final AttributeKey<String> METHOD_KEY = AttributeKey.stringKey("http.method");
  private static final AttributeKey<String> ROUTE_KEY = AttributeKey.stringKey("http.route");
  private static final AttributeKey<String> STATUS_CLASS_KEY =
      AttributeKey.stringKey("http.status_class");

  private final LongCounter requestsCounter;
  private final DoubleHistogram durationHistogram;
  private final LongUpDownCounter inFlightCounter;
  private final Tracer tracer;

  public GatewayMetricsMiddleware(OpenTelemetry openTelemetry) {
    Meter meter = openTelemetry.getMeter("api-gateway");
    this.requestsCounter = meter.counterBuilder("http.requests_total")
        .setDescription("Total HTTP requests handled by the gateway")
        .build();
    this.durationHistogram = meter.histogramBuilder("http.request_duration_seconds")
        .setDescription("HTTP request duration in seconds")
        .setUnit("s")
        .build();
    this.inFlightCounter = meter.upDownCounterBuilder("http.in_flight_requests")
        .setDescription("HTTP requests currently being processed by the gateway")
        .build();
    this.tracer = openTelemetry.getTracer("api-gateway");
  }

  public static final String TRACE_ID_KEY = "gw_trace_id";

  @Override
  public void handle(RoutingContext ctx) {
    long start = System.nanoTime();
    inFlightCounter.add(1);

    String method = ctx.request().method().name();
    String route = ctx.normalisedPath();

    Span span = tracer.spanBuilder("HTTP " + method + " " + route)
        .setSpanKind(SpanKind.SERVER)
        .setAttribute(METHOD_KEY, method)
        .setAttribute(ROUTE_KEY, route)
        .startSpan();

    // Publish the trace id on the routing context so async error paths
    // (e.g. a 504 from withDeadline) can include it in the envelope even
    // after the span is no longer "current".
    var spanContext = span.getSpanContext();
    if (spanContext.isValid()) {
      ctx.put(TRACE_ID_KEY, spanContext.getTraceId());
    }

    // Guard against double-finish: endHandler fires on normal completion,
    // closeHandler on client disconnect — never both.
    AtomicBoolean finished = new AtomicBoolean(false);
    Context context = Context.current().with(span);
    try (Scope scope = context.makeCurrent()) {
      HttpServerResponse response = ctx.response();
      response.endHandler(v -> finishOnce(ctx, span, start, method, route, finished));
      response.closeHandler(v -> finishOnce(ctx, span, start, method, route, finished));
      ctx.next();
    } catch (Exception e) {
      // Prevent a later end/close handler from double-decrementing the
      // in-flight gauge or double-ending the span (Vert.x failure handling
      // may still write & end the response after we rethrow).
      finished.set(true);
      span.setStatus(StatusCode.ERROR, e.getMessage());
      span.end();
      inFlightCounter.add(-1);
      throw e;
    }
  }

  private void finishOnce(RoutingContext ctx, Span span, long start, String method, String route,
                          AtomicBoolean finished) {
    if (!finished.compareAndSet(false, true)) {
      return;
    }
    try {
      long elapsed = System.nanoTime() - start;
      double seconds = elapsed / 1_000_000_000.0;
      int status = ctx.response().getStatusCode();
      String statusClass = status >= 500 ? "5xx" : status >= 400 ? "4xx"
          : status >= 300 ? "3xx" : "2xx";

      Attributes attributes = Attributes.builder()
          .put(METHOD_KEY, method)
          .put(ROUTE_KEY, route)
          .put(STATUS_CLASS_KEY, statusClass)
          .build();

      requestsCounter.add(1, attributes);
      durationHistogram.record(seconds, attributes);

      if (status >= 400) {
        span.setStatus(StatusCode.ERROR, "HTTP " + status);
      } else {
        span.setStatus(StatusCode.OK);
      }
    } finally {
      span.end();
      inFlightCounter.add(-1);
    }
  }
}
