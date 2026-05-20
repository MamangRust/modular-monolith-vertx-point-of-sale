package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.order.Order;
import pb.order.VertxOrderQueryServiceGrpcClient;
import pb.order.VertxOrderCommandServiceGrpcClient;

public class OrderProxyHandler {
    private final VertxOrderQueryServiceGrpcClient queryClient;
    private final VertxOrderCommandServiceGrpcClient commandClient;

    public OrderProxyHandler(VertxOrderQueryServiceGrpcClient queryClient, VertxOrderCommandServiceGrpcClient commandClient) {
        this.queryClient = queryClient;
        this.commandClient = commandClient;
    }

    public void findAll(RoutingContext ctx) {
        var req = Order.FindAllOrderRequest.newBuilder()
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findAll(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findByMerchant(RoutingContext ctx) {
        int merchantId = Integer.parseInt(ctx.pathParam("merchantId"));
        var req = Order.FindAllOrderMerchantRequest.newBuilder()
                .setMerchantId(merchantId)
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findActive(RoutingContext ctx) {
        var req = Order.FindAllOrderRequest.newBuilder()
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findByActive(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findTrashed(RoutingContext ctx) {
        var req = Order.FindAllOrderRequest.newBuilder()
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
        var req = Order.FindByIdOrderRequest.newBuilder().setId(id).build();

        queryClient.findById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void create(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var builder = Order.CreateOrderRequest.newBuilder()
                .setMerchantId(body.getInteger("merchantId", 0))
                .setCashierId(body.getInteger("cashierId", 0));

        if (body.containsKey("items")) {
            body.getJsonArray("items").forEach(itemObj -> {
                JsonObject item = (JsonObject) itemObj;
                builder.addItems(Order.CreateOrderItemRequest.newBuilder()
                        .setProductId(item.getInteger("productId", 0))
                        .setQuantity(item.getInteger("quantity", 0))
                        .build());
            });
        }

        commandClient.create(builder.build())
                .onSuccess(resp -> sendResponse(ctx, resp, 201))
                .onFailure(ctx::fail);
    }

    public void update(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject body = ctx.body().asJsonObject();
        var builder = Order.UpdateOrderRequest.newBuilder()
                .setOrderId(id)
                .setCashierId(body.getInteger("cashierId", 0));

        if (body.containsKey("items")) {
            body.getJsonArray("items").forEach(itemObj -> {
                JsonObject item = (JsonObject) itemObj;
                builder.addItems(Order.UpdateOrderItemRequest.newBuilder()
                        .setOrderItemId(item.getInteger("orderItemId", 0))
                        .setProductId(item.getInteger("productId", 0))
                        .setQuantity(item.getInteger("quantity", 0))
                        .build());
            });
        }

        commandClient.update(builder.build())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void trashed(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var req = Order.FindByIdOrderRequest.newBuilder().setId(id).build();

        commandClient.trashedOrder(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void restore(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var req = Order.FindByIdOrderRequest.newBuilder().setId(id).build();

        commandClient.restoreOrder(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void deletePermanent(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var req = Order.FindByIdOrderRequest.newBuilder().setId(id).build();

        commandClient.deleteOrderPermanent(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void restoreAll(RoutingContext ctx) {
        commandClient.restoreAllOrder(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void deleteAllPermanent(RoutingContext ctx) {
        commandClient.deleteAllOrderPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    // Revenue Stats
    public void findMonthlyTotalRevenue(RoutingContext ctx) {
        var req = Order.FindYearMonthTotalRevenue.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setMonth(Integer.parseInt(ctx.queryParams().get("month")))
                .build();
        queryClient.findMonthlyTotalRevenue(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findYearlyTotalRevenue(RoutingContext ctx) {
        var req = Order.FindYearTotalRevenue.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .build();
        queryClient.findYearlyTotalRevenue(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findMonthlyTotalRevenueById(RoutingContext ctx) {
        var req = Order.FindYearMonthTotalRevenueById.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setMonth(Integer.parseInt(ctx.queryParams().get("month")))
                .setOrderId(Integer.parseInt(ctx.pathParam("orderId")))
                .build();
        queryClient.findMonthlyTotalRevenueById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findYearlyTotalRevenueById(RoutingContext ctx) {
        var req = Order.FindYearTotalRevenueById.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setOrderId(Integer.parseInt(ctx.pathParam("orderId")))
                .build();
        queryClient.findYearlyTotalRevenueById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findMonthlyTotalRevenueByMerchant(RoutingContext ctx) {
        var req = Order.FindYearMonthTotalRevenueByMerchant.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setMonth(Integer.parseInt(ctx.queryParams().get("month")))
                .setMerchantId(Integer.parseInt(ctx.pathParam("merchantId")))
                .build();
        queryClient.findMonthlyTotalRevenueByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findYearlyTotalRevenueByMerchant(RoutingContext ctx) {
        var req = Order.FindYearTotalRevenueByMerchant.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setMerchantId(Integer.parseInt(ctx.pathParam("merchantId")))
                .build();
        queryClient.findYearlyTotalRevenueByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findMonthlyRevenue(RoutingContext ctx) {
        var req = Order.FindYearOrder.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .build();
        queryClient.findMonthlyRevenue(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findYearlyRevenue(RoutingContext ctx) {
        var req = Order.FindYearOrder.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .build();
        queryClient.findYearlyRevenue(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findMonthlyRevenueByMerchant(RoutingContext ctx) {
        var req = Order.FindYearOrderByMerchant.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setMerchantId(Integer.parseInt(ctx.pathParam("merchantId")))
                .build();
        queryClient.findMonthlyRevenueByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findYearlyRevenueByMerchant(RoutingContext ctx) {
        var req = Order.FindYearOrderByMerchant.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setMerchantId(Integer.parseInt(ctx.pathParam("merchantId")))
                .build();
        queryClient.findYearlyRevenueByMerchant(req)
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
