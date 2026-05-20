package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.product.Product;
import pb.product.ProductCommand;
import pb.product.VertxProductServiceGrpcClient;
import pb.product.VertxProductCommandServiceGrpcClient;

public class ProductProxyHandler {
    private final VertxProductServiceGrpcClient queryClient;
    private final VertxProductCommandServiceGrpcClient commandClient;

    public ProductProxyHandler(VertxProductServiceGrpcClient queryClient, VertxProductCommandServiceGrpcClient commandClient) {
        this.queryClient = queryClient;
        this.commandClient = commandClient;
    }

    public void findAll(RoutingContext ctx) {
        var req = Product.FindAllProductRequest.newBuilder()
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findAll(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findActive(RoutingContext ctx) {
        var req = Product.FindAllProductRequest.newBuilder()
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findByActive(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findTrashed(RoutingContext ctx) {
        var req = Product.FindAllProductRequest.newBuilder()
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findByTrashed(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findById(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var req = Product.FindByIdProductRequest.newBuilder().setId(id).build();

        queryClient.findById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findByMerchant(RoutingContext ctx) {
        int merchantId = Integer.parseInt(ctx.pathParam("merchantId"));
        var req = Product.FindAllProductMerchantRequest.newBuilder()
                .setMerchantId(merchantId)
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setCategoryId(ctx.queryParams().contains("categoryId") ? Integer.parseInt(ctx.queryParams().get("categoryId")) : 0)
                .setMinPrice(ctx.queryParams().contains("minPrice") ? Integer.parseInt(ctx.queryParams().get("minPrice")) : 0)
                .setMaxPrice(ctx.queryParams().contains("maxPrice") ? Integer.parseInt(ctx.queryParams().get("maxPrice")) : 0)
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findByCategory(RoutingContext ctx) {
        String categoryName = ctx.pathParam("categoryName");
        var req = Product.FindAllProductCategoryRequest.newBuilder()
                .setCategoryName(categoryName)
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setMinprice(ctx.queryParams().contains("minPrice") ? Integer.parseInt(ctx.queryParams().get("minPrice")) : 0)
                .setMaxprice(ctx.queryParams().contains("maxPrice") ? Integer.parseInt(ctx.queryParams().get("maxPrice")) : 0)
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findByCategory(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void create(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = ProductCommand.CreateProductRequest.newBuilder()
                .setMerchantId(body.getInteger("merchantId", 0))
                .setCategoryId(body.getInteger("categoryId", 0))
                .setName(body.getString("name", ""))
                .setDescription(body.getString("description", ""))
                .setPrice(body.getInteger("price", 0))
                .setCountInStock(body.getInteger("countInStock", 0))
                .setBrand(body.getString("brand", ""))
                .setWeight(body.getInteger("weight", 0))
                .setImageProduct(body.getString("imageProduct", ""))
                .build();

        commandClient.create(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 201))
                .onFailure(ctx::fail);
    }

    public void update(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject body = ctx.body().asJsonObject();
        var req = ProductCommand.UpdateProductRequest.newBuilder()
                .setProductId(id)
                .setMerchantId(body.getInteger("merchantId", 0))
                .setCategoryId(body.getInteger("categoryId", 0))
                .setName(body.getString("name", ""))
                .setDescription(body.getString("description", ""))
                .setPrice(body.getInteger("price", 0))
                .setCountInStock(body.getInteger("countInStock", 0))
                .setBrand(body.getString("brand", ""))
                .setWeight(body.getInteger("weight", 0))
                .setImageProduct(body.getString("imageProduct", ""))
                .build();

        commandClient.update(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void trashed(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var req = Product.FindByIdProductRequest.newBuilder().setId(id).build();

        commandClient.trashedProduct(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void restore(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var req = Product.FindByIdProductRequest.newBuilder().setId(id).build();

        commandClient.restoreProduct(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void deletePermanent(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var req = Product.FindByIdProductRequest.newBuilder().setId(id).build();

        commandClient.deleteProductPermanent(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void restoreAll(RoutingContext ctx) {
        commandClient.restoreAllProduct(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void deleteAllPermanent(RoutingContext ctx) {
        commandClient.deleteAllProductPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    private void sendResponse(RoutingContext ctx, com.google.protobuf.MessageOrBuilder proto, int defaultStatus) {
        JsonObject json = ProtoMapper.toJson(proto);
        int status = json.getInteger("status", defaultStatus);
        ctx.response()
                .setStatusCode(status == 0 ? defaultStatus : status)
                .putHeader("Content-Type", "application/json")
                .end(json.encode());
    }
}
