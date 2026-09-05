package io.example.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.auth.model.AuthUser;
import io.example.auth.model.ResetPasswordRequest;
import io.example.auth.model.ResetToken;
import io.example.auth.repository.ResetTokenRepository;
import io.example.auth.repository.UserRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    ResetTokenRepository resetTokenRepository;
    @Mock
    RedisService redisService;
    @Mock
    TracingMetrics tracingMetrics;
    @Mock
    KafkaService kafkaService;
    @Mock
    TracingMetrics.TracingContext tracingContext;

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository, resetTokenRepository, redisService, tracingMetrics, kafkaService);
    }

    @Test
    void forgotPassword_shouldCreateResetTokenAndSendEmail() {
        AuthUser user = AuthUser.builder().userId(1).email("john@test.com").build();
        ResetToken resetToken = ResetToken.builder().userId(1).token("dummy-token").build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(userRepository.findByEmail("john@test.com")).thenReturn(Future.succeededFuture(user));
        when(resetTokenRepository.createResetToken(anyInt(), anyString(), any(LocalDateTime.class)))
                .thenReturn(Future.succeededFuture(resetToken));
        when(redisService.set(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Future.succeededFuture("OK"));
        when(kafkaService.sendMessage(eq("email-service-topic-auth-forgot-password"), eq("1"), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture());

        Future<Boolean> result = passwordResetService.forgotPassword("john@test.com");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
        verify(kafkaService).sendMessage(eq("email-service-topic-auth-forgot-password"), anyString(), any(JsonObject.class));
    }

    @Test
    void forgotPassword_shouldFailWhenUserNotFound() {
        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Future.succeededFuture(null));

        Future<Boolean> result = passwordResetService.forgotPassword("unknown@test.com");

        assertThat(result.failed()).isTrue();
        assertThat(result.cause().getMessage()).isEqualTo("User not found");
    }

    @Test
    void resetPassword_shouldUpdatePasswordWhenTokensMatch() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken("token-123")
                .password("new-password")
                .confirmPassword("new-password")
                .build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.get("resetToken:" + request.getResetToken())).thenReturn(Future.succeededFuture("1"));
        when(userRepository.updateUserPassword(1, "new-password")).thenReturn(Future.succeededFuture(
                AuthUser.builder().userId(1).build()));

        Future<Boolean> result = passwordResetService.resetPassword(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
        verify(userRepository).updateUserPassword(1, "new-password");
    }

    @Test
    void resetPassword_shouldFailWhenPasswordsDontMatch() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken("token-123")
                .password("new-password")
                .confirmPassword("different-password")
                .build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);

        Future<Boolean> result = passwordResetService.resetPassword(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause().getMessage()).isEqualTo("Passwords do not match");
    }

    @Test
    void resetPassword_shouldFailWhenTokenInvalid() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .resetToken("token-123")
                .password("new-password")
                .confirmPassword("new-password")
                .build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(redisService.get("resetToken:" + request.getResetToken())).thenReturn(Future.succeededFuture(null));
        when(resetTokenRepository.findByToken("token-123")).thenReturn(Future.succeededFuture(null));

        Future<Boolean> result = passwordResetService.resetPassword(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause().getMessage()).isEqualTo("Invalid or expired reset token");
    }

    @Test
    void verifyCode_shouldVerifyUser() {
        AuthUser user = AuthUser.builder().userId(1).email("test@test.com").build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(userRepository.findByVerificationCode("code-123")).thenReturn(Future.succeededFuture(user));
        when(userRepository.updateUserIsVerified(1, true)).thenReturn(Future.succeededFuture(user));
        when(redisService.delete("verification:test@test.com")).thenReturn(Future.succeededFuture(1L));
        when(kafkaService.sendMessage(eq("email-service-topic-auth-verify-code-success"), eq("1"), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture());

        Future<Boolean> result = passwordResetService.verifyCode("code-123");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
        verify(kafkaService).sendMessage(eq("email-service-topic-auth-verify-code-success"), anyString(), any(JsonObject.class));
    }

    @Test
    void verifyCode_shouldFailWhenCodeInvalid() {
        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(userRepository.findByVerificationCode("invalid-code")).thenReturn(Future.succeededFuture(null));

        Future<Boolean> result = passwordResetService.verifyCode("invalid-code");

        assertThat(result.failed()).isTrue();
        assertThat(result.cause().getMessage()).isEqualTo("Invalid verification code");
    }
}
