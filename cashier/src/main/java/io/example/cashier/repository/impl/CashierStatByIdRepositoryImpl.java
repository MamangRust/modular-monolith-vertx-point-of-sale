package io.example.cashier.repository.impl;

import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.CashierStatByIdRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CashierStatByIdRepositoryImpl implements CashierStatByIdRepository {
    private final Pool client;

    public CashierStatByIdRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<List<CashierMonthTotalSales>> getMonthlyTotalSalesById(int year, int month, Long cashierId) {
        Tuple args = getMonthlyTuple(year, month).addLong(cashierId);
        return client
                .preparedQuery("""
                        WITH monthly_totals AS (
                            SELECT EXTRACT(YEAR FROM o.created_at)::TEXT AS year, EXTRACT(MONTH FROM o.created_at)::integer AS month, COALESCE(SUM(o.total_price), 0)::INTEGER AS total_sales
                            FROM orders o JOIN cashiers c ON o.cashier_id = c.cashier_id
                            WHERE o.deleted_at IS NULL AND c.deleted_at IS NULL
                              AND ( (o.created_at >= $1 AND o.created_at <= $2) OR (o.created_at >= $3 AND o.created_at <= $4) )
                              AND c.cashier_id = $5
                            GROUP BY EXTRACT(YEAR FROM o.created_at), EXTRACT(MONTH FROM o.created_at)
                        ), all_months AS (
                            SELECT EXTRACT(YEAR FROM $1)::TEXT AS year, EXTRACT(MONTH FROM $1)::integer AS month, TO_CHAR($1, 'FMMonth') AS month_name
                            UNION SELECT EXTRACT(YEAR FROM $3)::TEXT AS year, EXTRACT(MONTH FROM $3)::integer AS month, TO_CHAR($3, 'FMMonth') AS month_name
                        )
                        SELECT COALESCE(am.year, EXTRACT(YEAR FROM $1)::TEXT) AS year, COALESCE(am.month_name, TO_CHAR($1, 'FMMonth')) AS month, COALESCE(mt.total_sales, 0) AS total_sales
                        FROM all_months am LEFT JOIN monthly_totals mt ON am.year = mt.year AND am.month = mt.month
                        ORDER BY am.year::INT DESC, am.month DESC;
                        """)
                .execute(args)
                .map(this::mapCashierMonthTotalSales);
    }

    @Override
    public Future<List<CashierYearTotalSales>> getYearlyTotalSalesById(int year, Long cashierId) {
        return client
                .preparedQuery("""
                        WITH yearly_data AS (
                            SELECT EXTRACT(YEAR FROM o.created_at)::integer AS year, COALESCE(SUM(o.total_price), 0)::INTEGER AS total_sales
                            FROM orders o JOIN cashiers c ON o.cashier_id = c.cashier_id
                            WHERE o.deleted_at IS NULL AND c.deleted_at IS NULL
                              AND (EXTRACT(YEAR FROM o.created_at) = $1::integer OR EXTRACT(YEAR FROM o.created_at) = $1::integer - 1)
                              AND c.cashier_id = $2
                            GROUP BY EXTRACT(YEAR FROM o.created_at)
                        ), all_years AS ( SELECT $1 AS year UNION SELECT $1 - 1 AS year )
                        SELECT a.year::text AS year, COALESCE(yd.total_sales, 0) AS total_sales
                        FROM all_years a LEFT JOIN yearly_data yd ON a.year = yd.year ORDER BY a.year DESC;
                        """)
                .execute(Tuple.of(year, cashierId))
                .map(this::mapCashierYearTotalSales);
    }

    @Override
    public Future<List<CashierMonthSales>> getMonthlyCashierById(int year, Long cashierId) {
        Timestamp refTs = Timestamp.valueOf(LocalDateTime.of(year, 1, 1, 0, 0));
        return client
                .preparedQuery("""
                        WITH date_range AS (
                            SELECT date_trunc('month', $1::timestamp) AS start_date, date_trunc('month', $1::timestamp) + interval '1 year' - interval '1 day' AS end_date
                        ), cashier_activity AS (
                            SELECT c.cashier_id, c.name AS cashier_name, date_trunc('month', o.created_at) AS activity_month,
                                   COUNT(o.order_id) AS order_count, SUM(o.total_price) AS total_sales
                            FROM orders o JOIN cashiers c ON o.cashier_id = c.cashier_id
                            WHERE o.deleted_at IS NULL AND c.deleted_at IS NULL AND c.cashier_id = $2
                              AND o.created_at BETWEEN (SELECT start_date FROM date_range) AND (SELECT end_date FROM date_range)
                            GROUP BY c.cashier_id, c.name, activity_month
                        )
                        SELECT ca.cashier_id, ca.cashier_name, TO_CHAR(ca.activity_month, 'Mon') AS month, ca.order_count, ca.total_sales
                        FROM cashier_activity ca ORDER BY ca.activity_month, ca.cashier_id;
                        """)
                .execute(Tuple.of(refTs, cashierId))
                .map(this::mapCashierMonthSales);
    }

    @Override
    public Future<List<CashierYearSales>> getYearlyCashierById(int year, Long cashierId) {
        Timestamp refTs = Timestamp.valueOf(LocalDateTime.of(year, 1, 1, 0, 0));
        return client
                .preparedQuery("""
                        WITH last_five_years AS (
                            SELECT c.cashier_id, c.name AS cashier_name, EXTRACT(YEAR FROM o.created_at)::text AS year,
                                   COUNT(o.order_id) AS order_count, SUM(o.total_price) AS total_sales
                            FROM orders o JOIN cashiers c ON o.cashier_id = c.cashier_id
                            WHERE o.deleted_at IS NULL AND c.deleted_at IS NULL AND c.cashier_id = $2
                              AND EXTRACT(YEAR FROM o.created_at) BETWEEN (EXTRACT(YEAR FROM $1::timestamp) - 4) AND EXTRACT(YEAR FROM $1::timestamp)
                            GROUP BY c.cashier_id, c.name, EXTRACT(YEAR FROM o.created_at)
                        )
                        SELECT year, cashier_id, cashier_name, order_count, total_sales
                        FROM last_five_years ORDER BY year, cashier_id;
                        """)
                .execute(Tuple.of(refTs, cashierId))
                .map(this::mapCashierYearSales);
    }

    private Tuple getMonthlyTuple(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDate startPrevDate = startDate.minusYears(1);
        LocalDate endPrevDate = startPrevDate.withDayOfMonth(startPrevDate.lengthOfMonth());
        return Tuple.of(startDate, endDate, startPrevDate, endPrevDate);
    }

    private List<CashierMonthTotalSales> mapCashierMonthTotalSales(RowSet<Row> rows) {
        List<CashierMonthTotalSales> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new CashierMonthTotalSales(
                    r.getString("year"),
                    r.getString("month"),
                    r.getLong("total_sales")));
        }
        return list;
    }

    private List<CashierYearTotalSales> mapCashierYearTotalSales(RowSet<Row> rows) {
        List<CashierYearTotalSales> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new CashierYearTotalSales(
                    r.getString("year"),
                    r.getLong("total_sales")));
        }
        return list;
    }

    private List<CashierMonthSales> mapCashierMonthSales(RowSet<Row> rows) {
        List<CashierMonthSales> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new CashierMonthSales(
                    r.getString("month"),
                    r.getInteger("cashier_id"),
                    r.getString("cashier_name"),
                    r.getInteger("order_count"),
                    r.getLong("total_sales")));
        }
        return list;
    }

    private List<CashierYearSales> mapCashierYearSales(RowSet<Row> rows) {
        List<CashierYearSales> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new CashierYearSales(
                    r.getString("year"),
                    r.getInteger("cashier_id"),
                    r.getString("cashier_name"),
                    r.getInteger("order_count"),
                    r.getLong("total_sales")));
        }
        return list;
    }
}
