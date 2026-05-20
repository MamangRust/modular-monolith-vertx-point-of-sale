package io.example.order.repository;

import java.util.List;
import io.example.order.domain.requests.MonthTotalRevenue;
import io.example.order.model.OrderMonthTotalRevenue;
import io.example.order.model.OrderYearTotalRevenue;
import io.example.order.model.OrderMonth;
import io.example.order.model.OrderYear;
import io.vertx.core.Future;

public interface OrderStatsRepository {
    Future<List<OrderMonthTotalRevenue>> getMonthlyTotalRevenue(MonthTotalRevenue req);
    Future<List<OrderYearTotalRevenue>> getYearlyTotalRevenue(int year);
    Future<List<OrderMonth>> getMonthlyOrder(int year);
    Future<List<OrderYear>> getYearlyOrder(int year);
}
