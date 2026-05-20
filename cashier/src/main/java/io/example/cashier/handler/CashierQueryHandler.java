package io.example.cashier.handler;

import io.example.cashier.service.CashierQueryService;
import io.example.cashier.service.CashierStatsByIdService;
import io.example.cashier.service.CashierStatsByMerchant;
import io.example.cashier.service.CashierStatsService;
import io.vertx.core.Future;
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

public class CashierQueryHandler implements pb.cashier.VertxCashierServiceGrpcServer.CashierServiceApi {
        private final CashierStatsService statsService;
        private final CashierStatsByIdService statsByIdService;
        private final CashierStatsByMerchant statsByMerchantService;
        private final CashierQueryService queryService;

        public CashierQueryHandler(CashierStatsService statsService,
                        CashierStatsByIdService statsByIdService,
                        CashierStatsByMerchant statsByMerchantService,
                        CashierQueryService queryService) {
                this.statsService = statsService;
                this.statsByIdService = statsByIdService;
                this.statsByMerchantService = statsByMerchantService;
                this.queryService = queryService;
        }

        private pb.common.PaginationMeta toMeta(int page, int pageSize, int totalRecords) {
                int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalRecords / pageSize) : 0;
                return pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(page)
                                .setPageSize(pageSize)
                                .setTotalPages(totalPages)
                                .setTotalRecords(totalRecords)
                                .build();
        }

        @Override
        public Future<ApiResponseCashierMonthlyTotalSales> findMonthlyTotalSales(FindYearMonthTotalSales request) {
                return statsService.getMonthlyTotalSales(request.getYear(), request.getMonth())
                                .map(sales -> ApiResponseCashierMonthlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total sales fetched successfully")
                                                .addAllData(sales.stream()
                                                                .map(ProtoConverter::toMonthTotalSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierYearlyTotalSales> findYearlyTotalSales(FindYearTotalSales request) {
                return statsService.getYearlyTotalSales(request.getYear())
                                .map(sales -> ApiResponseCashierYearlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total sales fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearTotalSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierMonthlyTotalSales> findMonthlyTotalSalesById(
                        FindYearMonthTotalSalesById request) {
                return statsByIdService
                                .getMonthlyTotalSalesById(request.getYear(), request.getMonth(),
                                                (long) request.getCashierId())
                                .map(sales -> ApiResponseCashierMonthlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total sales by ID fetched successfully")
                                                .addAllData(sales.stream()
                                                                .map(ProtoConverter::toMonthTotalSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierYearlyTotalSales> findYearlyTotalSalesById(FindYearTotalSalesById request) {
                return statsByIdService.getYearlyTotalSalesById(request.getYear(), (long) request.getCashierId())
                                .map(sales -> ApiResponseCashierYearlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total sales by ID fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearTotalSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierMonthlyTotalSales> findMonthlyTotalSalesByMerchant(
                        FindYearMonthTotalSalesByMerchant request) {
                return statsByMerchantService
                                .getMonthlyTotalSalesByMerchant(request.getYear(), request.getMonth(),
                                                (long) request.getMerchantId())
                                .map(sales -> ApiResponseCashierMonthlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total sales by merchant fetched successfully")
                                                .addAllData(sales.stream()
                                                                .map(ProtoConverter::toMonthTotalSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierYearlyTotalSales> findYearlyTotalSalesByMerchant(
                        FindYearTotalSalesByMerchant request) {
                return statsByMerchantService
                                .getYearlyTotalSalesByMerchant(request.getYear(), (long) request.getMerchantId())
                                .map(sales -> ApiResponseCashierYearlyTotalSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total sales by merchant fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearTotalSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponsePaginationCashier> findAll(FindAllCashierRequest request) {
                return queryService.getCashiers(request.getSearch(), request.getPage(), request.getPageSize())
                                .map(paged -> ApiResponsePaginationCashier.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashiers fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCashierResponse).toList())
                                                .setPagination(toMeta(request.getPage(), request.getPageSize(),
                                                                paged.getTotalRecords()))
                                                .build());
        }

        @Override
        public Future<ApiResponseCashier> findById(FindByIdCashierRequest request) {
                return queryService.getCashierById((long) request.getId())
                                .map(cashier -> ApiResponseCashier.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier fetched successfully")
                                                .setData(ProtoConverter.toCashierResponse(cashier))
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierMonthSales> findMonthSales(FindYearCashier request) {
                return statsService.getMonthlyCashier(request.getYear())
                                .map(sales -> ApiResponseCashierMonthSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly cashier sales fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toMonthSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierYearSales> findYearSales(FindYearCashier request) {
                return statsService.getYearlyCashier(request.getYear())
                                .map(sales -> ApiResponseCashierYearSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly cashier sales fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierMonthSales> findMonthSalesByMerchant(FindYearCashierByMerchant request) {
                return statsByMerchantService
                                .getMonthlyCashierByMerchant(request.getYear(), (long) request.getMerchantId())
                                .map(sales -> ApiResponseCashierMonthSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly cashier sales by merchant fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toMonthSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierYearSales> findYearSalesByMerchant(FindYearCashierByMerchant request) {
                return statsByMerchantService
                                .getYearlyCashierByMerchant(request.getYear(), (long) request.getMerchantId())
                                .map(sales -> ApiResponseCashierYearSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly cashier sales by merchant fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierMonthSales> findMonthSalesById(FindYearCashierById request) {
                return statsByIdService.getMonthlyCashierById(request.getYear(), (long) request.getCashierId())
                                .map(sales -> ApiResponseCashierMonthSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly cashier sales by ID fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toMonthSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierYearSales> findYearSalesById(FindYearCashierById request) {
                return statsByIdService.getYearlyCashierById(request.getYear(), (long) request.getCashierId())
                                .map(sales -> ApiResponseCashierYearSales.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly cashier sales by ID fetched successfully")
                                                .addAllData(sales.stream().map(ProtoConverter::toYearSalesResponse)
                                                                .toList())
                                                .build());
        }

        @Override
        public Future<ApiResponsePaginationCashierDeleteAt> findByActive(FindAllCashierRequest request) {
                return queryService.getCashiersActive(request.getSearch(), request.getPage(), request.getPageSize())
                                .map(paged -> ApiResponsePaginationCashierDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Active cashiers fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCashierResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(request.getPage(), request.getPageSize(),
                                                                paged.getTotalRecords()))
                                                .build());
        }

        @Override
        public Future<ApiResponsePaginationCashierDeleteAt> findByTrashed(FindAllCashierRequest request) {
                return queryService.getCashiersTrashed(request.getSearch(), request.getPage(), request.getPageSize())
                                .map(paged -> ApiResponsePaginationCashierDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Trashed cashiers fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCashierResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(request.getPage(), request.getPageSize(),
                                                                paged.getTotalRecords()))
                                                .build());
        }

        @Override
        public Future<ApiResponsePaginationCashier> findByMerchant(FindByMerchantCashierRequest request) {
                return queryService
                                .getCashiersByMerchant((long) request.getMerchantId(), request.getSearch(),
                                                request.getPage(), request.getPageSize())
                                .map(paged -> ApiResponsePaginationCashier.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashiers by merchant fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCashierResponse).toList())
                                                .setPagination(toMeta(request.getPage(), request.getPageSize(),
                                                                paged.getTotalRecords()))
                                                .build());
        }
}
