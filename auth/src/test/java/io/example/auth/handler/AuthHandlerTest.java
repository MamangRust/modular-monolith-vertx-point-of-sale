package io.example.auth.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.auth.model.AuthUser;
import io.example.auth.service.IdentityService;
import io.example.auth.service.LoginService;
import io.example.auth.service.PasswordResetService;
import io.example.auth.service.RegisterService;
import io.vertx.core.Future;
import pb.Auth.ApiResponseForgotPassword;
import pb.Auth.ApiResponseGetMe;
import pb.Auth.ApiResponseLogin;
import pb.Auth.ApiResponseRefreshToken;
import pb.Auth.ApiResponseRegister;
import pb.Auth.ApiResponseResetPassword;
import pb.Auth.ApiResponseVerifyCode;
import pb.Auth.ForgotPasswordRequest;
import pb.Auth.GetMeRequest;
import pb.Auth.LoginRequest;
import pb.Auth.RefreshTokenRequest;
import pb.Auth.RegisterRequest;
import pb.Auth.ResetPasswordRequest;
import pb.Auth.VerifyCodeRequest;

@ExtendWith(MockitoExtension.class)
class AuthHandlerTest {

    @Mock
    RegisterService registerService;
    @Mock
    IdentityService identityService;
    @Mock
    PasswordResetService passwordResetService;
    @Mock
    LoginService loginService;

    private AuthHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AuthHandler(
                registerService, identityService, passwordResetService, loginService);
    }

    @Test
    void registerUser_shouldDelegateToRegisterService() {
        RegisterRequest request = RegisterRequest.newBuilder()
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@test.com")
                .setPassword("pass")
                .build();

        AuthUser mockUser = AuthUser.builder().userId(1).email("john@test.com").build();

        when(registerService.register(any())).thenReturn(Future.succeededFuture(mockUser));

        Future<ApiResponseRegister> result = handler.registerUser(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(registerService).register(any());
    }

    @Test
    void loginUser_shouldDelegateToLoginService() {
        LoginRequest request = LoginRequest.newBuilder()
                .setEmail("john@test.com")
                .setPassword("pass")
                .build();

        io.example.auth.model.TokenResponse tokenResponse = io.example.auth.model.TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(loginService.login(any())).thenReturn(Future.succeededFuture(tokenResponse));

        Future<ApiResponseLogin> result = handler.loginUser(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getAccessToken()).isEqualTo("access-token");
        assertThat(result.result().getData().getRefreshToken()).isEqualTo("refresh-token");
        verify(loginService).login(any());
    }

    @Test
    void getMe_shouldDelegateToIdentityService() {
        GetMeRequest request = GetMeRequest.newBuilder().setUserId(1).build();

        AuthUser mockUser = AuthUser.builder().userId(1).email("john@test.com").build();

        when(identityService.getMe(1)).thenReturn(Future.succeededFuture(mockUser));

        Future<ApiResponseGetMe> result = handler.getMe(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(identityService).getMe(1);
    }

    @Test
    void verifyCode_shouldDelegateToPasswordResetService() {
        VerifyCodeRequest request = VerifyCodeRequest.newBuilder().setCode("code-123").build();

        when(passwordResetService.verifyCode("code-123")).thenReturn(Future.succeededFuture(true));

        Future<ApiResponseVerifyCode> result = handler.verifyCode(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Verification code verified");
        verify(passwordResetService).verifyCode("code-123");
    }

    @Test
    void forgotPassword_shouldDelegateToPasswordResetService() {
        ForgotPasswordRequest request = ForgotPasswordRequest.newBuilder().setEmail("john@test.com").build();

        when(passwordResetService.forgotPassword("john@test.com")).thenReturn(Future.succeededFuture(true));

        Future<ApiResponseForgotPassword> result = handler.forgotPassword(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(passwordResetService).forgotPassword("john@test.com");
    }

    @Test
    void resetPassword_shouldDelegateToPasswordResetService() {
        ResetPasswordRequest request = ResetPasswordRequest.newBuilder()
                .setResetToken("token")
                .setPassword("pass")
                .setConfirmPassword("pass")
                .build();

        when(passwordResetService.resetPassword(any())).thenReturn(Future.succeededFuture(true));

        Future<ApiResponseResetPassword> result = handler.resetPassword(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(passwordResetService).resetPassword(any());
    }

    @Test
    void refreshToken_shouldDelegateToIdentityService() {
        RefreshTokenRequest request = RefreshTokenRequest.newBuilder().setRefreshToken("token-123").build();

        io.example.auth.model.TokenResponse tokenResponse = io.example.auth.model.TokenResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();

        when(identityService.refreshToken("token-123")).thenReturn(Future.succeededFuture(tokenResponse));

        Future<ApiResponseRefreshToken> result = handler.refreshToken(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getAccessToken()).isEqualTo("access-token");
        assertThat(result.result().getData().getRefreshToken()).isEqualTo("refresh-token");
        verify(identityService).refreshToken("token-123");
    }
}
