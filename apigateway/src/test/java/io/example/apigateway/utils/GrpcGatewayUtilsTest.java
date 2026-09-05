package io.example.apigateway.utils;

import com.google.protobuf.MessageOrBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrpcGatewayUtilsTest {

    @Mock
    RoutingContext ctx;
    @Mock
    HttpServerResponse response;
    @Mock
    HttpServerRequest request;
    @Mock
    MessageOrBuilder proto;

    // ── sendResponse ────────────────────────────────────────────────────────

    @Test
    void sendResponse_shouldSetStatusCodeContentTypeAndBody() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(200)).thenReturn(response);

        GrpcGatewayUtils.sendResponse(ctx, proto, 200);

        verify(response).setStatusCode(200);
        verify(response).putHeader("Content-Type", "application/json");
        verify(response).end(anyString());
    }

    // ── handleError ────────────────────────────────────────────────────────

    @Test
    void handleError_shouldMapNotFoundTo404() {
        var sre = Status.NOT_FOUND.withDescription("user not found").asRuntimeException();
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(404)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, sre);

        verify(response).setStatusCode(404);
        var captor = ArgumentCaptor.forClass(String.class);
        verify(response).end(captor.capture());
        assertThat(captor.getValue()).contains("user not found");
    }

    @Test
    void handleError_shouldMapInvalidArgumentTo400() {
        var sre = Status.INVALID_ARGUMENT.withDescription("bad request").asRuntimeException();
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(400)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, sre);

        verify(response).setStatusCode(400);
    }

    @Test
    void handleError_shouldMapFailedPreconditionTo400() {
        var sre = Status.FAILED_PRECONDITION.asRuntimeException();
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(400)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, sre);

        verify(response).setStatusCode(400);
    }

    @Test
    void handleError_shouldMapAlreadyExistsTo409() {
        var sre = Status.ALREADY_EXISTS.asRuntimeException();
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(409)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, sre);

        verify(response).setStatusCode(409);
    }

    @Test
    void handleError_shouldMapResourceExhaustedTo429() {
        var sre = Status.RESOURCE_EXHAUSTED.asRuntimeException();
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(429)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, sre);

        verify(response).setStatusCode(429);
    }

    @Test
    void handleError_shouldMapApiExceptionToItsStatusCode() {
        var apiEx = new io.example.common.exception.api.BadRequestException("bad form");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(400)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, apiEx);

        verify(response).setStatusCode(400);
    }

    @Test
    void handleError_shouldMapUnauthenticatedTo401() {
        var sre = Status.UNAUTHENTICATED.asRuntimeException();
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(401)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, sre);

        verify(response).setStatusCode(401);
    }

    @Test
    void handleError_shouldMapPermissionDeniedTo403() {
        var sre = Status.PERMISSION_DENIED.asRuntimeException();
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(403)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, sre);

        verify(response).setStatusCode(403);
    }

    @Test
    void handleError_shouldMapUnavailableTo503() {
        var sre = Status.UNAVAILABLE.asRuntimeException();
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(503)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, sre);

        verify(response).setStatusCode(503);
    }

    @Test
    void handleError_shouldMapUnknownTo500() {
        var sre = Status.INTERNAL.withDescription("boom").asRuntimeException();
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(500)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, sre);

        verify(response).setStatusCode(500);
    }

    @Test
    void handleError_shouldFallbackTo500ForNonGrpcException() {
        var err = new RuntimeException("generic");
        GrpcGatewayUtils.handleError(ctx, err);
        verify(ctx).fail(500, err);
    }

    @Test
    void handleError_shouldParseUpstreamInvalidGrpcStatusTo401() {
        // Vert.x gRPC client fails the future with a plain message instead of
        // a StatusRuntimeException — the gateway must parse the numeric code
        // so UnauthorizedException reaches the client as 401 (gap #17).
        var err = new RuntimeException("Invalid gRPC status 16");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(401)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, err);

        verify(response).setStatusCode(401);
    }

    @Test
    void handleError_shouldParseUpstreamInvalidGrpcStatusNotFoundTo404() {
        var err = new RuntimeException("Invalid gRPC status 5");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(404)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, err);

        verify(response).setStatusCode(404);
    }

    @Test
    void handleError_shouldParseUpstreamInvalidGrpcStatusUnavailableTo503() {
        var err = new RuntimeException("Invalid gRPC status 14");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(503)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, err);

        verify(response).setStatusCode(503);
    }

    @Test
    void handleError_shouldFallbackTo500ForUnknownUpstreamGrpcStatusCode() {
        var err = new RuntimeException("Invalid gRPC status 999");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(500)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, err);

        verify(response).setStatusCode(500);
    }

    @Test
    void handleError_shouldUseCodeNameWhenDescriptionIsNull() {
        var sre = Status.PERMISSION_DENIED.asRuntimeException(); // no description
        when(ctx.response()).thenReturn(response);
        when(response.putHeader("Content-Type", "application/json")).thenReturn(response);
        when(response.setStatusCode(403)).thenReturn(response);

        GrpcGatewayUtils.handleError(ctx, sre);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(response).end(captor.capture());
        assertThat(captor.getValue()).contains("PERMISSION_DENIED");
    }

    // ── getSafePathInt ─────────────────────────────────────────────────────

    @Test
    void getSafePathInt_shouldReturnIntWhenParamIsValid() {
        when(ctx.pathParam("id")).thenReturn("42");
        assertThat(GrpcGatewayUtils.getSafePathInt(ctx, "id")).isEqualTo(42);
    }

    @Test
    void getSafePathInt_shouldThrowWhenParamIsNotAnInt() {
        when(ctx.pathParam("id")).thenReturn("abc");
        assertThatThrownBy(() -> GrpcGatewayUtils.getSafePathInt(ctx, "id"))
                .isInstanceOf(io.example.common.exception.api.BadRequestException.class)
                .hasMessageContaining("must be an integer");
    }

    // ── getJsonString ──────────────────────────────────────────────────────

    @Test
    void getJsonString_shouldReturnValueWhenKeyExists() {
        var json = new JsonObject().put("name", "test");
        assertThat(GrpcGatewayUtils.getJsonString(json, "name", "default")).isEqualTo("test");
    }

    @Test
    void getJsonString_shouldReturnDefaultWhenKeyIsNull() {
        var json = new JsonObject().put("name", (String) null);
        assertThat(GrpcGatewayUtils.getJsonString(json, "name", "default")).isEqualTo("default");
    }

    @Test
    void getJsonString_shouldReturnDefaultWhenKeyMissing() {
        var json = new JsonObject();
        assertThat(GrpcGatewayUtils.getJsonString(json, "missing", "default")).isEqualTo("default");
    }

    // ── getJsonInteger (object) ─────────────────────────────────────────────

    @Test
    void getJsonInteger_shouldReturnValueWhenKeyExists() {
        var json = new JsonObject().put("count", 10);
        assertThat(GrpcGatewayUtils.getJsonInteger(json, "count", 0)).isEqualTo(10);
    }

    @Test
    void getJsonInteger_shouldReturnDefaultWhenKeyMissing() {
        var json = new JsonObject();
        assertThat(GrpcGatewayUtils.getJsonInteger(json, "missing", 42)).isEqualTo(42);
    }

    // ── getJsonInteger (primitive int) ──────────────────────────────────────

    @Test
    void getJsonIntegerPrimitive_shouldReturnValueWhenKeyExists() {
        var json = new JsonObject().put("count", 7);
        assertThat(GrpcGatewayUtils.getJsonInteger(json, "count", 0)).isEqualTo(7);
    }

    @Test
    void getJsonIntegerPrimitive_shouldReturnDefaultWhenKeyMissing() {
        var json = new JsonObject();
        assertThat(GrpcGatewayUtils.getJsonInteger(json, "missing", -1)).isEqualTo(-1);
    }

    @Test
    void getJsonIntegerPrimitive_shouldReturnDefaultOnClassCast() {
        var json = new JsonObject().put("count", "not-a-number");
        assertThat(GrpcGatewayUtils.getJsonInteger(json, "count", 99)).isEqualTo(99);
    }

    // ── getQueryInt ─────────────────────────────────────────────────────────

    @Test
    void getQueryInt_shouldReturnValueWhenPresent() {
        var params = MultiMap.caseInsensitiveMultiMap().add("page", "3");
        when(ctx.queryParams()).thenReturn(params);

        assertThat(GrpcGatewayUtils.getQueryInt(ctx, "page", 1)).isEqualTo(3);
    }

    @Test
    void getQueryInt_shouldReturnDefaultWhenMissing() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        assertThat(GrpcGatewayUtils.getQueryInt(ctx, "page", 1)).isEqualTo(1);
    }

    @Test
    void getQueryInt_shouldReturnDefaultWhenInvalid() {
        var params = MultiMap.caseInsensitiveMultiMap().add("page", "abc");
        when(ctx.queryParams()).thenReturn(params);
        assertThat(GrpcGatewayUtils.getQueryInt(ctx, "page", 1)).isEqualTo(1);
    }

    // ── getQueryString ──────────────────────────────────────────────────────

    @Test
    void getQueryString_shouldReturnValueWhenPresent() {
        var params = MultiMap.caseInsensitiveMultiMap().add("q", "test");
        when(ctx.queryParams()).thenReturn(params);
        assertThat(GrpcGatewayUtils.getQueryString(ctx, "q", "default")).isEqualTo("test");
    }

    @Test
    void getQueryString_shouldReturnDefaultWhenMissing() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        assertThat(GrpcGatewayUtils.getQueryString(ctx, "q", "default")).isEqualTo("default");
    }

    // ── getFormString ───────────────────────────────────────────────────────

    @Test
    void getFormString_shouldReturnValueWhenPresent() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("name")).thenReturn("alice");

        assertThat(GrpcGatewayUtils.getFormString(ctx, "name", "default")).isEqualTo("alice");
    }

    @Test
    void getFormString_shouldReturnDefaultWhenNull() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("name")).thenReturn(null);

        assertThat(GrpcGatewayUtils.getFormString(ctx, "name", "default")).isEqualTo("default");
    }

    @Test
    void getFormString_shouldReturnDefaultWhenBlank() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("name")).thenReturn("   ");

        assertThat(GrpcGatewayUtils.getFormString(ctx, "name", "default")).isEqualTo("default");
    }

    // ── getFormInteger ──────────────────────────────────────────────────────

    @Test
    void getFormInteger_shouldReturnValueWhenValid() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("qty")).thenReturn("5");

        assertThat(GrpcGatewayUtils.getFormInteger(ctx, "qty", 0)).isEqualTo(5);
    }

    @Test
    void getFormInteger_shouldReturnDefaultWhenNull() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("qty")).thenReturn(null);

        assertThat(GrpcGatewayUtils.getFormInteger(ctx, "qty", 1)).isEqualTo(1);
    }

    @Test
    void getFormInteger_shouldReturnDefaultWhenBlank() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("qty")).thenReturn("");

        assertThat(GrpcGatewayUtils.getFormInteger(ctx, "qty", 1)).isEqualTo(1);
    }

    @Test
    void getFormInteger_shouldReturnDefaultWhenInvalid() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("qty")).thenReturn("abc");

        assertThat(GrpcGatewayUtils.getFormInteger(ctx, "qty", 1)).isEqualTo(1);
    }

    // ── getRequiredFormString ───────────────────────────────────────────────

    @Test
    void getRequiredFormString_shouldReturnValueWhenPresent() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("key")).thenReturn("value");

        assertThat(GrpcGatewayUtils.getRequiredFormString(ctx, "key")).isEqualTo("value");
    }

    @Test
    void getRequiredFormString_shouldThrowWhenNull() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("key")).thenReturn(null);

        assertThatThrownBy(() -> GrpcGatewayUtils.getRequiredFormString(ctx, "key"))
                .isInstanceOf(io.example.common.exception.api.BadRequestException.class)
                .hasMessageContaining("key is required");
    }

    // ── getRequiredFormInteger ──────────────────────────────────────────────

    @Test
    void getRequiredFormInteger_shouldReturnValueWhenValid() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("num")).thenReturn("99");

        assertThat(GrpcGatewayUtils.getRequiredFormInteger(ctx, "num")).isEqualTo(99);
    }

    @Test
    void getRequiredFormInteger_shouldThrowWhenNull() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("num")).thenReturn(null);

        assertThatThrownBy(() -> GrpcGatewayUtils.getRequiredFormInteger(ctx, "num"))
                .isInstanceOf(io.example.common.exception.api.BadRequestException.class);
    }

    @Test
    void getRequiredFormInteger_shouldThrowWhenInvalid() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("num")).thenReturn("abc");

        assertThatThrownBy(() -> GrpcGatewayUtils.getRequiredFormInteger(ctx, "num"))
                .isInstanceOf(io.example.common.exception.api.BadRequestException.class)
                .hasMessageContaining("must be a valid integer");
    }

    // ── getFileUpload ───────────────────────────────────────────────────────

    @Test
    void getFileUpload_shouldReturnMatchingUpload() {
        var upload = mock(FileUpload.class);
        when(upload.name()).thenReturn("avatar");
        when(ctx.fileUploads()).thenReturn(List.of(upload));

        assertThat(GrpcGatewayUtils.getFileUpload(ctx, "avatar")).isSameAs(upload);
    }

    @Test
    void getFileUpload_shouldReturnNullWhenNotFound() {
        when(ctx.fileUploads()).thenReturn(Collections.emptyList());

        assertThat(GrpcGatewayUtils.getFileUpload(ctx, "missing")).isNull();
    }
}
