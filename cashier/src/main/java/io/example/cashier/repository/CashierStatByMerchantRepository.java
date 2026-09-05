package io.example.cashier.repository;

import java.util.List;

import io.example.cashier.domain.requests.cashier.MonthCashierMerchantRequest;
import io.example.cashier.domain.requests.cashier.MonthTotalSalesMerchant;
import io.example.cashier.domain.requests.cashier.YearCashierMerchantRequest;
import io.example.cashier.domain.requests.cashier.YearTotalSalesMerchant;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.vertx.core.Future;

public interface CashierStatByMerchantRepository {
    Future<List<CashierMonthTotalSales>> getMonthlyTotalSalesByMerchant(MonthTotalSalesMerchant req);

    Future<List<CashierYearTotalSales>> getYearlyTotalSalesByMerchant(YearTotalSalesMerchant req);

    Future<List<CashierMonthSales>> getMonthlyCashierByMerchant(MonthCashierMerchantRequest req);

    Future<List<CashierYearSales>> getYearlyCashierByMerchant(YearCashierMerchantRequest req);
}
