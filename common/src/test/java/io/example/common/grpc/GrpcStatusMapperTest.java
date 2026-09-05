package io.example.common.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.example.common.exception.api.ForbiddenException;
import io.example.common.exception.api.NotFoundException;
import io.example.common.exception.grpc.UnauthorizedException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.vertx.grpc.common.GrpcStatus;

class GrpcStatusMapperTest {

  // ── GrpcStatusMapper ───────────────────────────────────────────────────

  @Test
  void unauthorizedMapsToUnauthenticated() {
    assertThat(GrpcStatusMapper.of(new UnauthorizedException("bad credentials")))
        .isEqualTo(GrpcStatus.UNAUTHENTICATED);
  }

  @Test
  void grpcDomainExceptionsMapToStatuses() {
    assertThat(GrpcStatusMapper.of(new io.example.common.exception.grpc.NotFoundException("n/a")))
        .isEqualTo(GrpcStatus.NOT_FOUND);
    assertThat(GrpcStatusMapper.of(new io.example.common.exception.grpc.ConflictException("dup")))
        .isEqualTo(GrpcStatus.ALREADY_EXISTS);
    assertThat(GrpcStatusMapper.of(new io.example.common.exception.grpc.BadRequestException("bad")))
        .isEqualTo(GrpcStatus.INVALID_ARGUMENT);
    assertThat(GrpcStatusMapper.of(new io.example.common.exception.grpc.TooManyRequestsException("x")))
        .isEqualTo(GrpcStatus.RESOURCE_EXHAUSTED);
  }

  @Test
  void apiExceptionsBridgeToGrpcStatus() {
    assertThat(GrpcStatusMapper.of(new NotFoundException("doc")))
        .isEqualTo(GrpcStatus.NOT_FOUND);
    assertThat(GrpcStatusMapper.of(new ForbiddenException("denied")))
        .isEqualTo(GrpcStatus.PERMISSION_DENIED);
  }

  @Test
  void statusRuntimeExceptionPreservesCode() {
    StatusRuntimeException original = Status.UNAVAILABLE.withDescription("down").asRuntimeException();
    assertThat(GrpcStatusMapper.of(original)).isEqualTo(GrpcStatus.UNAVAILABLE);
  }

  @Test
  void unknownThrowableMapsToInternal() {
    assertThat(GrpcStatusMapper.of(new RuntimeException("boom"))).isEqualTo(GrpcStatus.INTERNAL);
  }

  // ── GrpcServerBinder ───────────────────────────────────────────────────

  private pb.VertxAuthServiceGrpcServer.AuthServiceApi authProxy() {
    return (pb.VertxAuthServiceGrpcServer.AuthServiceApi) Proxy.newProxyInstance(
        getClass().getClassLoader(),
        new Class<?>[]{pb.VertxAuthServiceGrpcServer.AuthServiceApi.class},
        (proxy, method, args) -> null);
  }

  @Test
  void binderDiscoversEveryRpcMethodOfGeneratedInterface() {
    List<GrpcServerBinder.BoundMethod> methods =
        GrpcServerBinder.discoverMethods(authProxy());
    assertThat(methods).extracting(m -> m.method.getName())
        .containsExactlyInAnyOrder("verifyCode", "forgotPassword", "resetPassword",
            "registerUser", "loginUser", "refreshToken", "getMe");
  }

  @Test
  void everyRpcMethodHasMatchingServiceMethodConstant() throws Exception {
    for (GrpcServerBinder.BoundMethod bound : GrpcServerBinder.discoverMethods(authProxy())) {
      String fieldName = Character.toUpperCase(bound.method.getName().charAt(0))
          + bound.method.getName().substring(1);
      assertThat(bound.generated.getField(fieldName)).as(fieldName).isNotNull();
    }
  }

  @Test
  void binderRejectsServiceWithoutGeneratedInterface() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> GrpcServerBinder.bindAll(null, new Object()));
  }
}
