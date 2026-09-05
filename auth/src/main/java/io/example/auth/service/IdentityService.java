package io.example.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import io.vertx.core.json.JsonArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.auth.model.AuthUser;
import io.example.auth.model.TokenResponse;
import io.example.auth.repository.RefreshTokenRepository;
import io.example.auth.repository.UserRepository;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.exception.grpc.UnauthorizedException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class IdentityService {
        private static final Logger logger = LoggerFactory.getLogger(IdentityService.class);

        private final UserRepository userRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final RedisService redisService;
        private final TokenService tokenService;
        private final JWTAuth jwtProvider;
        private final TracingMetrics tracingMetrics;

        public Future<TokenResponse> refreshToken(String token) {
                String method = "RefreshToken";
                TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(method);

                return redisService.get("refreshToken:" + token)
                                .<TokenResponse>compose(cachedUserId -> {
                                        if (cachedUserId != null) {
                                                Integer userId = Integer.parseInt(cachedUserId);
                                                redisService.delete("refreshToken:" + token);
                                                logger.debug("Invalidated old refresh token from cache: {}", token);

                                                return issueTokens(userId)
                                                                .compose(tokens -> redisService
                                                                                .set("refreshToken:" + tokens.getRefreshToken(),
                                                                                                userId.toString(),
                                                                                                Duration.ofHours(24))
                                                                                .map(v -> tokens));
                                        }

                                        // If not in cache, validate JWT
                                        return jwtProvider.authenticate(new TokenCredentials(token))
                                                        .recover(err -> Future.failedFuture(
                                                                        new UnauthorizedException("Invalid or expired refresh token")))
                                                        .<TokenResponse>compose(user -> {
                                                                final Integer userId = Integer.parseInt(
                                                                                user.principal().getString("sub"));

                                                                return refreshTokenRepository.deleteRefreshToken(token)
                                                                                .compose(v -> {
                                                                                        return issueTokens(userId)
                                                                                                        .compose(tokens -> {
                                                                                                                LocalDateTime expiryTime = LocalDateTime
                                                                                                                                .now()
                                                                                                                                .plusHours(24);
                                                                                                                return refreshTokenRepository
                                                                                                                                .updateRefreshToken(
                                                                                                                                                userId,
                                                                                                                                                tokens.getRefreshToken(),
                                                                                                                                                expiryTime)
                                                                                                                                .compose(rt -> redisService
                                                                                                                                                .set("refreshToken:"
                                                                                                                                                                + tokens.getRefreshToken(),
                                                                                                                                                                userId.toString(),
                                                                                                                                                                Duration.ofHours(24))
                                                                                                                                                .map(v2 -> tokens));
                                                                                                        });
                                                                                });
                                                        });
                                })
                                .onSuccess(res -> tracingMetrics.completeSpanSuccess(tracingContext, method,
                                                "Token refreshed successfully"))
                                .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, method,
                                                err.getMessage()));
        }

        private Future<TokenResponse> issueTokens(Integer userId) {
                Future<AuthUser> userFuture = userRepository.findById(userId);
                if (userFuture == null) {
                        return Future.failedFuture(new UnauthorizedException("Invalid or expired refresh token"));
                }
                return userFuture.compose(user -> {
                        if (user == null || user.getDeletedAt() != null) {
                                return Future.failedFuture(
                                                new UnauthorizedException("Invalid or expired refresh token"));
                        }
                        JsonArray roles = user.getRoles() == null
                                        ? new JsonArray()
                                        : new JsonArray(user.getRoles());
                        return Future.succeededFuture(buildTokens(userId, roles));
                });
        }

        private TokenResponse buildTokens(Integer userId, JsonArray roles) {
                String accessToken = roles == null || roles.isEmpty()
                                ? tokenService.createAccessToken(userId)
                                : tokenService.createAccessToken(userId, roles);
                String refreshToken = roles == null || roles.isEmpty()
                                ? tokenService.createRefreshToken(userId)
                                : tokenService.createRefreshToken(userId, roles);
                return TokenResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .build();
        }

        public Future<AuthUser> getMe(Integer userId) {
                String method = "GetMe";
                TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(method);

                return redisService.getJson("user:" + userId, AuthUser.class)
                                .compose(cachedUser -> {
                                        if (cachedUser != null) {
                                                return Future.succeededFuture(cachedUser);
                                        }

                                        return userRepository.findById(userId)
                                                        .compose(user -> {
                                                                if (user == null) {
                                                                        return Future.failedFuture(
                                                                                        new NotFoundException("User not found"));
                                                                }
                                                                return redisService
                                                                                .setJson("user:" + userId, user,
                                                                                                Duration.ofMinutes(5))
                                                                                .map(v -> user);
                                                        });
                                })
                                .onSuccess(res -> tracingMetrics.completeSpanSuccess(tracingContext, method,
                                                "User details fetched successfully"))
                                .onFailure(err -> tracingMetrics.completeSpanError(tracingContext, method,
                                                err.getMessage()));
        }
}
