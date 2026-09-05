package io.example.cashier.service;

import java.util.List;

import io.example.cashier.domain.requests.cashier.MonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.vertx.core.Future;

public interface CashierStatsService {
    Future<List<CashierResponseMonthTotalSales>> getMonthlyTotalSales(MonthTotalSales req);

    Future<List<CashierResponseYearTotalSales>> getYearlyTotalSales(int year);

    Future<List<CashierResponseMonthSales>> getMonthlyCashier(int year);

    Future<List<CashierResponseYearSales>> getYearlyCashier(int year);
}
