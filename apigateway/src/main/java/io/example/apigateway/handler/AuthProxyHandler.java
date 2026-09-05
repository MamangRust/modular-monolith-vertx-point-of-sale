package io.example.apigateway.handler;

import static io.example.apigateway.utils.GrpcGatewayUtils.sendResponse;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.Auth;
import pb.VertxAuthServiceGrpcClient;

@Slf4j
@RequiredArgsConstructor
public class AuthProxyHandler {
    private static final long DEFAULT_DEADLINE_MS = 5000;
    private final VertxAuthServiceGrpcClient client;

    public void register(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = Auth.RegisterRequest.newBuilder()
                .setFirstname(GrpcGatewayUtils.getJsonString(body, "firstname", ""))
                .setLastname(GrpcGatewayUtils.getJsonString(body, "lastname", ""))
                .setEmail(GrpcGatewayUtils.getJsonString(body, "email", ""))
                .setPassword(GrpcGatewayUtils.getJsonString(body, "password", ""))
                .setConfirmPassword(GrpcGatewayUtils.getJsonString(body, "confirm_password", ""))
                .build();
        GrpcGatewayUtils.withDeadline(client.registerUser(req), DEFAULT_DEADLINE_MS)
                .onSuccess(resp -> sendResponse(ctx, resp, 201))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void login(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = Auth.LoginRequest.newBuilder()
                .setEmail(GrpcGatewayUtils.getJsonString(body, "email", ""))
                .setPassword(GrpcGatewayUtils.getJsonString(body, "password", ""))
                .build();
        GrpcGatewayUtils.withDeadline(client.loginUser(req), DEFAULT_DEADLINE_MS)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void refreshToken(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = Auth.RefreshTokenRequest.newBuilder()
                .setRefreshToken(GrpcGatewayUtils.getJsonString(body, "refresh_token", ""))
                .build();
        GrpcGatewayUtils.withDeadline(client.refreshToken(req), DEFAULT_DEADLINE_MS)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void getMe(RoutingContext ctx) {
        if (ctx.user() == null || ctx.user().principal() == null) {
            ctx.response().setStatusCode(401).end("Unauthorized");
            return;
        }
        int userId = ctx.user().principal().getInteger("userId", 0);
        var req = Auth.GetMeRequest.newBuilder().setUserId(userId).build();
        GrpcGatewayUtils.withDeadline(client.getMe(req), DEFAULT_DEADLINE_MS)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void logout(RoutingContext ctx) {
        ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(new JsonObject().put("status", 200).put("message", "Successfully logged out").encode());
    }
}
