package io.example.cashier.repository;

import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.vertx.core.Future;

import java.util.List;

public interface CashierStatByIdRepository {
    Future<List<CashierMonthTotalSales>> getMonthlyTotalSalesById(int year, int month, Long cashierId);
    Future<List<CashierYearTotalSales>> getYearlyTotalSalesById(int year, Long cashierId);
    Future<List<CashierMonthSales>> getMonthlyCashierById(int year, Long cashierId);
    Future<List<CashierYearSales>> getYearlyCashierById(int year, Long cashierId);
}
