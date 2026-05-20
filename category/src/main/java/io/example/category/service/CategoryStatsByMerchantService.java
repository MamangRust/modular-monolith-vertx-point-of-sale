package io.example.category.service;

import io.example.category.domain.requests.MonthPriceMerchant;
import io.example.category.domain.requests.MonthTotalPriceMerchant;
import io.example.category.domain.requests.YearPriceMerchant;
import io.example.category.domain.requests.YearTotalPriceMerchant;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.vertx.core.Future;

import java.util.List;

public interface CategoryStatsByMerchantService {
    Future<List<CategoryMonthTotalPrice>> getMonthlyTotalPriceByMerchant(MonthTotalPriceMerchant req);
    Future<List<CategoryYearTotalPrice>> getYearlyTotalPriceByMerchant(YearTotalPriceMerchant req);
    Future<List<CategoryMonthPrice>> getMonthlyCategoryByMerchant(MonthPriceMerchant req);
    Future<List<CategoryYearPrice>> getYearlyCategoryByMerchant(YearPriceMerchant req);
}
