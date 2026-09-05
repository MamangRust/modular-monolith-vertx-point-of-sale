package io.example.category.service;

import io.example.category.domain.requests.MonthPriceMerchant;
import io.example.category.domain.requests.MonthTotalPriceMerchant;
import io.example.category.domain.requests.YearPriceMerchant;
import io.example.category.domain.requests.YearTotalPriceMerchant;
import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.vertx.core.Future;

import java.util.List;

public interface CategoryStatsByMerchantService {
    Future<List<CategoriesMonthPriceResponse>> getMonthlyCategoryByMerchant(MonthPriceMerchant req);

    Future<List<CategoriesYearPriceResponse>> getYearlyCategoryByMerchant(YearPriceMerchant req);

    Future<List<CategoriesMonthlyTotalPriceResponse>> getMonthlyTotalPriceByMerchant(MonthTotalPriceMerchant req);

    Future<List<CategoriesYearlyTotalPriceResponse>> getYearlyTotalPriceByMerchant(YearTotalPriceMerchant req);
}
