package io.example.auth.service;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TokenService {
    private final JWTAuth jwtProvider;

    public String createAccessToken(Integer userId) {
        return createAccessToken(userId, new JsonArray());
    }

    public String createAccessToken(Integer userId, JsonArray roleNames) {
        return jwtProvider.generateToken(
                new JsonObject()
                        .put("sub", userId.toString())
                        .put("userId", userId)
                        .put("roleNames", roleNames == null ? new JsonArray() : roleNames),
                new JWTOptions().setExpiresInMinutes(15));
    }

    public String createRefreshToken(Integer userId) {
        return createRefreshToken(userId, new JsonArray());
    }

    public String createRefreshToken(Integer userId, JsonArray roleNames) {
        return jwtProvider.generateToken(
                new JsonObject()
                        .put("sub", userId.toString())
                        .put("userId", userId)
                        .put("roleNames", roleNames == null ? new JsonArray() : roleNames),
                new JWTOptions().setExpiresInMinutes(1440) // 24 hours
        );
    }
}
