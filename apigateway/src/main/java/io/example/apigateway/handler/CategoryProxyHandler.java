package io.example.apigateway.handler;

import static io.example.apigateway.utils.GrpcGatewayUtils.sendResponse;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.category.Category;
import pb.category.CategoryCommand;
import pb.category.VertxCategoryCommandServiceGrpcClient;
import pb.category.VertxCategoryServiceGrpcClient;

@Slf4j
@RequiredArgsConstructor
public class CategoryProxyHandler {
    private final VertxCategoryServiceGrpcClient queryClient;
    private final VertxCategoryCommandServiceGrpcClient commandClient;

    public void findAll(RoutingContext ctx) {
        var req = Category.FindAllCategoryRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findAll(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findActive(RoutingContext ctx) {
        var req = Category.FindAllCategoryRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findByActive(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findTrashed(RoutingContext ctx) {
        var req = Category.FindAllCategoryRequest.newBuilder()
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
        var req = Category.FindByIdCategoryRequest.newBuilder().setId(id).build();
        queryClient.findById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void create(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = CategoryCommand.CreateCategoryRequest.newBuilder()
                .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
                .setDescription(GrpcGatewayUtils.getJsonString(body, "description", ""))
                .build();
        commandClient.create(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 201))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void update(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        JsonObject body = ctx.body().asJsonObject();
        var req = CategoryCommand.UpdateCategoryRequest.newBuilder()
                .setCategoryId(id)
                .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
                .setDescription(GrpcGatewayUtils.getJsonString(body, "description", ""))
                .build();
        commandClient.update(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void trashed(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Category.FindByIdCategoryRequest.newBuilder().setId(id).build();
        commandClient.trashedCategory(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restore(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Category.FindByIdCategoryRequest.newBuilder().setId(id).build();
        commandClient.restoreCategory(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deletePermanent(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Category.FindByIdCategoryRequest.newBuilder().setId(id).build();
        commandClient.deleteCategoryPermanent(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restoreAll(RoutingContext ctx) {
        commandClient.restoreAllCategory(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deleteAllPermanent(RoutingContext ctx) {
        commandClient.deleteAllCategoryPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }
}
