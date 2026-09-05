package io.example.category.service;

import java.util.List;

import io.example.category.domain.requests.MonthTotalPriceCategory;
import io.example.category.domain.requests.YearPriceId;
import io.example.category.domain.requests.YearTotalPriceCategory;
import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.vertx.core.Future;

public interface CategoryStatsByIdService {
    Future<List<CategoriesMonthPriceResponse>> getMonthlyCategoryById(YearPriceId req);

    Future<List<CategoriesYearPriceResponse>> getYearlyCategoryById(YearPriceId req);

    Future<List<CategoriesMonthlyTotalPriceResponse>> getMonthlyTotalPriceById(MonthTotalPriceCategory req);

    Future<List<CategoriesYearlyTotalPriceResponse>> getYearlyTotalPriceById(YearTotalPriceCategory req);
}
