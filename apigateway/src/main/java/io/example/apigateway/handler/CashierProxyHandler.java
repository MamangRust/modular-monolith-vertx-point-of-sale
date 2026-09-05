package io.example.apigateway.handler;

import static io.example.apigateway.utils.GrpcGatewayUtils.sendResponse;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.cashier.Cashier;
import pb.cashier.VertxCashierCommandServiceGrpcClient;
import pb.cashier.VertxCashierServiceGrpcClient;

@Slf4j
@RequiredArgsConstructor
public class CashierProxyHandler {
    private final VertxCashierServiceGrpcClient queryClient;
    private final VertxCashierCommandServiceGrpcClient commandClient;

    // =========================================================================
    // QUERIES
    // =========================================================================

    public void findAll(RoutingContext ctx) {
        var req = Cashier.FindAllCashierRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findAll(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findActive(RoutingContext ctx) {
        var req = Cashier.FindAllCashierRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findByActive(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findTrashed(RoutingContext ctx) {
        var req = Cashier.FindAllCashierRequest.newBuilder()
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
        var req = Cashier.FindByIdCashierRequest.newBuilder().setId(id).build();
        queryClient.findById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findByMerchant(RoutingContext ctx) {
        int merchantId = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
        var req = Cashier.FindByMerchantCashierRequest.newBuilder()
                .setMerchantId(merchantId)
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // =========================================================================
    // COMMANDS
    // =========================================================================

    public void create(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = Cashier.CreateCashierRequest.newBuilder()
                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                .setUserId(GrpcGatewayUtils.getJsonInteger(body, "user_id", 0))
                .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
                .build();
        commandClient.createCashier(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 201))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void update(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        JsonObject body = ctx.body().asJsonObject();
        var req = Cashier.UpdateCashierRequest.newBuilder()
                .setCashierId(id)
                .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
                .build();
        commandClient.updateCashier(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void trashed(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Cashier.FindByIdCashierRequest.newBuilder().setId(id).build();
        commandClient.trashedCashier(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restore(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Cashier.FindByIdCashierRequest.newBuilder().setId(id).build();
        commandClient.restoreCashier(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deletePermanent(RoutingContext ctx) {
        int id = GrpcGatewayUtils.getSafePathInt(ctx, "id");
        var req = Cashier.FindByIdCashierRequest.newBuilder().setId(id).build();
        commandClient.deleteCashierPermanent(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void restoreAll(RoutingContext ctx) {
        commandClient.restoreAllCashier(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void deleteAllPermanent(RoutingContext ctx) {
        commandClient.deleteAllCashierPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    // =========================================================================
    // STATS
    // =========================================================================

    public void findMonthlyTotalSales(RoutingContext ctx) {
        var req = Cashier.FindYearMonthTotalSales.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                .build();
        queryClient.findMonthlyTotalSales(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findYearlyTotalSales(RoutingContext ctx) {
        var req = Cashier.FindYearTotalSales.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .build();
        queryClient.findYearlyTotalSales(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findMonthlyTotalSalesById(RoutingContext ctx) {
        var req = Cashier.FindYearMonthTotalSalesById.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                .setCashierId(GrpcGatewayUtils.getSafePathInt(ctx, "cashierId"))
                .build();
        queryClient.findMonthlyTotalSalesById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findYearlyTotalSalesById(RoutingContext ctx) {
        var req = Cashier.FindYearTotalSalesById.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setCashierId(GrpcGatewayUtils.getSafePathInt(ctx, "cashierId"))
                .build();
        queryClient.findYearlyTotalSalesById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findMonthlyTotalSalesByMerchant(RoutingContext ctx) {
        var req = Cashier.FindYearMonthTotalSalesByMerchant.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
        queryClient.findMonthlyTotalSalesByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findYearlyTotalSalesByMerchant(RoutingContext ctx) {
        var req = Cashier.FindYearTotalSalesByMerchant.newBuilder()
                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                .setMerchantId(GrpcGatewayUtils.getSafePathInt(ctx, "merchantId"))
                .build();
        queryClient.findYearlyTotalSalesByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }
}
