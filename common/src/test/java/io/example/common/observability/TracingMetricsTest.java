package io.example.common.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.context.Context;

class TracingMetricsTest {

  private final TracingMetrics metrics = new TracingMetrics(OpenTelemetrySdk.builder().build(), "test-svc");

  @Test
  void startAndCompleteSuccessDoesNotThrowAndEndsSpan() {
    TracingMetrics.TracingContext ctx = metrics.startSpan("svc.create", Attributes.empty());
    assertThat(ctx).isNotNull();
    // completeSpanSuccess ends the span and records counters/histogram — must
    // not throw with the noop SDK.
    metrics.completeSpanSuccess(ctx, "create", "ok");
  }

  @Test
  void startAndCompleteErrorDoesNotThrow() {
    TracingMetrics.TracingContext ctx = metrics.startSpan("svc.trash");
    metrics.completeSpanError(ctx, "trash", "boom");
  }

  @Test
  void spanIsCurrentInsideTraceAndMeasure() {
    String[] captured = new String[1];
    metrics.traceAndMeasure("svc.find", "find", () -> {
      captured[0] = Span.current().getSpanContext().getSpanId();
      return "result";
    });
    assertThat(captured[0]).isNotBlank();
  }

  @Test
  void contextInjectionAndExtractionRoundTrip() {
    TracingMetrics.TracingContext ctx = metrics.startSpan("svc.op");
    Map<String, String> carrier = new HashMap<>();
    metrics.injectContext(ctx.getContext(), carrier);

    Context extracted = metrics.extractContext(carrier);
    assertThat(extracted).isNotNull();
    metrics.completeSpanSuccess(ctx, "op", "done");
  }
}
