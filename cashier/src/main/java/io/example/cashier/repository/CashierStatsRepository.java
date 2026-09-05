package io.example.cashier.repository;

import java.util.List;

import io.example.cashier.domain.requests.cashier.MonthTotalSales;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.vertx.core.Future;

public interface CashierStatsRepository {
    Future<List<CashierMonthTotalSales>> getMonthlyTotalSales(MonthTotalSales req);

    Future<List<CashierYearTotalSales>> getYearlyTotalSales(int year);

    Future<List<CashierMonthSales>> getMonthlyCashier(int year);

    Future<List<CashierYearSales>> getYearlyCashier(int year);
}
