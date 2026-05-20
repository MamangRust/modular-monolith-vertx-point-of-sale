package io.example.apigateway.handler;

import io.example.apigateway.utils.ProtoMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import pb.cashier.Cashier;
import pb.cashier.VertxCashierServiceGrpcClient;
import pb.cashier.VertxCashierCommandServiceGrpcClient;

public class CashierProxyHandler {
    private final VertxCashierServiceGrpcClient queryClient;
    private final VertxCashierCommandServiceGrpcClient commandClient;

    public CashierProxyHandler(VertxCashierServiceGrpcClient queryClient, VertxCashierCommandServiceGrpcClient commandClient) {
        this.queryClient = queryClient;
        this.commandClient = commandClient;
    }

    public void findAll(RoutingContext ctx) {
        var req = Cashier.FindAllCashierRequest.newBuilder()
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findAll(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findActive(RoutingContext ctx) {
        var req = Cashier.FindAllCashierRequest.newBuilder()
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findByActive(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findTrashed(RoutingContext ctx) {
        var req = Cashier.FindAllCashierRequest.newBuilder()
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
        var req = Cashier.FindByIdCashierRequest.newBuilder().setId(id).build();

        queryClient.findById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findByMerchant(RoutingContext ctx) {
        int merchantId = Integer.parseInt(ctx.pathParam("merchantId"));
        var req = Cashier.FindByMerchantCashierRequest.newBuilder()
                .setMerchantId(merchantId)
                .setSearch(ctx.queryParams().get("search") != null ? ctx.queryParams().get("search") : "")
                .setPage(ctx.queryParams().contains("page") ? Integer.parseInt(ctx.queryParams().get("page")) : 1)
                .setPageSize(ctx.queryParams().contains("pageSize") ? Integer.parseInt(ctx.queryParams().get("pageSize")) : 10)
                .build();

        queryClient.findByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void create(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        var req = Cashier.CreateCashierRequest.newBuilder()
                .setMerchantId(body.getInteger("merchantId", 0))
                .setUserId(body.getInteger("userId", 0))
                .setName(body.getString("name", ""))
                .build();

        commandClient.createCashier(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 201))
                .onFailure(ctx::fail);
    }

    public void update(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject body = ctx.body().asJsonObject();
        var req = Cashier.UpdateCashierRequest.newBuilder()
                .setCashierId(id)
                .setName(body.getString("name", ""))
                .build();

        commandClient.updateCashier(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void trashed(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var req = Cashier.FindByIdCashierRequest.newBuilder().setId(id).build();

        commandClient.trashedCashier(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void restore(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var req = Cashier.FindByIdCashierRequest.newBuilder().setId(id).build();

        commandClient.restoreCashier(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void deletePermanent(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        var req = Cashier.FindByIdCashierRequest.newBuilder().setId(id).build();

        commandClient.deleteCashierPermanent(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void restoreAll(RoutingContext ctx) {
        commandClient.restoreAllCashier(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void deleteAllPermanent(RoutingContext ctx) {
        commandClient.deleteAllCashierPermanent(com.google.protobuf.Empty.getDefaultInstance())
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    // Cashier Stats
    public void findMonthlyTotalSales(RoutingContext ctx) {
        var req = Cashier.FindYearMonthTotalSales.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setMonth(Integer.parseInt(ctx.queryParams().get("month")))
                .build();
        queryClient.findMonthlyTotalSales(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findYearlyTotalSales(RoutingContext ctx) {
        var req = Cashier.FindYearTotalSales.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .build();
        queryClient.findYearlyTotalSales(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findMonthlyTotalSalesById(RoutingContext ctx) {
        var req = Cashier.FindYearMonthTotalSalesById.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setMonth(Integer.parseInt(ctx.queryParams().get("month")))
                .setCashierId(Integer.parseInt(ctx.pathParam("cashierId")))
                .build();
        queryClient.findMonthlyTotalSalesById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findYearlyTotalSalesById(RoutingContext ctx) {
        var req = Cashier.FindYearTotalSalesById.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setCashierId(Integer.parseInt(ctx.pathParam("cashierId")))
                .build();
        queryClient.findYearlyTotalSalesById(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findMonthlyTotalSalesByMerchant(RoutingContext ctx) {
        var req = Cashier.FindYearMonthTotalSalesByMerchant.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setMonth(Integer.parseInt(ctx.queryParams().get("month")))
                .setMerchantId(Integer.parseInt(ctx.pathParam("merchantId")))
                .build();
        queryClient.findMonthlyTotalSalesByMerchant(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(ctx::fail);
    }

    public void findYearlyTotalSalesByMerchant(RoutingContext ctx) {
        var req = Cashier.FindYearTotalSalesByMerchant.newBuilder()
                .setYear(Integer.parseInt(ctx.queryParams().get("year")))
                .setMerchantId(Integer.parseInt(ctx.pathParam("merchantId")))
                .build();
        queryClient.findYearlyTotalSalesByMerchant(req)
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
