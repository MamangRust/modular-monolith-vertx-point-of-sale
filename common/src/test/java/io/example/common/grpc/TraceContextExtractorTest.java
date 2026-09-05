package io.example.common.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.vertx.core.MultiMap;
import io.vertx.core.http.impl.headers.HeadersMultiMap;

import org.junit.jupiter.api.Test;

class TraceContextExtractorTest {

  private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
  private static final String SPAN_ID = "00f067aa0ba902b7";

  @Test
  void parsesValidSampledTraceparent() {
    SpanContext sc = TraceContextExtractor.parseTraceparent(
        "00-" + TRACE_ID + "-" + SPAN_ID + "-01");
    assertThat(sc.isValid()).isTrue();
    assertThat(sc.getTraceId()).isEqualTo(TRACE_ID);
    assertThat(sc.getSpanId()).isEqualTo(SPAN_ID);
    assertThat(sc.isSampled()).isTrue();
  }

  @Test
  void parsesUnsampledTraceparent() {
    SpanContext sc = TraceContextExtractor.parseTraceparent(
        "00-" + TRACE_ID + "-" + SPAN_ID + "-00");
    assertThat(sc.isValid()).isTrue();
    assertThat(sc.isSampled()).isFalse();
  }

  @Test
  void rejectsMalformedTraceparent() {
    assertThat(TraceContextExtractor.parseTraceparent("01-abc-xyz-01").isValid()).isFalse();
    assertThat(TraceContextExtractor.parseTraceparent(
        "00-1234-5678-01").isValid()).isFalse(); // wrong lengths
    assertThat(TraceContextExtractor.parseTraceparent(
        "00-gggggggggggggggggggggggggggggggg-0000000000000000-01").isValid()).isFalse();
    assertThat(TraceContextExtractor.parseTraceparent("garbage").isValid()).isFalse();
  }

  @Test
  void extractReturnsRootWhenHeaderMissing() {
    MultiMap headers = new HeadersMultiMap();
    headers.add("other", "value");
    Context extracted = TraceContextExtractor.extract(headers);
    assertThat(extracted).isSameAs(Context.root());
  }

  @Test
  void extractCarriesRemoteSpanWhenHeaderPresent() {
    MultiMap headers = new HeadersMultiMap();
    headers.add(TraceContextExtractor.TRACEPARENT_HEADER,
        "00-" + TRACE_ID + "-" + SPAN_ID + "-01");
    Context extracted = TraceContextExtractor.extract(headers);
    assertThat(extracted).isNotSameAs(Context.root());
    assertThat(io.opentelemetry.api.trace.Span.fromContext(extracted).getSpanContext().getTraceId())
        .isEqualTo(TRACE_ID);
  }

  @Test
  void extractIgnoresMalformedHeader() {
    MultiMap headers = new HeadersMultiMap();
    headers.add(TraceContextExtractor.TRACEPARENT_HEADER, "not-a-traceparent");
    assertThat(TraceContextExtractor.extract(headers)).isSameAs(Context.root());
  }
}
