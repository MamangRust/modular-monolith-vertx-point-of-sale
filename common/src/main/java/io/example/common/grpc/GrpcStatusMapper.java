package io.example.common.grpc;

import io.grpc.StatusRuntimeException;
import io.vertx.grpc.common.GrpcStatus;

/**
 * Maps a {@link Throwable} to the Vert.x {@link GrpcStatus} that should be put
 * on the wire. Reuses {@link GrpcExceptionMapper} so the domain exception
 * hierarchy remains the single source of truth for status codes.
 */
public final class GrpcStatusMapper {

  private GrpcStatusMapper() {
  }

  public static GrpcStatus of(Throwable throwable) {
    StatusRuntimeException sre = GrpcExceptionMapper.toStatusRuntimeException(throwable);
    return GrpcStatus.valueOf(sre.getStatus().getCode().value());
  }
}
