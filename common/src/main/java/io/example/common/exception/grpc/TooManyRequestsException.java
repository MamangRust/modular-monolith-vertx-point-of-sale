package io.example.common.exception.grpc;

public class TooManyRequestsException extends GrpcException {
    public TooManyRequestsException(String message) {
        super("TOO_MANY_REQUESTS", message);
    }

    @Override
    public io.grpc.Status.Code getGrpcStatusCode() {
        return io.grpc.Status.Code.RESOURCE_EXHAUSTED;
    }
}
