package io.example.apigateway.resilience;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.grpc.MethodDescriptor;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.client.GrpcClientRequest;
import io.vertx.grpc.common.ServiceMethod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decorates the shared {@link GrpcClient} so every downstream call:
 *
 * <ol>
 *   <li><b>Trace propagation (gap #24)</b> — injects the W3C {@code traceparent}
 *       header into the gRPC request metadata. The header is captured
 *       synchronously while the gateway span is still current (the generated
 *       stub calls {@code request(...)} during the request's synchronous phase)
 *       and attached in the async callback, so it always carries the correct
 *       gateway span. Services extract it via
 *       {@code common ... TraceContextExtractor}.</li>
 *   <li><b>Circuit breaker (gap #25)</b> — a per-upstream breaker keyed by
 *       socket address fails fast (HTTP 503) once a downstream service becomes
 *       unreachable, instead of letting every request time out.</li>
 * </ol>
 */
public class ResilientGrpcClient implements GrpcClient {

  private static final String TRACEPARENT_HEADER = "traceparent";

  private final GrpcClient delegate;
  private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

  public ResilientGrpcClient(GrpcClient delegate) {
    this.delegate = delegate;
  }

  @Override
  public Future<GrpcClientRequest<Buffer, Buffer>> request(SocketAddress socketAddress) {
    return guarded(socketAddress, delegate.request(socketAddress));
  }

  @Override
  public <Req, Resp> Future<GrpcClientRequest<Req, Resp>> request(
      SocketAddress socketAddress, MethodDescriptor<Req, Resp> methodDescriptor) {
    return guarded(socketAddress, delegate.request(socketAddress, methodDescriptor));
  }

  @Override
  public <Req, Resp> Future<GrpcClientRequest<Req, Resp>> request(
      SocketAddress socketAddress, ServiceMethod<Resp, Req> serviceMethod) {
    return guarded(socketAddress, delegate.request(socketAddress, serviceMethod));
  }

  @Override
  public Future<Void> close() {
    return delegate.close();
  }

  private <Req, Resp> Future<GrpcClientRequest<Req, Resp>> guarded(
      SocketAddress address, Future<GrpcClientRequest<Req, Resp>> upstream) {
    CircuitBreaker breaker = breakers.computeIfAbsent(address.toString(), CircuitBreaker::new);
    if (!breaker.isCallAllowed()) {
      return Future.failedFuture(breaker.unavailable());
    }
    // Captured synchronously — the gateway span is current while the generated
    // stub initiates the call (see GatewayMetricsMiddleware).
    String traceparent = GrpcGatewayUtils.currentTraceparent();
    return upstream
        .map(req -> {
          if (traceparent != null) {
            req.headers().add(TRACEPARENT_HEADER, traceparent);
          }
          return req;
        })
        .onComplete(ar -> {
          if (ar.succeeded()) {
            breaker.recordSuccess();
          } else {
            breaker.recordFailure();
          }
        });
  }
}
