package io.example.cashier.handler;

import io.example.cashier.domain.requests.cashier.FindAllCashierMerchant;
import io.example.cashier.domain.requests.cashier.FindAllCashiers;
import io.example.cashier.domain.requests.cashier.MonthCashierIdRequest;
import io.example.cashier.domain.requests.cashier.MonthCashierMerchantRequest;
import io.example.cashier.domain.requests.cashier.MonthTotalSales;
import io.example.cashier.domain.requests.cashier.MonthTotalSalesCashier;
import io.example.cashier.domain.requests.cashier.MonthTotalSalesMerchant;
import io.example.cashier.domain.requests.cashier.YearCashierIdRequest;
import io.example.cashier.domain.requests.cashier.YearCashierMerchantRequest;
import io.example.cashier.domain.requests.cashier.YearTotalSalesCashier;
import io.example.cashier.domain.requests.cashier.YearTotalSalesMerchant;
import io.example.cashier.service.CashierQueryService;
import io.example.cashier.service.CashierStatsByIdService;
import io.example.cashier.service.CashierStatsByMerchant;
import io.example.cashier.service.CashierStatsService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.cashier.Cashier.ApiResponseCashier;
import pb.cashier.Cashier.ApiResponseCashierMonthSales;
import pb.cashier.Cashier.ApiResponseCashierYearSales;
import pb.cashier.Cashier.FindAllCashierRequest;
import pb.cashier.Cashier.FindByIdCashierRequest;
import pb.cashier.Cashier.FindByMerchantCashierRequest;
import pb.cashier.Cashier.FindYearCashier;
import pb.cashier.Cashier.FindYearCashierById;
import pb.cashier.Cashier.FindYearCashierByMerchant;
import pb.cashier.Cashier.FindYearMonthTotalSales;
import pb.cashier.Cashier.FindYearMonthTotalSalesById;
import pb.cashier.Cashier.FindYearMonthTotalSalesByMerchant;
import pb.cashier.Cashier.FindYearTotalSales;
import pb.cashier.Cashier.FindYearTotalSalesById;
import pb.cashier.Cashier.FindYearTotalSalesByMerchant;
import pb.cashier.CashierQuery.ApiResponseCashierMonthlyTotalSales;
import pb.cashier.CashierQuery.ApiResponseCashierYearlyTotalSales;
import pb.cashier.CashierQuery.ApiResponsePaginationCashier;
import pb.cashier.CashierQuery.ApiResponsePaginationCashierDeleteAt;

@RequiredArgsConstructor
public class CashierQueryHandler implements pb.cashier.VertxCashierServiceGrpcServer.CashierServiceApi {
        private final CashierStatsService statsService;
        private final CashierStatsByIdService statsByIdService;
        private final CashierStatsByMerchant statsByMerchantService;
        private final CashierQueryService queryService;

        // --- Helper Methods (Pattern User) ---

        private FindAllCashiers toDomainReq(FindAllCashierRequest req) {
                return FindAllCashiers.builder()
                                .search(req.getSearch())
                                .page(req.getPage() > 0 ? req.getPage() : 1)
                                .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
                                .build();
        }

        private FindAllCashierMerchant toDomainMerchantReq(FindByMerchantCashierRequest req) {
                return FindAllCashierMerchant.builder()
                                .merchantId(req.getMerchantId())
                                .search(req.getSearch())
                                .page(req.getPage() > 0 ? req.getPage() : 1)
                                .pageSize(req.getPageSize() > 0 ? req.getPageSize() : 10)
                                .build();
        }

        private pb.common.PaginationMeta toMeta(int totalRecords, int page, int pageSize) {
                int currentPage = page > 0 ? page : 1;
                int size = pageSize > 0 ? pageSize : 10;
                int totalPages = size > 0 ? (int) Math.ceil((double) totalRecords / size) : 0;
                return pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(currentPage)
                                .setPageSize(size)
                                .setTotalPages(totalPages)
                                .setTotalRecords(totalRecords)
                                .build();
        }

