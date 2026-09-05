package io.example.apigateway.handler;

import static io.example.apigateway.utils.GrpcGatewayUtils.sendResponse;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.order.Order;
import pb.order.VertxOrderCommandServiceGrpcClient;
import pb.order.VertxOrderQueryServiceGrpcClient;

@Slf4j
@RequiredArgsConstructor
public class OrderProxyHandler {
    private final VertxOrderQueryServiceGrpcClient queryClient;
    private final VertxOrderCommandServiceGrpcClient commandClient;

    // =========================================================================
    // QUERIES
    // =========================================================================

    public void findAll(RoutingContext ctx) {
        var req = Order.FindAllOrderRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findAll(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findByMerchant(RoutingContext ctx) {
        int merchantId = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
        var req = Order.FindAllOrderMerchantRequest.newBuilder()
                .setMerchantId(merchantId)
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findActive(RoutingContext ctx) {
        var req = Order.FindAllOrderRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findByActive(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findTrashed(RoutingContext ctx) {
        var req = Order.FindAllOrderRequest.newBuilder()
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
        var req = Order.FindByIdOrderRequest.newBuilder().setId(id).build();
        queryClient.findById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // =========================================================================
    // COMMANDS
    // =========================================================================

    public void create(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var builder = Order.CreateOrderRequest.newBuilder()
                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                .setCashierId(GrpcGatewayUtils.getJsonInteger(body, "cashier_id", 0));

        if (body.containsKey("items")) {
            body.getJsonArray("items").forEach(itemObj -> {
                JsonObject item = (JsonObject) itemObj;
                builder.addItems(Order.CreateOrderItemRequest.newBuilder()
                        .setProductId(GrpcGatewayUtils.getJsonInteger(item, "product_id", 0))
                        .setQuantity(GrpcGatewayUtils.getJsonInteger(item, "quantity", 0))
                        .build());
            });
        }

        commandClient.create(builder.build())
                .onSuccess(resp -> sendResponse(ctx, resp, 201))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void update(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        JsonObject body = ctx.body().asJsonObject();
        var builder = Order.UpdateOrderRequest.newBuilder()
                .setOrderId(id)
                .setCashierId(GrpcGatewayUtils.getJsonInteger(body, "cashier_id", 0));

        if (body.containsKey("items")) {
            body.getJsonArray("items").forEach(itemObj -> {
                JsonObject item = (JsonObject) itemObj;
                builder.addItems(Order.UpdateOrderItemRequest.newBuilder()
                        .setOrderItemId(GrpcGatewayUtils.getJsonInteger(item, "order_item_id", 0))
                        .setProductId(GrpcGatewayUtils.getJsonInteger(item, "product_id", 0))
                        .setQuantity(GrpcGatewayUtils.getJsonInteger(item, "quantity", 0))
                        .build());
            });
        }

        commandClient.update(builder.build())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void trashed(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Order.FindByIdOrderRequest.newBuilder().setId(id).build();
        commandClient.trashedOrder(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restore(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Order.FindByIdOrderRequest.newBuilder().setId(id).build();
        commandClient.restoreOrder(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deletePermanent(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Order.FindByIdOrderRequest.newBuilder().setId(id).build();
        commandClient.deleteOrderPermanent(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restoreAll(RoutingContext ctx) {
        commandClient.restoreAllOrder(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deleteAllPermanent(RoutingContext ctx) {
        commandClient.deleteAllOrderPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // =========================================================================
    // STATS — REVENUE
    // =========================================================================

    public void findMonthlyTotalRevenue(RoutingContext ctx) {
        var req = Order.FindYearMonthTotalRevenue.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                .build();
        queryClient.findMonthlyTotalRevenue(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findYearlyTotalRevenue(RoutingContext ctx) {
        var req = Order.FindYearTotalRevenue.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .build();
        queryClient.findYearlyTotalRevenue(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findMonthlyTotalRevenueById(RoutingContext ctx) {
        var req = Order.FindYearMonthTotalRevenueById.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                .setOrderId(GrpcGatewayUtils.getSafePathInt(ctx, "orderId"))
                .build();
        queryClient.findMonthlyTotalRevenueById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findYearlyTotalRevenueById(RoutingContext ctx) {
        var req = Order.FindYearTotalRevenueById.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setOrderId(GrpcGatewayUtils.getSafePathInt(ctx, "orderId"))
                .build();
        queryClient.findYearlyTotalRevenueById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findMonthlyTotalRevenueByMerchant(RoutingContext ctx) {
        var req = Order.FindYearMonthTotalRevenueByMerchant.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
        queryClient.findMonthlyTotalRevenueByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findYearlyTotalRevenueByMerchant(RoutingContext ctx) {
        var req = Order.FindYearTotalRevenueByMerchant.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
        queryClient.findYearlyTotalRevenueByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findMonthlyRevenue(RoutingContext ctx) {
        var req = Order.FindYearOrder.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .build();
        queryClient.findMonthlyRevenue(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findYearlyRevenue(RoutingContext ctx) {
        var req = Order.FindYearOrder.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .build();
        queryClient.findYearlyRevenue(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findMonthlyRevenueByMerchant(RoutingContext ctx) {
        var req = Order.FindYearOrderByMerchant.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
        queryClient.findMonthlyRevenueByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findYearlyRevenueByMerchant(RoutingContext ctx) {
        var req = Order.FindYearOrderByMerchant.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
        queryClient.findYearlyRevenueByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }
}
