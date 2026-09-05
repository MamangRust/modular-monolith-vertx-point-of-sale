package io.example.apigateway.handler;

import static io.example.apigateway.utils.GrpcGatewayUtils.sendResponse;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.role.Role;
import pb.role.RoleCommand;
import pb.role.VertxRoleCommandServiceGrpcClient;
import pb.role.VertxRoleServiceGrpcClient;

@Slf4j
@RequiredArgsConstructor
public class RoleProxyHandler {
    private final VertxRoleServiceGrpcClient queryClient;
    private final VertxRoleCommandServiceGrpcClient commandClient;

    public void findAll(RoutingContext ctx) {
        var req = Role.FindAllRoleRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findAllRole(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findActive(RoutingContext ctx) {
        var req = Role.FindAllRoleRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findByActive(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findTrashed(RoutingContext ctx) {
        var req = Role.FindAllRoleRequest.newBuilder()
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
        var req = Role.FindByIdRoleRequest.newBuilder().setRoleId(id).build();
        queryClient.findByIdRole(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void create(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = RoleCommand.CreateRoleRequest.newBuilder()
                .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
                .build();
        commandClient.createRole(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 201))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void update(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        JsonObject body = ctx.body().asJsonObject();
        var req = RoleCommand.UpdateRoleRequest.newBuilder()
                .setId(id)
                .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
                .build();
        commandClient.updateRole(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void trashed(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Role.FindByIdRoleRequest.newBuilder().setRoleId(id).build();
        commandClient.trashedRole(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restore(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Role.FindByIdRoleRequest.newBuilder().setRoleId(id).build();
        commandClient.restoreRole(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deletePermanent(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Role.FindByIdRoleRequest.newBuilder().setRoleId(id).build();
        commandClient.deleteRolePermanent(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restoreAllRoles(RoutingContext ctx) {
        commandClient.restoreAllRole(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deleteAllPermanentRoles(RoutingContext ctx) {
        commandClient.deleteAllRolePermanent(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }
}
