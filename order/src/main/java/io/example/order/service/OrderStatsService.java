package io.example.order.service;

import java.util.List;

import io.example.order.domain.requests.MonthTotalRevenue;
import io.example.order.domain.response.order.OrderMonthlyResponse;
import io.example.order.domain.response.order.OrderMonthlyTotalRevenueResponse;
import io.example.order.domain.response.order.OrderYearlyResponse;
import io.example.order.domain.response.order.OrderYearlyTotalRevenueResponse;
import io.vertx.core.Future;

public interface OrderStatsService {
    Future<List<OrderMonthlyTotalRevenueResponse>> findMonthlyTotalRevenue(MonthTotalRevenue req);

    Future<List<OrderYearlyTotalRevenueResponse>> findYearlyTotalRevenue(int year);

    Future<List<OrderMonthlyResponse>> findMonthlyOrder(int year);

    Future<List<OrderYearlyResponse>> findYearlyOrder(int year);
}
