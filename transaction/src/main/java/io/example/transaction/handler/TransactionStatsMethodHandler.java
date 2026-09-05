package io.example.transaction.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transaction.domain.requests.transactions.MonthMethodTransactionMerchantRequest;
import io.example.transaction.domain.requests.transactions.MonthMethodTransactionRequest;
import io.example.transaction.domain.requests.transactions.YearMethodTransactionMerchantRequest;
import io.example.transaction.service.TransactionStatsService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transaction.stats.ApiResponseTransactionMonthMethod;
import pb.transaction.stats.ApiResponseTransactionYearMethod;
import pb.transaction.stats.MonthTransactionMethod;
import pb.transaction.stats.MonthTransactionMethodByMerchant;
import pb.transaction.stats.YearTransactionMethod;
import pb.transaction.stats.YearTransactionMethodByMerchant;

@RequiredArgsConstructor
public class TransactionStatsMethodHandler
                implements
                pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcServer.TransactionStatsMethodServiceApi {

        private final TransactionStatsService service;

        @Override
        public Future<ApiResponseTransactionMonthMethod> findMonthMethodSuccess(MonthTransactionMethod req) {
                MonthMethodTransactionRequest domainReq = MonthMethodTransactionRequest.builder()
                                .year(req.getYear())
                                .month(req.getMonth())
                                .build();

                return service.findMonthlyPaymentMethodsSuccess(domainReq)
                                .map(list -> ApiResponseTransactionMonthMethod.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly payment methods (success) fetched successfully")
                                                .addAllData(list.stream().map(ProtoConverter::toMonthMethodResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionYearMethod> findYearMethodSuccess(YearTransactionMethod req) {
                return service.findYearlyPaymentMethodsSuccess(req.getYear())
                                .map(list -> ApiResponseTransactionYearMethod.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly payment methods (success) fetched successfully")
                                                .addAllData(list.stream().map(ProtoConverter::toYearMethodResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionMonthMethod> findMonthMethodByMerchantSuccess(
                        MonthTransactionMethodByMerchant req) {
                MonthMethodTransactionMerchantRequest domainReq = MonthMethodTransactionMerchantRequest.builder()
                                .year(req.getYear())
                                .month(req.getMonth())
                                .merchantId(req.getMerchantId())
                                .build();

                return service.findMonthlyPaymentMethodsByMerchantSuccess(domainReq)
                                .map(list -> ApiResponseTransactionMonthMethod.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly payment methods by merchant (success) fetched successfully")
                                                .addAllData(list.stream().map(ProtoConverter::toMonthMethodResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionYearMethod> findYearMethodByMerchantSuccess(
                        YearTransactionMethodByMerchant req) {
                YearMethodTransactionMerchantRequest domainReq = YearMethodTransactionMerchantRequest.builder()
                                .year(req.getYear())
                                .merchantId(req.getMerchantId())
                                .build();

                return service.findYearlyPaymentMethodsByMerchantSuccess(domainReq)
                                .map(list -> ApiResponseTransactionYearMethod.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly payment methods by merchant (success) fetched successfully")
                                                .addAllData(list.stream().map(ProtoConverter::toYearMethodResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionMonthMethod> findMonthMethodFailed(MonthTransactionMethod req) {
                MonthMethodTransactionRequest domainReq = MonthMethodTransactionRequest.builder()
                                .year(req.getYear())
                                .month(req.getMonth())
                                .build();

                return service.findMonthlyPaymentMethodsFailed(domainReq)
                                .map(list -> ApiResponseTransactionMonthMethod.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly payment methods (failed) fetched successfully")
                                                .addAllData(list.stream().map(ProtoConverter::toMonthMethodResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionYearMethod> findYearMethodFailed(YearTransactionMethod req) {
                return service.findYearlyPaymentMethodsFailed(req.getYear())
                                .map(list -> ApiResponseTransactionYearMethod.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly payment methods (failed) fetched successfully")
                                                .addAllData(list.stream().map(ProtoConverter::toYearMethodResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionMonthMethod> findMonthMethodByMerchantFailed(
                        MonthTransactionMethodByMerchant req) {
                MonthMethodTransactionMerchantRequest domainReq = MonthMethodTransactionMerchantRequest.builder()
                                .year(req.getYear())
                                .month(req.getMonth())
                                .merchantId(req.getMerchantId())
                                .build();

                return service.findMonthlyPaymentMethodsByMerchantFailed(domainReq)
                                .map(list -> ApiResponseTransactionMonthMethod.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly payment methods by merchant (failed) fetched successfully")
                                                .addAllData(list.stream().map(ProtoConverter::toMonthMethodResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseTransactionYearMethod> findYearMethodByMerchantFailed(
                        YearTransactionMethodByMerchant req) {
                YearMethodTransactionMerchantRequest domainReq = YearMethodTransactionMerchantRequest.builder()
                                .year(req.getYear())
                                .merchantId(req.getMerchantId())
                                .build();

                return service.findYearlyPaymentMethodsByMerchantFailed(domainReq)
                                .map(list -> ApiResponseTransactionYearMethod.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly payment methods by merchant (failed) fetched successfully")
                                                .addAllData(list.stream().map(ProtoConverter::toYearMethodResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}