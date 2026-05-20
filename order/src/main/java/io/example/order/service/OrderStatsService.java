package io.example.order.service;

import java.util.List;
import io.example.order.domain.requests.MonthTotalRevenue;
import io.example.order.model.OrderMonthTotalRevenue;
import io.example.order.model.OrderYearTotalRevenue;
import io.example.order.model.OrderMonth;
import io.example.order.model.OrderYear;
import io.vertx.core.Future;

public interface OrderStatsService {
    Future<List<OrderMonthTotalRevenue>> findMonthlyTotalRevenue(MonthTotalRevenue req);
    Future<List<OrderYearTotalRevenue>> findYearlyTotalRevenue(int year);
    Future<List<OrderMonth>> findMonthlyOrder(int year);
    Future<List<OrderYear>> findYearlyOrder(int year);
}
