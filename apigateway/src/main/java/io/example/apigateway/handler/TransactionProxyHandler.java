package io.example.apigateway.handler;

import static io.example.apigateway.utils.GrpcGatewayUtils.sendResponse;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.transaction.VertxTransactionCommandServiceGrpcClient;
import pb.transaction.VertxTransactionQueryServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcClient;

@Slf4j
@RequiredArgsConstructor
public class TransactionProxyHandler {
        private final VertxTransactionQueryServiceGrpcClient queryClient;
        private final VertxTransactionCommandServiceGrpcClient commandClient;
        private final VertxTransactionStatsMethodServiceGrpcClient statsMethodClient;
        private final VertxTransactionStatsStatusServiceGrpcClient statsStatusClient;

        // =========================================================================
        // QUERIES
        // =========================================================================

        public void getTransactions(RoutingContext ctx) {
                var req = pb.transaction.FindAllTransactionRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findAll(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getActiveTransactions(RoutingContext ctx) {
                var req = pb.transaction.FindAllTransactionRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findByActive(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getTrashedTransactions(RoutingContext ctx) {
                var req = pb.transaction.FindAllTransactionRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findByTrashed(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getTransactionById(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "transactionId");
                var req = pb.transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(id).build();
                queryClient.findById(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void createTransaction(RoutingContext ctx) {
                JsonObject body = ctx.body().asJsonObject();
                var req = pb.transaction.CreateTransactionRequest.newBuilder()
                                .setOrderId(GrpcGatewayUtils.getJsonInteger(body, "order_id", 0))
                                .setAmount(GrpcGatewayUtils.getJsonInteger(body, "amount", 0))
                                .setPaymentMethod(GrpcGatewayUtils.getJsonString(body, "payment_method", ""))
                                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                                .build();

                commandClient.create(req)
                                .onSuccess(r -> sendResponse(ctx, r, 201))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void updateTransaction(RoutingContext ctx) {
                JsonObject body = ctx.body().asJsonObject();
                var req = pb.transaction.UpdateTransactionRequest.newBuilder()
                                .setTransactionId(GrpcGatewayUtils.getJsonInteger(body, "transaction_id", 0))
                                .setPaymentMethod(GrpcGatewayUtils.getJsonString(body, "payment_method", ""))
                                .setAmount(GrpcGatewayUtils.getJsonInteger(body, "amount", 0))
                                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                                .setOrderId(GrpcGatewayUtils.getJsonInteger(body, "order_id", 0))
                                .setPaymentStatus(GrpcGatewayUtils.getJsonString(body, "payment_status", ""))
                                .build();
                commandClient.update(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        // =========================================================================
        // COMMANDS (LIFECYCLE)
        // =========================================================================

        public void trashTransaction(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "transactionId");
                var req = pb.transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(id).build();

                commandClient.trashedTransaction(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void restoreTransaction(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "transactionId");
                var req = pb.transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(id).build();
                commandClient.restoreTransaction(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void deleteTransactionPermanently(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "transactionId");
                var req = pb.transaction.FindByIdTransactionRequest.newBuilder().setTransactionId(id).build();
                commandClient.deleteTransactionPermanent(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void restoreAllTransactions(RoutingContext ctx) {
                commandClient.restoreAllTransaction(com.google.protobuf.Empty.getDefaultInstance())
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void deleteAllPermanentTransactions(RoutingContext ctx) {
                commandClient.deleteAllTransactionPermanent(com.google.protobuf.Empty.getDefaultInstance())
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        // =========================================================================
        // STATS — STATUS (SUCCESS/FAILED)
        // =========================================================================

        public void getMonthTransactionStatusSuccess(RoutingContext ctx) {
                var req = pb.transaction.stats.FindMonthlyTransactionStatus.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                                .build();
                statsStatusClient.findMonthStatusSuccess(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getYearlyTransactionStatusSuccess(RoutingContext ctx) {
                var req = pb.transaction.stats.FindYearlyTransactionStatus.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .build();
                statsStatusClient.findYearStatusSuccess(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getMonthTransactionStatusFailed(RoutingContext ctx) {
                var req = pb.transaction.stats.FindMonthlyTransactionStatus.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                                .build();
                statsStatusClient.findMonthStatusFailed(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getYearlyTransactionStatusFailed(RoutingContext ctx) {
                var req = pb.transaction.stats.FindYearlyTransactionStatus.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .build();
                statsStatusClient.findYearStatusFailed(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        // STATS BY MERCHANT (Status)
        // =========================================================================

        public void getMonthTransactionStatusSuccessByMerchant(RoutingContext ctx) {
                int merchantId = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = pb.transaction.stats.FindMonthlyTransactionStatusByMerchant.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                                .setMerchantId(merchantId)
                                .build();

                statsStatusClient.findMonthStatusSuccessByMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getYearlyTransactionStatusSuccessByMerchant(RoutingContext ctx) {
                int merchantId = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = pb.transaction.stats.FindYearlyTransactionStatusByMerchant.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .setMerchantId(merchantId)
                                .build();

                statsStatusClient.findYearStatusSuccessByMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getMonthTransactionStatusFailedByMerchant(RoutingContext ctx) {
                int merchantId = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = pb.transaction.stats.FindMonthlyTransactionStatusByMerchant.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                                .setMerchantId(merchantId)
                                .build();

                statsStatusClient.findMonthStatusFailedByMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getYearlyTransactionStatusFailedByMerchant(RoutingContext ctx) {
                int merchantId = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = pb.transaction.stats.FindYearlyTransactionStatusByMerchant.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .setMerchantId(merchantId)
                                .build();

                statsStatusClient.findYearStatusFailedByMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        // =========================================================================
        // STATS — METHODS (Payment Methods)
        // =========================================================================

        public void getMonthlyPaymentMethods(RoutingContext ctx) {
                var req = pb.transaction.stats.MonthTransactionMethod.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                                .build();

                statsMethodClient.findMonthMethodSuccess(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getYearlyPaymentMethods(RoutingContext ctx) {
                var req = pb.transaction.stats.YearTransactionMethod.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .build();

                statsMethodClient.findYearMethodSuccess(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        // STATS BY MERCHANT (Methods)
        // =========================================================================

        public void getMonthlyPaymentMethodsByMerchant(RoutingContext ctx) {
                int merchantId = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = pb.transaction.stats.MonthTransactionMethodByMerchant.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .setMonth(GrpcGatewayUtils.getQueryInt(ctx, "month", 1))
                                .setMerchantId(merchantId)
                                .build();

                statsMethodClient.findMonthMethodByMerchantSuccess(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getYearlyPaymentMethodsByMerchant(RoutingContext ctx) {
                int merchantId = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = pb.transaction.stats.YearTransactionMethodByMerchant.newBuilder()
                                .setYear(GrpcGatewayUtils.getQueryInt(ctx, "year", 2024))
                                .setMerchantId(merchantId)
                                .build();

                statsMethodClient.findYearMethodByMerchantSuccess(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }
}