package io.example.cashier.service;

import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.vertx.core.Future;

import java.util.List;

public interface CashierStatsService {
    Future<List<CashierMonthTotalSales>> getMonthlyTotalSales(int year, int month);
    Future<List<CashierYearTotalSales>> getYearlyTotalSales(int year);
    Future<List<CashierMonthSales>> getMonthlyCashier(int year);
    Future<List<CashierYearSales>> getYearlyCashier(int year);
}
