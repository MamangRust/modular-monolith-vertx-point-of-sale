package io.example.transaction.handler;

import io.example.transaction.service.TransactionStatsService;
import io.vertx.core.Future;
import pb.transaction.Transaction.FindByYearCardNumberTransactionRequest;
import pb.transaction.Transaction.FindYearTransactionStatus;
import pb.transaction.stats.TransactionStatsMethod.*;

public class TransactionStatsMethodHandler implements pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcServer.TransactionStatsMethodServiceApi {
    private final TransactionStatsService service;

    public TransactionStatsMethodHandler(TransactionStatsService service) {
        this.service = service;
    }

    @Override
    public Future<ApiResponseTransactionMonthMethod> findMonthlyPaymentMethods(FindYearTransactionStatus req) {
        return service.findMonthlyPaymentMethods(req.getYear())
                .map(list -> ApiResponseTransactionMonthMethod.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly payment methods fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toMonthMethodResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionYearMethod> findYearlyPaymentMethods(FindYearTransactionStatus req) {
        return service.findYearlyPaymentMethods(req.getYear())
                .map(list -> ApiResponseTransactionYearMethod.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly payment methods fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toYearMethodResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionMonthMethod> findMonthlyPaymentMethodsByCardNumber(FindByYearCardNumberTransactionRequest req) {
        return service.findMonthlyPaymentMethodsByCardNumber(req.getCardNumber(), req.getYear())
                .map(list -> ApiResponseTransactionMonthMethod.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly payment methods by card fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toMonthMethodResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionYearMethod> findYearlyPaymentMethodsByCardNumber(FindByYearCardNumberTransactionRequest req) {
        return service.findYearlyPaymentMethodsByCardNumber(req.getCardNumber(), req.getYear())
                .map(list -> ApiResponseTransactionYearMethod.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly payment methods by card fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toYearMethodResponse).toList())
                        .build());
    }
}
