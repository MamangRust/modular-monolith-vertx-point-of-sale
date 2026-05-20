package io.example.category.handler;

import io.example.category.domain.requests.*;
import io.example.category.service.*;
import io.vertx.core.Future;
import pb.category.Category.*;
import pb.category.CategoryQuery.*;

public class CategoryQueryHandler implements pb.category.VertxCategoryServiceGrpcServer.CategoryServiceApi {
    private final CategoryStatsService statsService;
    private final CategoryStatsByIdService statsByIdService;
    private final CategoryStatsByMerchantService statsByMerchantService;
    private final CategoryQueryService queryService;

    public CategoryQueryHandler(CategoryStatsService statsService,
                                CategoryStatsByIdService statsByIdService,
                                CategoryStatsByMerchantService statsByMerchantService,
                                CategoryQueryService queryService) {
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
    public Future<ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPrices(FindYearMonthTotalPrices request) {
        MonthTotalPrice req = new MonthTotalPrice();
        req.setYear(request.getYear());
        req.setMonth(request.getMonth());

        return statsService.getMonthlyTotalPrice(req)
                .map(prices -> ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly total price fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoriesMonthlyTotalPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPrices(FindYearTotalPrices request) {
        return statsService.getYearlyTotalPrice(request.getYear())
                .map(prices -> ApiResponseCategoryYearlyTotalPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly total price fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoriesYearlyTotalPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPricesById(FindYearMonthTotalPriceById request) {
        MonthTotalPriceCategory req = new MonthTotalPriceCategory();
        req.setCategoryId(request.getCategoryId());
        req.setYear(request.getYear());
        req.setMonth(request.getMonth());

        return statsByIdService.getMonthlyTotalPriceById(req)
                .map(prices -> ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly total price by ID fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoriesMonthlyTotalPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPricesById(FindYearTotalPriceById request) {
        YearTotalPriceCategory req = new YearTotalPriceCategory();
        req.setCategoryId(request.getCategoryId());
        req.setYear(request.getYear());

        return statsByIdService.getYearlyTotalPriceById(req)
                .map(prices -> ApiResponseCategoryYearlyTotalPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly total price by ID fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoriesYearlyTotalPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryMonthlyTotalPrice> findMonthlyTotalPricesByMerchant(FindYearMonthTotalPriceByMerchant request) {
        MonthTotalPriceMerchant req = new MonthTotalPriceMerchant();
        req.setMerchantId(request.getMerchantId());
        req.setYear(request.getYear());
        req.setMonth(request.getMonth());

        return statsByMerchantService.getMonthlyTotalPriceByMerchant(req)
                .map(prices -> ApiResponseCategoryMonthlyTotalPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly total price by merchant fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoriesMonthlyTotalPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryYearlyTotalPrice> findYearlyTotalPricesByMerchant(FindYearTotalPriceByMerchant request) {
        YearTotalPriceMerchant req = new YearTotalPriceMerchant();
        req.setMerchantId(request.getMerchantId());
        req.setYear(request.getYear());

        return statsByMerchantService.getYearlyTotalPriceByMerchant(req)
                .map(prices -> ApiResponseCategoryYearlyTotalPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly total price by merchant fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoriesYearlyTotalPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponsePaginationCategory> findAll(FindAllCategoryRequest request) {
        FindAllCategory req = new FindAllCategory();
        req.setSearch(request.getSearch());
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());

        return queryService.getCategories(req)
                .map(paged -> ApiResponsePaginationCategory.newBuilder()
                        .setStatus("success")
                        .setMessage("Categories fetched successfully")
                        .addAllData(paged.getData().stream().map(ProtoConverter::toCategoryResponse).toList())
                        .setPagination(toMeta(request.getPage(), request.getPageSize(), paged.getTotalRecords()))
                        .build());
    }

    @Override
    public Future<ApiResponseCategory> findById(FindByIdCategoryRequest request) {
        return queryService.getCategoryById((long) request.getId())
                .map(category -> ApiResponseCategory.newBuilder()
                        .setStatus("success")
                        .setMessage("Category fetched successfully")
                        .setData(ProtoConverter.toCategoryResponse(category))
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryMonthPrice> findMonthPrice(FindYearCategory request) {
        return statsService.getMonthlyCategory(request.getYear())
                .map(prices -> ApiResponseCategoryMonthPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly category prices fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoryMonthPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryYearPrice> findYearPrice(FindYearCategory request) {
        return statsService.getYearlyCategory(request.getYear())
                .map(prices -> ApiResponseCategoryYearPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly category prices fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoryYearPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryMonthPrice> findMonthPriceByMerchant(FindYearCategoryByMerchant request) {
        MonthPriceMerchant req = new MonthPriceMerchant();
        req.setMerchantId(request.getMerchantId());
        req.setYear(request.getYear());

        return statsByMerchantService.getMonthlyCategoryByMerchant(req)
                .map(prices -> ApiResponseCategoryMonthPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly category prices by merchant fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoryMonthPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryYearPrice> findYearPriceByMerchant(FindYearCategoryByMerchant request) {
        YearPriceMerchant req = new YearPriceMerchant();
        req.setMerchantId(request.getMerchantId());
        req.setYear(request.getYear());

        return statsByMerchantService.getYearlyCategoryByMerchant(req)
                .map(prices -> ApiResponseCategoryYearPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly category prices by merchant fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoryYearPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryMonthPrice> findMonthPriceById(FindYearCategoryById request) {
        YearPriceId req = new YearPriceId();
        req.setCategoryId(request.getCategoryId());
        req.setYear(request.getYear());

        return statsByIdService.getMonthlyCategoryById(req)
                .map(prices -> ApiResponseCategoryMonthPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Monthly category prices by ID fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoryMonthPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponseCategoryYearPrice> findYearPriceById(FindYearCategoryById request) {
        YearPriceId req = new YearPriceId();
        req.setCategoryId(request.getCategoryId());
        req.setYear(request.getYear());

        return statsByIdService.getYearlyCategoryById(req)
                .map(prices -> ApiResponseCategoryYearPrice.newBuilder()
                        .setStatus("success")
                        .setMessage("Yearly category prices by ID fetched successfully")
                        .addAllData(prices.stream().map(ProtoConverter::toCategoryYearPriceResponse).toList())
                        .build());
    }

    @Override
    public Future<ApiResponsePaginationCategoryDeleteAt> findByActive(FindAllCategoryRequest request) {
        FindAllCategory req = new FindAllCategory();
        req.setSearch(request.getSearch());
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());

        return queryService.getCategoriesActive(req)
                .map(paged -> ApiResponsePaginationCategoryDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Active categories fetched successfully")
                        .addAllData(paged.getData().stream().map(ProtoConverter::toCategoryResponseDeleteAt).toList())
                        .setPagination(toMeta(request.getPage(), request.getPageSize(), paged.getTotalRecords()))
                        .build());
    }

    @Override
    public Future<ApiResponsePaginationCategoryDeleteAt> findByTrashed(FindAllCategoryRequest request) {
        FindAllCategory req = new FindAllCategory();
        req.setSearch(request.getSearch());
        req.setPage(request.getPage());
        req.setPageSize(request.getPageSize());

        return queryService.getTrashedCategories(req)
                .map(paged -> ApiResponsePaginationCategoryDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Trashed categories fetched successfully")
                        .addAllData(paged.getData().stream().map(ProtoConverter::toCategoryResponseDeleteAt).toList())
                        .setPagination(toMeta(request.getPage(), request.getPageSize(), paged.getTotalRecords()))
                        .build());
    }
}
