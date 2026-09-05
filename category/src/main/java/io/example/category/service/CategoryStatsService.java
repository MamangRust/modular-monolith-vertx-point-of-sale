package io.example.category.service;

import java.util.List;

import io.example.category.domain.requests.MonthTotalPrice;
import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.vertx.core.Future;

public interface CategoryStatsService {
    Future<List<CategoriesMonthlyTotalPriceResponse>> getMonthlyTotalPrice(MonthTotalPrice req);

    Future<List<CategoriesYearlyTotalPriceResponse>> getYearlyTotalPrice(int year);

    Future<List<CategoriesMonthPriceResponse>> getMonthlyCategory(int year);

    Future<List<CategoriesYearPriceResponse>> getYearlyCategory(int year);
}
