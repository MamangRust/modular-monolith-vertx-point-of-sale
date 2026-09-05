package io.example.category.handler;

import io.example.category.domain.requests.FindAllCategory;
import io.example.category.domain.requests.MonthPriceMerchant;
import io.example.category.domain.requests.MonthTotalPrice;
import io.example.category.domain.requests.MonthTotalPriceCategory;
import io.example.category.domain.requests.MonthTotalPriceMerchant;
import io.example.category.domain.requests.YearPriceId;
import io.example.category.domain.requests.YearPriceMerchant;
import io.example.category.domain.requests.YearTotalPriceCategory;
import io.example.category.domain.requests.YearTotalPriceMerchant;
import io.example.category.service.CategoryQueryService;
import io.example.category.service.CategoryStatsByIdService;
import io.example.category.service.CategoryStatsByMerchantService;
import io.example.category.service.CategoryStatsService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.category.Category.ApiResponseCategory;
import pb.category.Category.ApiResponseCategoryMonthPrice;
import pb.category.Category.ApiResponseCategoryMonthlyTotalPrice;
import pb.category.Category.ApiResponseCategoryYearPrice;
import pb.category.Category.ApiResponseCategoryYearlyTotalPrice;
import pb.category.Category.FindAllCategoryRequest;
import pb.category.Category.FindByIdCategoryRequest;
import pb.category.Category.FindYearCategory;
import pb.category.Category.FindYearCategoryById;
import pb.category.Category.FindYearCategoryByMerchant;
import pb.category.Category.FindYearMonthTotalPriceById;
import pb.category.Category.FindYearMonthTotalPriceByMerchant;
import pb.category.Category.FindYearMonthTotalPrices;
import pb.category.Category.FindYearTotalPriceById;
import pb.category.Category.FindYearTotalPriceByMerchant;
import pb.category.Category.FindYearTotalPrices;
import pb.category.CategoryQuery.ApiResponsePaginationCategory;
import pb.category.CategoryQuery.ApiResponsePaginationCategoryDeleteAt;

@RequiredArgsConstructor
public class CategoryQueryHandler implements pb.category.VertxCategoryServiceGrpcServer.CategoryServiceApi {
        private final CategoryStatsService statsService;
        private final CategoryStatsByIdService statsByIdService;
        private final CategoryStatsByMerchantService statsByMerchantService;
        private final CategoryQueryService queryService;

