package io.example.cashier.repository;

import java.util.List;

import io.example.cashier.domain.requests.cashier.MonthCashierIdRequest;
import io.example.cashier.domain.requests.cashier.MonthTotalSalesCashier;
import io.example.cashier.domain.requests.cashier.YearCashierIdRequest;
import io.example.cashier.domain.requests.cashier.YearTotalSalesCashier;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.vertx.core.Future;

public interface CashierStatByIdRepository {
    Future<List<CashierMonthTotalSales>> getMonthlyTotalSalesById(MonthTotalSalesCashier req);

    Future<List<CashierYearTotalSales>> getYearlyTotalSalesById(YearTotalSalesCashier req);

    Future<List<CashierMonthSales>> getMonthlyCashierById(MonthCashierIdRequest req);

    Future<List<CashierYearSales>> getYearlyCashierById(YearCashierIdRequest req);
}
