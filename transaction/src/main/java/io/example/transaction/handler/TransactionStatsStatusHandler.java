package io.example.transaction.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transaction.domain.requests.transactions.MonthAmountTransactionMerchant;
import io.example.transaction.domain.requests.transactions.MonthAmountTransactionRequest;
import io.example.transaction.domain.requests.transactions.YearAmountTransactionMerchant;
import io.example.transaction.service.TransactionStatsService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transaction.stats.ApiResponseTransactionMonthStatusFailed;
import pb.transaction.stats.ApiResponseTransactionMonthStatusSuccess;
import pb.transaction.stats.ApiResponseTransactionYearStatusFailed;
import pb.transaction.stats.ApiResponseTransactionYearStatusSuccess;
import pb.transaction.stats.FindMonthlyTransactionStatus;
import pb.transaction.stats.FindMonthlyTransactionStatusByMerchant;
import pb.transaction.stats.FindYearlyTransactionStatus;
import pb.transaction.stats.FindYearlyTransactionStatusByMerchant;

@RequiredArgsConstructor
public class TransactionStatsStatusHandler
                implements
                pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcServer.TransactionStatsStatusServiceApi {

        private final TransactionStatsService service;

        @Override
        public Future<ApiResponseTransactionMonthStatusSuccess> findMonthStatusSuccess(
                        FindMonthlyTransactionStatus req) {
                MonthAmountTransactionRequest domainReq = MonthAmountTransactionRequest.builder()
                                .year(req.getYear())
                                .month(req.getMonth())
                                .build();

                return service.findMonthlyTransactionStatusSuccess(domainReq)
                                .map(list -> ApiResponseTransactionMonthStatusSuccess.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly successful transactions status fetched successfully")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toMonthStatusSuccessResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionYearStatusSuccess> findYearStatusSuccess(FindYearlyTransactionStatus req) {
                return service.findYearlyTransactionStatusSuccess(req.getYear())
                                .map(list -> ApiResponseTransactionYearStatusSuccess.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly successful transactions status fetched successfully")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toYearStatusSuccessResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionMonthStatusFailed> findMonthStatusFailed(FindMonthlyTransactionStatus req) {
                MonthAmountTransactionRequest domainReq = MonthAmountTransactionRequest.builder()
                                .year(req.getYear())
                                .month(req.getMonth())
                                .build();

                return service.findMonthlyTransactionStatusFailed(domainReq)
                                .map(list -> ApiResponseTransactionMonthStatusFailed.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly failed transactions status fetched successfully")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toMonthStatusFailedResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionYearStatusFailed> findYearStatusFailed(FindYearlyTransactionStatus req) {
                return service.findYearlyTransactionStatusFailed(req.getYear())
                                .map(list -> ApiResponseTransactionYearStatusFailed.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly failed transactions status fetched successfully")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toYearStatusFailedResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionMonthStatusSuccess> findMonthStatusSuccessByMerchant(
                        FindMonthlyTransactionStatusByMerchant req) {
                MonthAmountTransactionMerchant domainReq = MonthAmountTransactionMerchant.builder()
                                .year(req.getYear())
                                .month(req.getMonth())
                                .merchantId(req.getMerchantId())
                                .build();

                return service.findMonthlyTransactionStatusSuccessByMerchant(domainReq)
                                .map(list -> ApiResponseTransactionMonthStatusSuccess.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly successful transactions by merchant fetched successfully")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toMonthStatusSuccessResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionYearStatusSuccess> findYearStatusSuccessByMerchant(
                        FindYearlyTransactionStatusByMerchant req) {
                YearAmountTransactionMerchant domainReq = YearAmountTransactionMerchant.builder()
                                .year(req.getYear())
                                .merchantId(req.getMerchantId())
                                .build();

                return service.findYearlyTransactionStatusSuccessByMerchant(domainReq)
                                .map(list -> ApiResponseTransactionYearStatusSuccess.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly successful transactions by merchant fetched successfully")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toYearStatusSuccessResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionMonthStatusFailed> findMonthStatusFailedByMerchant(
                        FindMonthlyTransactionStatusByMerchant req) {
                MonthAmountTransactionMerchant domainReq = MonthAmountTransactionMerchant.builder()
                                .year(req.getYear())
                                .month(req.getMonth())
                                .merchantId(req.getMerchantId())
                                .build();

                return service.findMonthlyTransactionStatusFailedByMerchant(domainReq)
                                .map(list -> ApiResponseTransactionMonthStatusFailed.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly failed transactions by merchant fetched successfully")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toMonthStatusFailedResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionYearStatusFailed> findYearStatusFailedByMerchant(
                        FindYearlyTransactionStatusByMerchant req) {
                YearAmountTransactionMerchant domainReq = YearAmountTransactionMerchant.builder()
                                .merchantId(req.getMerchantId())
                                .year(req.getYear())
                                .build();

                return service.findYearlyTransactionStatusFailedByMerchant(domainReq)
                                .map(list -> ApiResponseTransactionYearStatusFailed.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly failed transactions by merchant fetched successfully")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toYearStatusFailedResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}