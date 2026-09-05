package io.example.apigateway.utils;

import com.google.protobuf.MessageOrBuilder;

import io.example.apigateway.observability.GatewayMetricsMiddleware;
import io.example.common.exception.api.ApiException;
import io.example.common.exception.api.BadRequestException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GrpcGatewayUtils {

    public static void sendResponse(RoutingContext ctx, MessageOrBuilder proto, int httpStatus) {
        JsonObject json = ProtoMapper.toJson(proto);
        ctx.response()
                .setStatusCode(httpStatus)
                .putHeader("Content-Type", "application/json")
                .end(json.encode());
    }

    public static void handleError(RoutingContext ctx, Throwable err) {
        Throwable root = rootCause(err);
        if (root instanceof StatusRuntimeException sre) {
            Status.Code code = sre.getStatus().getCode();
            String description = sre.getStatus().getDescription();
            sendError(ctx, httpStatusFor(code), description != null ? description : code.name());
        } else if (isUpstreamGrpcStatusError(root)) {
            // The Vert.x gRPC client (4.5.x) fails the future with a plain
            // message ("Invalid gRPC status <code>") instead of a
            // StatusRuntimeException, losing the structured status. Parse the
            // numeric code so error mapping stays consistent for every service.
            Status.Code code = upstreamGrpcStatusCode(root.getMessage());
            sendError(ctx, httpStatusFor(code), code.name());
        } else if (root instanceof ApiException ae) {
            sendError(ctx, ae.getStatusCode(), ae.getMessage());
        } else if (root instanceof IllegalArgumentException iae) {
            sendError(ctx, 400, iae.getMessage());
        } else if (root instanceof TimeoutException) {
            // Bounded gRPC deadline exceeded (see withDeadline) → 504.
            sendError(ctx, 504, "Upstream service timed out");
        } else {
            // Never leak the raw exception / stack trace to the client.
            ctx.fail(500, root);
        }
    }

    private static Throwable rootCause(Throwable err) {
        Throwable current = err;
        while (current.getCause() != null
                && (current instanceof java.util.concurrent.CompletionException
                    || current instanceof java.util.concurrent.ExecutionException
                    || current.getMessage() == null
                    || current.getMessage().isBlank())) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean isUpstreamGrpcStatusError(Throwable err) {
        String message = err.getMessage();
        return message != null && message.startsWith("Invalid gRPC status ");
    }

    /**
     * Parses the numeric code carried by the Vert.x gRPC client failure
     * message "Invalid gRPC status 13" (code 13 = INTERNAL). Falls back to
     * UNKNOWN when the suffix is not a number.
     */
    private static Status.Code upstreamGrpcStatusCode(String message) {
        try {
            int code = Integer.parseInt(message.substring("Invalid gRPC status ".length()).trim());
            for (Status.Code status : Status.Code.values()) {
                if (status.value() == code) {
                    return status;
                }
            }
            return Status.Code.UNKNOWN;
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            return Status.Code.UNKNOWN;
        }
    }

    /**
     * Maps a gRPC status code to the HTTP status returned by the gateway
     * (single source of truth for the error contract, see
     * ERROR_HANDLING_SUMMARY.md §4).
     */
    private static int httpStatusFor(Status.Code code) {
        return switch (code) {
            case NOT_FOUND -> 404;
            case INVALID_ARGUMENT, FAILED_PRECONDITION -> 400;
            case ALREADY_EXISTS -> 409;
            case UNAUTHENTICATED -> 401;
            case PERMISSION_DENIED -> 403;
            case RESOURCE_EXHAUSTED -> 429;
            case UNAVAILABLE -> 503;
            default -> 500;
        };
    }

    /**
     * Writes a standard error envelope consistent with {@code ErrorResponse}
     * in {@code common/src/main/proto/common/api.proto} ({status, message, code})
     * plus a {@code trace_id} for correlation. Never exposes the internal
     * exception or stack trace.
     */
    public static void sendError(RoutingContext ctx, int httpStatus, String message) {
        String safeMessage = message != null && !message.isBlank() ? message : "Unexpected error";
        ctx.response()
                .setStatusCode(httpStatus)
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject()
                        .put("status", "error")
                        .put("message", safeMessage)
                        .put("code", httpStatus)
                        .put("trace_id", resolveTraceId(ctx))
                        .encode());
    }

    /**
     * Prefers the trace id captured on the routing context by {@code
     * GatewayMetricsMiddleware} (valid even on async error paths where the
     * span is no longer current), falling back to {@link #currentTraceId()}.
     */
    private static String resolveTraceId(RoutingContext ctx) {
        String fromContext = ctx.get(GatewayMetricsMiddleware.TRACE_ID_KEY);
        return fromContext != null ? fromContext : currentTraceId();
    }

    /**
     * Applies a bounded deadline to an upstream gRPC call so a slow/unhealthy
     * service cannot hold a gateway request open indefinitely. The resulting
     * {@link TimeoutException} is mapped to HTTP 504 by {@link #handleError}.
     */
    public static <T> Future<T> withDeadline(Future<T> upstreamCall, long timeoutMs) {
        return upstreamCall.timeout(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the active OpenTelemetry trace id, or {@code "-"} when tracing
     * is not active for this request.
     */
    public static String currentTraceId() {
        var spanContext = Span.current().getSpanContext();
        return spanContext.isValid() ? spanContext.getTraceId() : "-";
    }

    /**
     * Serializes the current OpenTelemetry span as a W3C {@code traceparent}
     * header ({@code 00-<traceid>-<spanid>-<flags>}), or {@code null} when no
     * span is active. Captured synchronously while the gateway span is current
     * (see GatewayMetricsMiddleware) and injected by {@code ResilientGrpcClient}
     * into every downstream gRPC call.
     */
    public static String currentTraceparent() {
        var spanContext = Span.current().getSpanContext();
        if (!spanContext.isValid()) {
            return null;
        }
        return "00-" + spanContext.getTraceId() + "-" + spanContext.getSpanId()
                + "-" + (spanContext.isSampled() ? "01" : "00");
    }

    public static int getSafePathInt(RoutingContext ctx, String param) {
        try {
            return Integer.parseInt(ctx.pathParam(param));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid path parameter: " + param + " must be an integer");
        }
    }

    public static String getJsonString(JsonObject json, String key, String defaultValue) {
        String value = json.getString(key);
        return value != null ? value : defaultValue;
    }

    public static Integer getJsonInteger(JsonObject json, String key, Integer defaultValue) {
        Integer value = json.getInteger(key);
        return value != null ? value : defaultValue;
    }

    public static int getJsonInteger(JsonObject json, String key, int defaultValue) {
        try {
            return json.getInteger(key, defaultValue);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public static int getQueryInt(RoutingContext ctx, String key, int defaultValue) {
        try {
            return ctx.queryParams().contains(key) ? Integer.parseInt(ctx.queryParams().get(key)) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getQueryString(RoutingContext ctx, String key, String defaultValue) {
        return ctx.queryParams().contains(key) ? ctx.queryParams().get(key) : defaultValue;
    }

    public static String getFormString(RoutingContext ctx, String key, String defaultValue) {
        String value = ctx.request().getFormAttribute(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    public static int getFormInteger(RoutingContext ctx, String key, int defaultValue) {
        String value = ctx.request().getFormAttribute(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getRequiredFormString(RoutingContext ctx, String key) {
        String value = ctx.request().getFormAttribute(key);
        if (value == null || value.isBlank()) {
            throw new BadRequestException(key + " is required");
        }
        return value;
    }

    public static int getRequiredFormInteger(RoutingContext ctx, String key) {
        String value = ctx.request().getFormAttribute(key);
        if (value == null || value.isBlank()) {
            throw new BadRequestException(key + " is required");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BadRequestException(key + " must be a valid integer");
        }
    }

    public static FileUpload getFileUpload(RoutingContext ctx, String fieldName) {
        return ctx.fileUploads().stream()
                .filter(f -> fieldName.equals(f.name()))
                .findFirst()
                .orElse(null);
    }
}