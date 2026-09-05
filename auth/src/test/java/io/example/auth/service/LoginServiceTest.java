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

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.example.auth.model.AuthRequest;
import io.example.auth.model.AuthUser;
import io.example.auth.model.TokenResponse;
import io.example.auth.repository.UserRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class LoginServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    RedisService redisService;
    @Mock
    TokenService tokenService;
    @Mock
    TracingMetrics tracingMetrics;
    @Mock
    TracingMetrics.TracingContext tracingContext;

    private LoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginService(
                userRepository, redisService, tokenService, tracingMetrics);
    }

    @Test
    void login_shouldReturnTokenOnSuccess() {
        String password = "correct-password";
        String hash = BCrypt.withDefaults().hashToString(4, password.toCharArray());
        AuthUser user = AuthUser.builder().userId(1).password(hash).build();
        AuthRequest request = new AuthRequest("test@test.com", password);

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.exists(anyString())).thenReturn(Future.succeededFuture(false));
        when(userRepository.findByEmailAndVerify(anyString())).thenReturn(Future.succeededFuture(user));
        when(tokenService.createAccessToken(1)).thenReturn("access-token");
        when(tokenService.createRefreshToken(1)).thenReturn("refresh-token");
        when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));

        Future<TokenResponse> result = loginService.login(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getAccessToken()).isEqualTo("access-token");
        assertThat(result.result().getRefreshToken()).isEqualTo("refresh-token");
        verify(redisService, times(2)).delete(anyString());
    }

    @Test
    void login_shouldFailWhenAccountLocked() {
        AuthRequest request = new AuthRequest("test@test.com", "password");

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.exists(anyString())).thenReturn(Future.succeededFuture(true));

        Future<TokenResponse> result = loginService.login(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause().getMessage()).isEqualTo("Account is locked due to too many failed attempts");
    }

    @Test
    void login_shouldFailWhenUserNotFound() {
        AuthRequest request = new AuthRequest("test@test.com", "password");

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.exists(anyString())).thenReturn(Future.succeededFuture(false));
        when(userRepository.findByEmailAndVerify(anyString())).thenReturn(Future.succeededFuture(null));

        Future<TokenResponse> result = loginService.login(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(io.example.common.exception.grpc.UnauthorizedException.class);
        assertThat(result.cause().getMessage()).isEqualTo("Invalid credentials");
    }

    @Test
    void login_shouldFailWhenWrongPassword() {
        String hash = BCrypt.withDefaults().hashToString(4, "correct-password".toCharArray());
        AuthUser user = AuthUser.builder().userId(1).password(hash).build();
        AuthRequest request = new AuthRequest("test@test.com", "wrong-password");

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.exists(anyString())).thenReturn(Future.succeededFuture(false));
        when(userRepository.findByEmailAndVerify(anyString())).thenReturn(Future.succeededFuture(user));
        when(redisService.incr(anyString())).thenReturn(Future.succeededFuture(1L));

        Future<TokenResponse> result = loginService.login(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(io.example.common.exception.grpc.UnauthorizedException.class);
        assertThat(result.cause().getMessage()).isEqualTo("Invalid credentials");
        verify(redisService).incr(anyString());
        verify(redisService).expire(anyString(), any(Duration.class));
    }

    @Test
    void login_shouldLockAccountAfter5Attempts() {
        String hash = BCrypt.withDefaults().hashToString(4, "correct-password".toCharArray());
        AuthUser user = AuthUser.builder().userId(1).password(hash).build();
        AuthRequest request = new AuthRequest("test@test.com", "wrong-password");

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.exists(anyString())).thenReturn(Future.succeededFuture(false));
        when(userRepository.findByEmailAndVerify(anyString())).thenReturn(Future.succeededFuture(user));
        when(redisService.incr(anyString())).thenReturn(Future.succeededFuture(5L));
        when(redisService.set(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Future.succeededFuture("OK"));

        Future<TokenResponse> result = loginService.login(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(io.example.common.exception.grpc.TooManyRequestsException.class);
        assertThat(result.cause().getMessage()).isEqualTo("Account locked due to too many failed attempts");
        verify(redisService).set(eq("account_locked:test@test.com"), eq("true"), any(Duration.class));
    }
}
