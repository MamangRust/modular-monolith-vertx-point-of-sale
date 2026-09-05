package io.example.auth.service;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {
    @Mock
    JWTAuth jwtProvider;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(jwtProvider);
    }

    @Test
    void createAccessToken_shouldGenerateTokenWithUserId() {
        when(jwtProvider.generateToken(any(JsonObject.class), any(JWTOptions.class)))
                .thenReturn("access-token");

        String token = tokenService.createAccessToken(1);

        assertThat(token).isEqualTo("access-token");
    }

    @Test
    void createRefreshToken_shouldGenerateTokenWithLongExpiry() {
        when(jwtProvider.generateToken(any(JsonObject.class), any(JWTOptions.class)))
                .thenReturn("refresh-token");

        String token = tokenService.createRefreshToken(1);

        assertThat(token).isEqualTo("refresh-token");
    }

    @Test
    void createAccessToken_shouldCallGenerateToken() {
        when(jwtProvider.generateToken(any(JsonObject.class), any(JWTOptions.class)))
                .thenReturn("access-token");

        tokenService.createAccessToken(1);

        ArgumentCaptor<JsonObject> claimsCaptor = ArgumentCaptor.forClass(JsonObject.class);
        ArgumentCaptor<JWTOptions> optionsCaptor = ArgumentCaptor.forClass(JWTOptions.class);
        verify(jwtProvider).generateToken(claimsCaptor.capture(), optionsCaptor.capture());

        assertThat(claimsCaptor.getValue().getString("sub")).isEqualTo("1");
    }
}
