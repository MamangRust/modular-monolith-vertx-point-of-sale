package io.example.apigateway.handler;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.RequestBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.Auth;
import pb.VertxAuthServiceGrpcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthProxyHandlerTest {

    @Mock
    RoutingContext ctx;

    @Mock
    io.vertx.core.http.HttpServerResponse response;

    @Mock
    VertxAuthServiceGrpcClient client;

    @Mock
    RequestBody body;

    @Mock
    User user;

    @Captor
    ArgumentCaptor<Auth.RegisterRequest> registerCaptor;

    @Captor
    ArgumentCaptor<Auth.LoginRequest> loginCaptor;

    @Captor
    ArgumentCaptor<Auth.RefreshTokenRequest> refreshCaptor;

    @Captor
    ArgumentCaptor<Auth.GetMeRequest> getMeCaptor;

    private AuthProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AuthProxyHandler(client);
    }

    private void mockResponse() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
    }

    @Test
    void register_shouldBuildRequestAndCallGrpc() {
        mockResponse();
        JsonObject jsonObj = new JsonObject()
                .put("firstname", "John")
                .put("lastname", "Doe")
                .put("email", "john@example.com")
                .put("password", "secret123");
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(jsonObj);
        when(client.registerUser(any(Auth.RegisterRequest.class)))
                .thenReturn(Future.succeededFuture(Auth.ApiResponseRegister.getDefaultInstance()));

        handler.register(ctx);

        verify(client).registerUser(registerCaptor.capture());
        Auth.RegisterRequest req = registerCaptor.getValue();
        assertThat(req.getFirstname()).isEqualTo("John");
        assertThat(req.getLastname()).isEqualTo("Doe");
        assertThat(req.getEmail()).isEqualTo("john@example.com");
        assertThat(req.getPassword()).isEqualTo("secret123");
    }

    @Test
    void login_shouldBuildRequestAndCallGrpc() {
        mockResponse();
        JsonObject jsonObj = new JsonObject()
                .put("email", "john@example.com")
                .put("password", "secret123");
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(jsonObj);
        when(client.loginUser(any(Auth.LoginRequest.class)))
                .thenReturn(Future.succeededFuture(Auth.ApiResponseLogin.getDefaultInstance()));

        handler.login(ctx);

        verify(client).loginUser(loginCaptor.capture());
        Auth.LoginRequest req = loginCaptor.getValue();
        assertThat(req.getEmail()).isEqualTo("john@example.com");
        assertThat(req.getPassword()).isEqualTo("secret123");
    }

    @Test
    void refreshToken_shouldBuildRequestAndCallGrpc() {
        mockResponse();
        JsonObject jsonObj = new JsonObject()
                .put("refresh_token", "some-refresh-token-value");
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(jsonObj);
        when(client.refreshToken(any(Auth.RefreshTokenRequest.class)))
                .thenReturn(Future.succeededFuture(Auth.ApiResponseRefreshToken.getDefaultInstance()));

        handler.refreshToken(ctx);

        verify(client).refreshToken(refreshCaptor.capture());
        assertThat(refreshCaptor.getValue().getRefreshToken()).isEqualTo("some-refresh-token-value");
    }

    @Test
    void getMe_shouldReturn401WhenUserNotAuthenticated() {
        when(ctx.user()).thenReturn(null);
        when(ctx.response()).thenReturn(response);
        when(response.setStatusCode(401)).thenReturn(response);

        handler.getMe(ctx);

        verify(response).setStatusCode(401);
        verify(response).end("Unauthorized");
        verifyNoInteractions(client);
    }

    @Test
    void getMe_shouldCallGrpcWhenAuthenticated() {
        mockResponse();
        when(ctx.user()).thenReturn(user);
        when(user.principal()).thenReturn(new JsonObject().put("userId", 42));
        when(client.getMe(any(Auth.GetMeRequest.class)))
                .thenReturn(Future.succeededFuture(Auth.ApiResponseGetMe.getDefaultInstance()));

        handler.getMe(ctx);

        verify(client).getMe(getMeCaptor.capture());
        assertThat(getMeCaptor.getValue().getUserId()).isEqualTo(42);
    }

    @Test
    void logout_shouldReturnInlineResponse() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.end(anyString())).thenReturn(null);

        handler.logout(ctx);

        verify(response).putHeader("Content-Type", "application/json");
        verify(response).end(anyString());
        verifyNoInteractions(client);
    }

    @Test
    void register_shouldHandleError() {
        mockResponse();
        JsonObject jsonObj = new JsonObject()
                .put("firstname", "John")
                .put("lastname", "Doe")
                .put("email", "john@example.com")
                .put("password", "secret123");
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(jsonObj);

        StatusRuntimeException grpcError = new StatusRuntimeException(Status.INTERNAL);
        when(client.registerUser(any(Auth.RegisterRequest.class)))
                .thenReturn(Future.failedFuture(grpcError));

        handler.register(ctx);

        verify(client).registerUser(any(Auth.RegisterRequest.class));
        verify(response).setStatusCode(500);
    }

    @Test
    void login_shouldHandleError() {
        mockResponse();
        JsonObject jsonObj = new JsonObject()
                .put("email", "john@example.com")
                .put("password", "secret123");
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(jsonObj);

        StatusRuntimeException grpcError = new StatusRuntimeException(Status.UNAVAILABLE);
        when(client.loginUser(any(Auth.LoginRequest.class)))
                .thenReturn(Future.failedFuture(grpcError));

        handler.login(ctx);

        verify(client).loginUser(any(Auth.LoginRequest.class));
        verify(response).setStatusCode(503);
    }
}
