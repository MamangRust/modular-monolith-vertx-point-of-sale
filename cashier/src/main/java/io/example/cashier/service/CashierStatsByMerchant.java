package io.example.cashier.service;

import java.util.List;

import io.example.cashier.domain.requests.cashier.MonthCashierMerchantRequest;
import io.example.cashier.domain.requests.cashier.MonthTotalSalesMerchant;
import io.example.cashier.domain.requests.cashier.YearCashierMerchantRequest;
import io.example.cashier.domain.requests.cashier.YearTotalSalesMerchant;
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.vertx.core.Future;

public interface CashierStatsByMerchant {
    Future<List<CashierResponseMonthTotalSales>> getMonthlyTotalSalesByMerchant(MonthTotalSalesMerchant req);

    Future<List<CashierResponseYearTotalSales>> getYearlyTotalSalesByMerchant(YearTotalSalesMerchant req);

    Future<List<CashierResponseMonthSales>> getMonthlyCashierByMerchant(MonthCashierMerchantRequest req);

    Future<List<CashierResponseYearSales>> getYearlyCashierByMerchant(YearCashierMerchantRequest req);
}
