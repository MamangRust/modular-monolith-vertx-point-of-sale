package io.example.order.service;

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

public interface OrderStatByMerchantService {
    Future<List<OrderMonthTotalRevenue>> findMonthlyTotalRevenueByMerchant(MonthTotalRevenueMerchant req);
    Future<List<OrderYearTotalRevenue>> findYearlyTotalRevenueByMerchant(YearTotalRevenueMerchant req);
    Future<List<OrderMonth>> findMonthlyOrderByMerchant(MonthOrderMerchant req);
    Future<List<OrderYear>> findYearlyOrderByMerchant(YearOrderMerchant req);
}
