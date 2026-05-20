package io.example.category.service;

import io.example.category.domain.requests.MonthTotalPrice;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.vertx.core.Future;

import java.util.List;

public interface CategoryStatsService {
    Future<List<CategoryMonthTotalPrice>> getMonthlyTotalPrice(MonthTotalPrice req);
    Future<List<CategoryYearTotalPrice>> getYearlyTotalPrice(int year);
    Future<List<CategoryMonthPrice>> getMonthlyCategory(int year);
    Future<List<CategoryYearPrice>> getYearlyCategory(int year);
}
