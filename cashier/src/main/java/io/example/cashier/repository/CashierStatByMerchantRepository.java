package io.example.cashier.repository;

import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.vertx.core.Future;

import java.util.List;

public interface CashierStatByMerchantRepository {
    Future<List<CashierMonthTotalSales>> getMonthlyTotalSalesByMerchant(int year, int month, Long merchantId);
    Future<List<CashierYearTotalSales>> getYearlyTotalSalesByMerchant(int year, Long merchantId);
    Future<List<CashierMonthSales>> getMonthlyCashierByMerchant(int year, Long merchantId);
    Future<List<CashierYearSales>> getYearlyCashierByMerchant(int year, Long merchantId);
}
