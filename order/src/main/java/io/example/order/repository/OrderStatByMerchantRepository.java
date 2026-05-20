package io.example.order.repository;

import java.util.List;
import io.example.order.domain.requests.MonthTotalRevenueMerchant;
import io.example.order.domain.requests.YearTotalRevenueMerchant;
import io.example.order.domain.requests.MonthOrderMerchant;
import io.example.order.domain.requests.YearOrderMerchant;
import io.example.order.model.OrderMonthTotalRevenue;
import io.example.order.model.OrderYearTotalRevenue;
import io.example.order.model.OrderMonth;
import io.example.order.model.OrderYear;
import io.vertx.core.Future;

public interface OrderStatByMerchantRepository {
    Future<List<OrderMonthTotalRevenue>> getMonthlyTotalRevenueByMerchant(MonthTotalRevenueMerchant req);
    Future<List<OrderYearTotalRevenue>> getYearlyTotalRevenueByMerchant(YearTotalRevenueMerchant req);
    Future<List<OrderMonth>> getMonthlyOrderByMerchant(MonthOrderMerchant req);
    Future<List<OrderYear>> getYearlyOrderByMerchant(YearOrderMerchant req);
}
