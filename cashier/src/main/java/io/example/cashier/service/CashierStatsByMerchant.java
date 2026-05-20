package io.example.cashier.service;

import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.vertx.core.Future;

import java.util.List;

public interface CashierStatsByMerchant {
    Future<List<CashierMonthTotalSales>> getMonthlyTotalSalesByMerchant(int year, int month, Long merchantId);
    Future<List<CashierYearTotalSales>> getYearlyTotalSalesByMerchant(int year, Long merchantId);
    Future<List<CashierMonthSales>> getMonthlyCashierByMerchant(int year, Long merchantId);
    Future<List<CashierYearSales>> getYearlyCashierByMerchant(int year, Long merchantId);
}
