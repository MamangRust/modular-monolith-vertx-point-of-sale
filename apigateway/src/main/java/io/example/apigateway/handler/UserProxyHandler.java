package io.example.apigateway.handler;

import static io.example.apigateway.utils.GrpcGatewayUtils.sendResponse;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.user.User;
import pb.user.UserCommand;
import pb.user.VertxUserCommandServiceGrpcClient;
import pb.user.VertxUserQueryServiceGrpcClient;

@Slf4j
@RequiredArgsConstructor
public class UserProxyHandler {
    private final VertxUserQueryServiceGrpcClient queryClient;
    private final VertxUserCommandServiceGrpcClient commandClient;

    public void findAll(RoutingContext ctx) {
        var req = User.FindAllUserRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findAll(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findActive(RoutingContext ctx) {
        var req = User.FindAllUserRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findByActive(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findTrashed(RoutingContext ctx) {
        var req = User.FindAllUserRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findByTrashed(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findById(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = User.FindByIdUserRequest.newBuilder().setId(id).build();
        queryClient.findById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void update(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        JsonObject body = ctx.body().asJsonObject();
        var req = UserCommand.UpdateUserRequest.newBuilder()
                .setId(id)
                .setFirstname(GrpcGatewayUtils.getJsonString(body, "firstname", ""))
                .setLastname(GrpcGatewayUtils.getJsonString(body, "lastname", ""))
                .setEmail(GrpcGatewayUtils.getJsonString(body, "email", ""))
                .setPassword(GrpcGatewayUtils.getJsonString(body, "password", ""))
                .setConfirmPassword(GrpcGatewayUtils.getJsonString(body, "confirm_password", ""))
                .build();
        commandClient.update(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restore(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = User.FindByIdUserRequest.newBuilder().setId(id).build();
        commandClient.restoreUser(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void trashed(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = User.FindByIdUserRequest.newBuilder().setId(id).build();
        commandClient.trashedUser(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deletePermanent(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = User.FindByIdUserRequest.newBuilder().setId(id).build();
        commandClient.deleteUserPermanent(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restoreAllUsers(RoutingContext ctx) {
        commandClient.restoreAllUser(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deleteAllPermanentUsers(RoutingContext ctx) {
        commandClient.deleteAllUserPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }
}
