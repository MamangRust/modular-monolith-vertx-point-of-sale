package io.example.order.service;

import java.util.List;

import io.example.order.domain.requests.MonthOrderMerchant;
import io.example.order.domain.requests.MonthTotalRevenueMerchant;
import io.example.order.domain.requests.YearOrderMerchant;
import io.example.order.domain.requests.YearTotalRevenueMerchant;
import io.example.order.domain.response.order.OrderMonthlyResponse;
import io.example.order.domain.response.order.OrderMonthlyTotalRevenueResponse;
import io.example.order.domain.response.order.OrderYearlyResponse;
import io.example.order.domain.response.order.OrderYearlyTotalRevenueResponse;
import io.vertx.core.Future;

public interface OrderStatByMerchantService {
    Future<List<OrderMonthlyTotalRevenueResponse>> findMonthlyTotalRevenueByMerchant(MonthTotalRevenueMerchant req);

    Future<List<OrderYearlyTotalRevenueResponse>> findYearlyTotalRevenueByMerchant(YearTotalRevenueMerchant req);

    Future<List<OrderMonthlyResponse>> findMonthlyOrderByMerchant(MonthOrderMerchant req);

    Future<List<OrderYearlyResponse>> findYearlyOrderByMerchant(YearOrderMerchant req);
}
