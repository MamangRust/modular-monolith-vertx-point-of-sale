package io.example.cashier.service;

import java.util.List;

import io.example.cashier.domain.requests.cashier.MonthCashierIdRequest;
import io.example.cashier.domain.requests.cashier.MonthTotalSalesCashier;
import io.example.cashier.domain.requests.cashier.YearCashierIdRequest;
import io.example.cashier.domain.requests.cashier.YearTotalSalesCashier;
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.vertx.core.Future;

public interface CashierStatsByIdService {
    Future<List<CashierResponseMonthTotalSales>> getMonthlyTotalSalesById(MonthTotalSalesCashier req);

    Future<List<CashierResponseYearTotalSales>> getYearlyTotalSalesById(YearTotalSalesCashier req);

    Future<List<CashierResponseMonthSales>> getMonthlyCashierById(MonthCashierIdRequest req);

    Future<List<CashierResponseYearSales>> getYearlyCashierById(YearCashierIdRequest req);
}