        @Override
        public Future<ApiResponseCashierMonthlyTotalSales> findMonthlyTotalSales(FindYearMonthTotalSales request) {
                MonthTotalSales req = MonthTotalSales.builder()
                                .year(request.getYear())
                                .month(request.getMonth())
                                .build();

                return statsService.getMonthlyTotalSales(req)
                                .map(sales -> ApiResponseCashierMonthlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total sales fetched successfully")
                                                .addAllData(sales.stream()
                                                                .map(ProtoConverter::toMonthTotalSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierYearlyTotalSales> findYearlyTotalSales(FindYearTotalSales request) {
                return statsService.getYearlyTotalSales(request.getYear())
                                .map(sales -> ApiResponseCashierYearlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total sales fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearTotalSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierMonthlyTotalSales> findMonthlyTotalSalesById(
                        FindYearMonthTotalSalesById request) {
                MonthTotalSalesCashier req = MonthTotalSalesCashier.builder()
                                .year(request.getYear())
                                .month(request.getMonth())
                                .cashierId(request.getCashierId())
                                .build();

                return statsByIdService
                                .getMonthlyTotalSalesById(req)
                                .map(sales -> ApiResponseCashierMonthlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total sales by ID fetched successfully")
                                                .addAllData(sales.stream()
                                                                .map(ProtoConverter::toMonthTotalSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierYearlyTotalSales> findYearlyTotalSalesById(FindYearTotalSalesById request) {
                YearTotalSalesCashier req = YearTotalSalesCashier.builder()
                                .year(request.getYear())
                                .cashierId(request.getCashierId())
                                .build();

                return statsByIdService.getYearlyTotalSalesById(req)
                                .map(sales -> ApiResponseCashierYearlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total sales by ID fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearTotalSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierMonthlyTotalSales> findMonthlyTotalSalesByMerchant(
                        FindYearMonthTotalSalesByMerchant request) {
                MonthTotalSalesMerchant req = MonthTotalSalesMerchant.builder()
                                .year(request.getYear())
                                .month(request.getMonth())
                                .merchantId(request.getMerchantId())
                                .build();

                return statsByMerchantService
                                .getMonthlyTotalSalesByMerchant(req)
                                .map(sales -> ApiResponseCashierMonthlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total sales by merchant fetched successfully")
                                                .addAllData(sales.stream()
                                                                .map(ProtoConverter::toMonthTotalSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierYearlyTotalSales> findYearlyTotalSalesByMerchant(
                        FindYearTotalSalesByMerchant request) {
                YearTotalSalesMerchant req = YearTotalSalesMerchant.builder()
                                .year(request.getYear())
                                .merchantId(request.getMerchantId())
                                .build();

                return statsByMerchantService
                                .getYearlyTotalSalesByMerchant(req)
                                .map(sales -> ApiResponseCashierYearlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total sales by merchant fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearTotalSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationCashier> findAll(FindAllCashierRequest request) {
                var domainReq = toDomainReq(request);
                return queryService.getCashiers(domainReq)
                                .map(paged -> ApiResponsePaginationCashier.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashiers fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCashierResponse).toList())
                                                .setPagination(toMeta(paged.getTotalRecords(), domainReq.getPage(),
                                                                domainReq.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashier> findById(FindByIdCashierRequest request) {
                return queryService.getCashierById((long) request.getId())
                                .map(cashier -> ApiResponseCashier.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier fetched successfully")
                                                .setData(ProtoConverter.toCashierResponse(cashier))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierMonthSales> findMonthSales(FindYearCashier request) {
                return statsService.getMonthlyCashier(request.getYear())
                                .map(sales -> ApiResponseCashierMonthSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly cashier sales fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toMonthSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierYearSales> findYearSales(FindYearCashier request) {
                return statsService.getYearlyCashier(request.getYear())
                                .map(sales -> ApiResponseCashierYearSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly cashier sales fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierMonthSales> findMonthSalesByMerchant(FindYearCashierByMerchant request) {
                MonthCashierMerchantRequest req = MonthCashierMerchantRequest.builder()
                                .year(request.getYear())
                                .merchantId(request.getMerchantId())
                                .build();

                return statsByMerchantService
                                .getMonthlyCashierByMerchant(req)
                                .map(sales -> ApiResponseCashierMonthSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly cashier sales by merchant fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toMonthSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierYearSales> findYearSalesByMerchant(FindYearCashierByMerchant request) {
                YearCashierMerchantRequest req = YearCashierMerchantRequest.builder()
                                .year(request.getYear())
                                .merchantId(request.getMerchantId())
                                .build();

                return statsByMerchantService
                                .getYearlyCashierByMerchant(req)
                                .map(sales -> ApiResponseCashierYearSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly cashier sales by merchant fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierMonthSales> findMonthSalesById(FindYearCashierById request) {
                MonthCashierIdRequest req = MonthCashierIdRequest.builder()
                                .year(request.getYear())
                                .cashierId(request.getCashierId())
                                .build();

                return statsByIdService.getMonthlyCashierById(req)
                                .map(sales -> ApiResponseCashierMonthSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly cashier sales by ID fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toMonthSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierYearSales> findYearSalesById(FindYearCashierById request) {
                YearCashierIdRequest req = YearCashierIdRequest.builder()
                                .year(request.getYear())
                                .cashierId(request.getCashierId())
                                .build();

                return statsByIdService.getYearlyCashierById(req)
                                .map(sales -> ApiResponseCashierYearSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly cashier sales by ID fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearSalesResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationCashierDeleteAt> findByActive(FindAllCashierRequest request) {
                var domainReq = toDomainReq(request);
                return queryService.getCashiersActive(domainReq)
                                .map(paged -> ApiResponsePaginationCashierDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Active cashiers fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCashierResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(paged.getTotalRecords(), domainReq.getPage(),
                                                                domainReq.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationCashierDeleteAt> findByTrashed(FindAllCashierRequest request) {
                var domainReq = toDomainReq(request);
                return queryService.getCashiersTrashed(domainReq)
                                .map(paged -> ApiResponsePaginationCashierDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Trashed cashiers fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCashierResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(paged.getTotalRecords(), domainReq.getPage(),
                                                                domainReq.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationCashier> findByMerchant(FindByMerchantCashierRequest request) {
                var domainReq = toDomainMerchantReq(request);
                return queryService.getCashiersByMerchant(domainReq)
                                .map(paged -> ApiResponsePaginationCashier.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashiers by merchant fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCashierResponse).toList())
                                                .setPagination(toMeta(paged.getTotalRecords(), domainReq.getPage(),
                                                                domainReq.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}