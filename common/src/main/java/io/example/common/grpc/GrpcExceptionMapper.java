package io.example.common.grpc;

import io.example.common.exception.api.ApiException;
import io.example.common.exception.grpc.GrpcException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.vertx.core.Future;

public final class GrpcExceptionMapper {

    private GrpcExceptionMapper() {
    }

    /**
     * Maps any Throwable to a failed Future with proper gRPC Status.
     * This is the single place where domain exceptions → gRPC status mapping
     * happens.
     */
    public static <T> Future<T> toFailedFuture(Throwable throwable) {
        return Future.failedFuture(toStatusRuntimeException(throwable));
    }

    public static StatusRuntimeException toStatusRuntimeException(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException sre) {
            return sre;
        }

        if (throwable instanceof GrpcException de) {
            return mapDomainException(de);
        }

        if (throwable instanceof ApiException ae) {
            return mapApiException(ae);
        }

        return Status.INTERNAL
                .withDescription("An unexpected error occurred")
                .withCause(throwable)
                .asRuntimeException();
    }

    private static StatusRuntimeException mapDomainException(GrpcException ex) {
        // Every GrpcException declares its own gRPC status code, so the mapper
        // can never drift from the exception hierarchy (e.g. the old switch
        // silently mapped grpc.ForbiddenException to INTERNAL).
        Status.Code code = ex.getGrpcStatusCode() != null ? ex.getGrpcStatusCode() : Status.Code.INTERNAL;
        return Status.fromCode(code)
                .withDescription(ex.getMessage())
                .asRuntimeException();
    }

    /**
     * Maps legacy {@link ApiException} (HTTP-style status codes, used by
     * query services) to gRPC Status so it survives the gRPC boundary.
     */
    private static StatusRuntimeException mapApiException(ApiException ex) {
        Status status = switch (ex.getStatusCode()) {
            case 400 -> Status.INVALID_ARGUMENT;
            case 401 -> Status.UNAUTHENTICATED;
            case 403 -> Status.PERMISSION_DENIED;
            case 404 -> Status.NOT_FOUND;
            case 409 -> Status.ALREADY_EXISTS;
            case 429 -> Status.RESOURCE_EXHAUSTED;
            default -> Status.INTERNAL;
        };
        return status.withDescription(ex.getMessage()).asRuntimeException();
    }
}