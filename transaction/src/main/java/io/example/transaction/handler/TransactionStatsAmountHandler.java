package io.example.transaction.handler;

import io.example.transaction.service.TransactionStatsService;
import io.vertx.core.Future;
import pb.transaction.Transaction.FindByYearCardNumberTransactionRequest;
import pb.transaction.Transaction.FindYearTransactionStatus;
import pb.transaction.stats.TransactionStatsAmount.*;

public class TransactionStatsAmountHandler implements pb.transaction.stats.VertxTransactionStatsAmountServiceGrpcServer.TransactionStatsAmountServiceApi {
    private final TransactionStatsService service;

    public TransactionStatsAmountHandler(TransactionStatsService service) {
        this.service = service;
    }

    @Override
    public Future<ApiResponseTransactionMonthAmount> findMonthlyAmounts(FindYearTransactionStatus req) {
        return service.findMonthlyAmounts(req.getYear())
                .map(list -> ApiResponseTransactionMonthAmount.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly successful amounts fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toMonthAmountResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionYearAmount> findYearlyAmounts(FindYearTransactionStatus req) {
        return service.findYearlyAmounts(req.getYear())
                .map(list -> ApiResponseTransactionYearAmount.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly successful amounts fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toYearAmountResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionMonthAmount> findMonthlyAmountsByCardNumber(FindByYearCardNumberTransactionRequest req) {
        return service.findMonthlyAmountsByCardNumber(req.getCardNumber(), req.getYear())
                .map(list -> ApiResponseTransactionMonthAmount.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly successful amounts by card fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toMonthAmountResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionYearAmount> findYearlyAmountsByCardNumber(FindByYearCardNumberTransactionRequest req) {
        return service.findYearlyAmountsByCardNumber(req.getCardNumber(), req.getYear())
                .map(list -> ApiResponseTransactionYearAmount.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly successful amounts by card fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toYearAmountResponse).toList())
                        .build());
    }
}