        private FindAllCategory toDomainReq(FindAllCategoryRequest req) {
                return FindAllCategory.builder()
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

        // --- Stats Handlers ---

        @Override
        public Future<ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPrices(FindYearMonthTotalPrices request) {
                MonthTotalPrice req = MonthTotalPrice.builder()
                                .year(request.getYear())
                                .month(request.getMonth())
                                .build();

                return statsService.getMonthlyTotalPrice(req)
                                .map(prices -> ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total price fetched successfully")
                                                .addAllData(prices.stream().map(
                                                                ProtoConverter::toCategoriesMonthlyTotalPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPrices(FindYearTotalPrices request) {
                return statsService.getYearlyTotalPrice(request.getYear())
                                .map(prices -> ApiResponseCategoryYearlyTotalPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total price fetched successfully")
                                                .addAllData(prices.stream().map(
                                                                ProtoConverter::toCategoriesYearlyTotalPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPricesById(
                        FindYearMonthTotalPriceById request) {
                MonthTotalPriceCategory req = MonthTotalPriceCategory.builder()
                                .categoryId(request.getCategoryId())
                                .year(request.getYear())
                                .month(request.getMonth())
                                .build();

                return statsByIdService.getMonthlyTotalPriceById(req)
                                .map(prices -> ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total price by ID fetched successfully")
                                                .addAllData(prices.stream().map(
                                                                ProtoConverter::toCategoriesMonthlyTotalPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPricesById(FindYearTotalPriceById request) {
                YearTotalPriceCategory req = YearTotalPriceCategory.builder()
                                .categoryId(request.getCategoryId())
                                .year(request.getYear())
                                .build();

                return statsByIdService.getYearlyTotalPriceById(req)
                                .map(prices -> ApiResponseCategoryYearlyTotalPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total price by ID fetched successfully")
                                                .addAllData(prices.stream().map(
                                                                ProtoConverter::toCategoriesYearlyTotalPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPricesByMerchant(
                        FindYearMonthTotalPriceByMerchant request) {
                MonthTotalPriceMerchant req = MonthTotalPriceMerchant.builder()
                                .merchantId(request.getMerchantId())
                                .year(request.getYear())
                                .month(request.getMonth())
                                .build();

                return statsByMerchantService.getMonthlyTotalPriceByMerchant(req)
                                .map(prices -> ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total price by merchant fetched successfully")
                                                .addAllData(prices.stream().map(
                                                                ProtoConverter::toCategoriesMonthlyTotalPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPricesByMerchant(
                        FindYearTotalPriceByMerchant request) {
                YearTotalPriceMerchant req = YearTotalPriceMerchant.builder()
                                .merchantId(request.getMerchantId())
                                .year(request.getYear())
                                .build();

                return statsByMerchantService.getYearlyTotalPriceByMerchant(req)
                                .map(prices -> ApiResponseCategoryYearlyTotalPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total price by merchant fetched successfully")
                                                .addAllData(prices.stream().map(
                                                                ProtoConverter::toCategoriesYearlyTotalPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryMonthPrice> findMonthPrice(FindYearCategory request) {
                return statsService.getMonthlyCategory(request.getYear())
                                .map(prices -> ApiResponseCategoryMonthPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly category prices fetched successfully")
                                                .addAllData(prices.stream()
                                                                .map(ProtoConverter::toCategoryMonthPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryYearPrice> findYearPrice(FindYearCategory request) {
                return statsService.getYearlyCategory(request.getYear())
                                .map(prices -> ApiResponseCategoryYearPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly category prices fetched successfully")
                                                .addAllData(prices.stream()
                                                                .map(ProtoConverter::toCategoryYearPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryMonthPrice> findMonthPriceByMerchant(FindYearCategoryByMerchant request) {
                MonthPriceMerchant req = MonthPriceMerchant.builder()
                                .merchantId(request.getMerchantId())
                                .year(request.getYear())
                                .build();

                return statsByMerchantService.getMonthlyCategoryByMerchant(req)
                                .map(prices -> ApiResponseCategoryMonthPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly category prices by merchant fetched successfully")
                                                .addAllData(prices.stream()
                                                                .map(ProtoConverter::toCategoryMonthPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryYearPrice> findYearPriceByMerchant(FindYearCategoryByMerchant request) {
                YearPriceMerchant req = YearPriceMerchant.builder()
                                .merchantId(request.getMerchantId())
                                .year(request.getYear())
                                .build();

                return statsByMerchantService.getYearlyCategoryByMerchant(req)
                                .map(prices -> ApiResponseCategoryYearPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly category prices by merchant fetched successfully")
                                                .addAllData(prices.stream()
                                                                .map(ProtoConverter::toCategoryYearPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryMonthPrice> findMonthPriceById(FindYearCategoryById request) {
                YearPriceId req = YearPriceId.builder()
                                .categoryId(request.getCategoryId())
                                .year(request.getYear())
                                .build();

                return statsByIdService.getMonthlyCategoryById(req)
                                .map(prices -> ApiResponseCategoryMonthPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly category prices by ID fetched successfully")
                                                .addAllData(prices.stream()
                                                                .map(ProtoConverter::toCategoryMonthPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategoryYearPrice> findYearPriceById(FindYearCategoryById request) {
                YearPriceId req = YearPriceId.builder()
                                .categoryId(request.getCategoryId())
                                .year(request.getYear())
                                .build();

                return statsByIdService.getYearlyCategoryById(req)
                                .map(prices -> ApiResponseCategoryYearPrice.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly category prices by ID fetched successfully")
                                                .addAllData(prices.stream()
                                                                .map(ProtoConverter::toCategoryYearPriceResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        // --- Query Handlers ---

        @Override
        public Future<ApiResponsePaginationCategory> findAll(FindAllCategoryRequest request) {
                var domainReq = toDomainReq(request);
                return queryService.getCategories(domainReq)
                                .map(paged -> ApiResponsePaginationCategory.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Categories fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCategoryResponse).toList())
                                                .setPagination(toMeta(paged.getTotalRecords(), domainReq.getPage(),
                                                                domainReq.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCategory> findById(FindByIdCategoryRequest request) {
                return queryService.getCategoryById((long) request.getId())
                                .map(category -> ApiResponseCategory.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Category fetched successfully")
                                                .setData(ProtoConverter.toCategoryResponse(category))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationCategoryDeleteAt> findByActive(FindAllCategoryRequest request) {
                var domainReq = toDomainReq(request);
                return queryService.getCategoriesActive(domainReq)
                                .map(paged -> ApiResponsePaginationCategoryDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Active categories fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCategoryResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(paged.getTotalRecords(), domainReq.getPage(),
                                                                domainReq.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationCategoryDeleteAt> findByTrashed(FindAllCategoryRequest request) {
                var domainReq = toDomainReq(request);
                return queryService.getTrashedCategories(domainReq)
                                .map(paged -> ApiResponsePaginationCategoryDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Trashed categories fetched successfully")
                                                .addAllData(paged.getData().stream()
                                                                .map(ProtoConverter::toCategoryResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(paged.getTotalRecords(), domainReq.getPage(),
                                                                domainReq.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}