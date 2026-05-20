package io.example.category.service;

import io.example.category.domain.requests.MonthTotalPriceCategory;
import io.example.category.domain.requests.YearPriceId;
import io.example.category.domain.requests.YearTotalPriceCategory;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.vertx.core.Future;

import java.util.List;

public interface CategoryStatsByIdService {
    Future<List<CategoryMonthTotalPrice>> getMonthlyTotalPriceById(MonthTotalPriceCategory req);
    Future<List<CategoryYearTotalPrice>> getYearlyTotalPriceById(YearTotalPriceCategory req);
    Future<List<CategoryMonthPrice>> getMonthlyCategoryById(YearPriceId req);
    Future<List<CategoryYearPrice>> getYearlyCategoryById(YearPriceId req);
}
