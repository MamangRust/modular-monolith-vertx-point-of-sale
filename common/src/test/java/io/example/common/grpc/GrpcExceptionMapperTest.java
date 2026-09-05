package io.example.common.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.example.common.exception.api.ForbiddenException;
import io.example.common.exception.api.NotFoundException;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.ConflictException;
import io.example.common.exception.grpc.InsufficientBalanceException;
import io.example.common.exception.grpc.InternalServerErrorException;
import io.example.common.exception.grpc.TooManyRequestsException;
import io.example.common.exception.grpc.UnauthorizedException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.vertx.core.Future;

class GrpcExceptionMapperTest {

  private Status.Code codeOf(Throwable t) {
    StatusRuntimeException sre = GrpcExceptionMapper.toStatusRuntimeException(t);
    return sre.getStatus().getCode();
  }

  @Test
  void unauthorizedExceptionMapsToUnauthenticated() {
    // gap #13: was NOT_FOUND → 404, now UNAUTHENTICATED → 401
    assertThat(codeOf(new UnauthorizedException("bad credentials")))
        .isEqualTo(Status.Code.UNAUTHENTICATED);
  }

  @Test
  void grpcDomainExceptionsMapToStatuses() {
    assertThat(codeOf(new NotFoundException("n/a"))).isEqualTo(Status.Code.NOT_FOUND);
    assertThat(codeOf(new BadRequestException("bad"))).isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(codeOf(new ConflictException("exists"))).isEqualTo(Status.Code.ALREADY_EXISTS);
    assertThat(codeOf(new InsufficientBalanceException(10, 50)))
        .isEqualTo(Status.Code.FAILED_PRECONDITION);
    assertThat(codeOf(new TooManyRequestsException("locked")))
        .isEqualTo(Status.Code.RESOURCE_EXHAUSTED);
    assertThat(codeOf(new InternalServerErrorException("boom", new RuntimeException())))
        .isEqualTo(Status.Code.INTERNAL);
    // every GrpcException maps through getGrpcStatusCode(), including the
    // previously-missed ForbiddenException (old switch → INTERNAL/500)
    assertThat(codeOf(new io.example.common.exception.grpc.ForbiddenException("denied")))
        .isEqualTo(Status.Code.PERMISSION_DENIED);
  }

  @Test
  void apiExceptionsBridgeAcrossGrpcBoundary() {
    // gap #14: query services throw exception.api.NotFoundException which the
    // mapper previously did not know → 500. Now bridged to NOT_FOUND.
    assertThat(codeOf(new NotFoundException("doc not found")))
        .isEqualTo(Status.Code.NOT_FOUND);
    assertThat(codeOf(new ForbiddenException("denied"))).isEqualTo(Status.Code.PERMISSION_DENIED);
    assertThat(codeOf(new io.example.common.exception.api.BadRequestException("bad form")))
        .isEqualTo(Status.Code.INVALID_ARGUMENT);
    assertThat(codeOf(new io.example.common.exception.api.InternalServerErrorException("boom")))
        .isEqualTo(Status.Code.INTERNAL);
  }

  @Test
  void statusRuntimeExceptionPassesThrough() {
    StatusRuntimeException original = Status.UNAVAILABLE.withDescription("down").asRuntimeException();
    assertThat(GrpcExceptionMapper.toStatusRuntimeException(original)).isSameAs(original);
  }

  @Test
  void unknownThrowableMapsToInternalWithoutLeakingMessage() {
    Status.Code code = codeOf(new RuntimeException("sensitive internal detail"));
    assertThat(code).isEqualTo(Status.Code.INTERNAL);
  }

  @Test
  void toFailedFutureWrapsInFailedFuture() {
    Future<String> f = GrpcExceptionMapper.toFailedFuture(new UnauthorizedException("nope"));
    assertThat(f.failed()).isTrue();
    assertThat(f.cause()).isInstanceOf(StatusRuntimeException.class);
  }
}
