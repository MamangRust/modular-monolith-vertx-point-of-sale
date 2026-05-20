package io.example.transaction.handler;

import io.example.transaction.service.TransactionStatsService;
import io.vertx.core.Future;
import pb.transaction.Transaction.*;
import pb.transaction.stats.TransactionStatsStatus.*;

public class TransactionStatsStatusHandler implements pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcServer.TransactionStatsStatusServiceApi {
    private final TransactionStatsService service;

    public TransactionStatsStatusHandler(TransactionStatsService service) {
        this.service = service;
    }

    @Override
    public Future<ApiResponseTransactionMonthStatusSuccess> findMonthlyTransactionStatusSuccess(FindMonthlyTransactionStatus req) {
        return service.findMonthlyTransactionStatusSuccess(req.getYear(), req.getMonth())
                .map(list -> ApiResponseTransactionMonthStatusSuccess.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly successful transactions status fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toMonthStatusSuccessResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionYearStatusSuccess> findYearlyTransactionStatusSuccess(FindYearTransactionStatus req) {
        return service.findYearlyTransactionStatusSuccess(req.getYear())
                .map(list -> ApiResponseTransactionYearStatusSuccess.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly successful transactions status fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toYearStatusSuccessResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionMonthStatusFailed> findMonthlyTransactionStatusFailed(FindMonthlyTransactionStatus req) {
        return service.findMonthlyTransactionStatusFailed(req.getYear(), req.getMonth())
                .map(list -> ApiResponseTransactionMonthStatusFailed.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly failed transactions status fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toMonthStatusFailedResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionYearStatusFailed> findYearlyTransactionStatusFailed(FindYearTransactionStatus req) {
        return service.findYearlyTransactionStatusFailed(req.getYear())
                .map(list -> ApiResponseTransactionYearStatusFailed.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly failed transactions status fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toYearStatusFailedResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionMonthStatusSuccess> findMonthlyTransactionStatusSuccessByCardNumber(FindMonthlyTransactionStatusCardNumber req) {
        return service.findMonthlyTransactionStatusSuccessByCardNumber(req.getCardNumber(), req.getYear(), req.getMonth())
                .map(list -> ApiResponseTransactionMonthStatusSuccess.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly successful transactions status by card fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toMonthStatusSuccessResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionYearStatusSuccess> findYearlyTransactionStatusSuccessByCardNumber(FindYearTransactionStatusCardNumber req) {
        return service.findYearlyTransactionStatusSuccessByCardNumber(req.getCardNumber(), req.getYear())
                .map(list -> ApiResponseTransactionYearStatusSuccess.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly successful transactions status by card fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toYearStatusSuccessResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionMonthStatusFailed> findMonthlyTransactionStatusFailedByCardNumber(FindMonthlyTransactionStatusCardNumber req) {
        return service.findMonthlyTransactionStatusFailedByCardNumber(req.getCardNumber(), req.getYear(), req.getMonth())
                .map(list -> ApiResponseTransactionMonthStatusFailed.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly failed transactions status by card fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toMonthStatusFailedResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseTransactionYearStatusFailed> findYearlyTransactionStatusFailedByCardNumber(FindYearTransactionStatusCardNumber req) {
        return service.findYearlyTransactionStatusFailedByCardNumber(req.getCardNumber(), req.getYear())
                .map(list -> ApiResponseTransactionYearStatusFailed.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly failed transactions status by card fetched successfully")
                        .addAllData(list.stream().map(ProtoConverter::toYearStatusFailedResponse).toList())
                        .build());
    }
}
