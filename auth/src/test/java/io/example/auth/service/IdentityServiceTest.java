package io.example.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.auth.model.AuthUser;
import io.example.auth.model.TokenResponse;
import io.example.auth.repository.RefreshTokenRepository;
import io.example.auth.repository.UserRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    RedisService redisService;
    @Mock
    TokenService tokenService;
    @Mock
    JWTAuth jwtProvider;
    @Mock
    TracingMetrics tracingMetrics;
    @Mock
    TracingMetrics.TracingContext tracingContext;

    private IdentityService identityService;

    @BeforeEach
    void setUp() {
        identityService = new IdentityService(
                userRepository, refreshTokenRepository, redisService, tokenService, jwtProvider, tracingMetrics);
    }

    @Test
    void refreshToken_shouldReturnCachedToken() {
        String token = "token-123";

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.get("refreshToken:" + token)).thenReturn(Future.succeededFuture("1"));
        when(redisService.delete("refreshToken:" + token)).thenReturn(Future.succeededFuture(1L));
        when(userRepository.findById(1)).thenReturn(Future.succeededFuture(AuthUser.builder().userId(1).build()));
        when(tokenService.createAccessToken(1)).thenReturn("new-access-token");
        when(tokenService.createRefreshToken(1)).thenReturn("new-refresh-token");
        when(redisService.set(eq("refreshToken:new-refresh-token"), eq("1"), any(Duration.class)))
                .thenReturn(Future.succeededFuture("OK"));

        Future<TokenResponse> result = identityService.refreshToken(token);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.result().getRefreshToken()).isEqualTo("new-refresh-token");
        verify(redisService).delete("refreshToken:" + token);
    }

    @Test
    void refreshToken_shouldValidateJwtWhenNotCached() {
        String token = "token-456";

        io.vertx.ext.auth.User jwtUser = mock(io.vertx.ext.auth.User.class);
        JsonObject principal = new JsonObject().put("sub", "1");

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.get("refreshToken:" + token)).thenReturn(Future.succeededFuture(null));
        when(jwtUser.principal()).thenReturn(principal);
        when(jwtProvider.authenticate(any(TokenCredentials.class))).thenReturn(Future.succeededFuture(jwtUser));
        when(refreshTokenRepository.deleteRefreshToken(token)).thenReturn(Future.succeededFuture());
        when(userRepository.findById(1)).thenReturn(Future.succeededFuture(AuthUser.builder().userId(1).build()));
        when(tokenService.createAccessToken(1)).thenReturn("new-access-token");
        when(tokenService.createRefreshToken(1)).thenReturn("new-refresh-token");
        when(refreshTokenRepository.updateRefreshToken(eq(1), eq("new-refresh-token"), any()))
                .thenReturn(Future.succeededFuture(null));
        when(redisService.set(eq("refreshToken:new-refresh-token"), eq("1"), any(Duration.class)))
                .thenReturn(Future.succeededFuture("OK"));

        Future<TokenResponse> result = identityService.refreshToken(token);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getAccessToken()).isEqualTo("new-access-token");
        assertThat(result.result().getRefreshToken()).isEqualTo("new-refresh-token");
        verify(refreshTokenRepository).deleteRefreshToken(token);
        verify(refreshTokenRepository).updateRefreshToken(eq(1), eq("new-refresh-token"), any());
    }

    @Test
    void refreshToken_shouldRejectMissingUser() {
        String token = "token-missing-user";
        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.get("refreshToken:" + token)).thenReturn(Future.succeededFuture("99"));
        when(userRepository.findById(99)).thenReturn(Future.succeededFuture(null));

        Future<TokenResponse> result = identityService.refreshToken(token);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(io.example.common.exception.grpc.UnauthorizedException.class);
    }

    @Test
    void getMe_shouldReturnCachedUser() {
        AuthUser mockUser = AuthUser.builder().userId(1).email("test@test.com").build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.getJson(eq("user:1"), eq(AuthUser.class))).thenReturn(Future.succeededFuture(mockUser));

        Future<AuthUser> result = identityService.getMe(1);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getUserId()).isEqualTo(1);
        assertThat(result.result().getEmail()).isEqualTo("test@test.com");
        verify(userRepository, never()).findById(anyInt());
    }

    @Test
    void getMe_shouldFetchFromDbWhenNotCached() {
        AuthUser mockUser = AuthUser.builder().userId(1).email("test@test.com").build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.getJson(eq("user:1"), eq(AuthUser.class))).thenReturn(Future.succeededFuture(null));
        when(userRepository.findById(1)).thenReturn(Future.succeededFuture(mockUser));
        when(redisService.setJson(eq("user:1"), eq(mockUser), any(Duration.class)))
                .thenReturn(Future.succeededFuture("OK"));

        Future<AuthUser> result = identityService.getMe(1);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getUserId()).isEqualTo(1);
        assertThat(result.result().getEmail()).isEqualTo("test@test.com");
        verify(redisService).setJson(eq("user:1"), eq(mockUser), any(Duration.class));
    }

    @Test
    void getMe_shouldFailWhenUserNotFound() {
        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.getJson(eq("user:1"), eq(AuthUser.class))).thenReturn(Future.succeededFuture(null));
        when(userRepository.findById(1)).thenReturn(Future.succeededFuture(null));

        Future<AuthUser> result = identityService.getMe(1);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause().getMessage()).isEqualTo("User not found");
    }
}
