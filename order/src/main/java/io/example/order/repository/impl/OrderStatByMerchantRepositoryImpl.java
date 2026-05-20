package io.example.order.repository.impl;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import io.example.order.domain.requests.MonthOrderMerchant;
import io.example.order.domain.requests.MonthTotalRevenueMerchant;
import io.example.order.domain.requests.YearOrderMerchant;
import io.example.order.domain.requests.YearTotalRevenueMerchant;
import io.example.order.model.OrderMonth;
import io.example.order.model.OrderMonthTotalRevenue;
import io.example.order.model.OrderYear;
import io.example.order.model.OrderYearTotalRevenue;
import io.example.order.repository.OrderStatByMerchantRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class OrderStatByMerchantRepositoryImpl implements OrderStatByMerchantRepository {
    private final Pool client;

    public OrderStatByMerchantRepositoryImpl(Pool client) {
        this.client = client;
    }

    private Tuple getMonthlyTuple(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDate startPrevDate = startDate.minusYears(1);
        LocalDate endPrevDate = startPrevDate.withDayOfMonth(startPrevDate.lengthOfMonth());
        return Tuple.of(startDate, endDate, startPrevDate, endPrevDate);
    }

    @Override
    public Future<List<OrderMonthTotalRevenue>> getMonthlyTotalRevenueByMerchant(MonthTotalRevenueMerchant req) {
        Tuple args = getMonthlyTuple(req.getYear(), req.getMonth()).addLong(req.getMerchantId());
        return client.preparedQuery(
                """
                WITH monthly_revenue AS (
                    SELECT EXTRACT(YEAR FROM o.created_at)::TEXT AS year, EXTRACT(MONTH FROM o.created_at)::integer AS month, COALESCE(SUM(o.total_price), 0)::INTEGER AS total_revenue
                    FROM orders o JOIN order_items oi ON o.order_id = oi.order_id
                    WHERE o.deleted_at IS NULL AND oi.deleted_at IS NULL
                      AND ( (o.created_at >= $1 AND o.created_at <= $2) OR (o.created_at >= $3 AND o.created_at <= $4) )
                      AND o.merchant_id = $5
                    GROUP BY EXTRACT(YEAR FROM o.created_at), EXTRACT(MONTH FROM o.created_at)
                ), all_months AS (
                    SELECT EXTRACT(YEAR FROM $1)::TEXT AS year, EXTRACT(MONTH FROM $1)::integer AS month, TO_CHAR($1, 'FMMonth') AS month_name
                    UNION SELECT EXTRACT(YEAR FROM $3)::TEXT AS year, EXTRACT(MONTH FROM $3)::integer AS month, TO_CHAR($3, 'FMMonth') AS month_name
                )
                SELECT COALESCE(am.year, EXTRACT(YEAR FROM $1)::TEXT) AS year, COALESCE(am.month_name, TO_CHAR($1, 'FMMonth')) AS month, COALESCE(mr.total_revenue, 0) AS total_revenue
                FROM all_months am LEFT JOIN monthly_revenue mr ON am.year = mr.year AND am.month = mr.month
                ORDER BY am.year DESC, am.month DESC;
                """)
                .execute(args)
                .map(this::mapOrderMonthTotalRevenue);
    }

    @Override
    public Future<List<OrderYearTotalRevenue>> getYearlyTotalRevenueByMerchant(YearTotalRevenueMerchant req) {
        return client.preparedQuery(
                """
                WITH yearly_revenue AS (
                    SELECT EXTRACT(YEAR FROM o.created_at)::integer AS year, COALESCE(SUM(o.total_price), 0)::INTEGER AS total_revenue
                    FROM orders o JOIN order_items oi ON o.order_id = oi.order_id
                    WHERE o.deleted_at IS NULL AND oi.deleted_at IS NULL
                      AND (EXTRACT(YEAR FROM o.created_at) = $1::integer OR EXTRACT(YEAR FROM o.created_at) = $1::integer - 1)
                      AND o.merchant_id = $2
                    GROUP BY EXTRACT(YEAR FROM o.created_at)
                ), all_years AS ( SELECT $1 AS year UNION SELECT $1 - 1 AS year )
                SELECT ay.year::text AS year, COALESCE(yr.total_revenue, 0) AS total_revenue
                FROM all_years ay LEFT JOIN yearly_revenue yr ON ay.year = yr.year
                ORDER BY ay.year DESC;
                """)
                .execute(Tuple.of(req.getYear(), req.getMerchantId()))
                .map(this::mapOrderYearTotalRevenue);
    }

    @Override
    public Future<List<OrderMonth>> getMonthlyOrderByMerchant(MonthOrderMerchant req) {
        Timestamp refTs = Timestamp.valueOf(LocalDateTime.of(req.getYear(), 1, 1, 0, 0));
        return client.preparedQuery(
                """
                WITH date_range AS (
                    SELECT date_trunc('month', $1::timestamp) AS start_date, date_trunc('month', $1::timestamp) + interval '1 year' - interval '1 day' AS end_date
                ), monthly_orders AS (
                    SELECT date_trunc('month', o.created_at) AS activity_month, COUNT(o.order_id) AS order_count, SUM(o.total_price)::NUMERIC AS total_revenue, SUM(oi.quantity) AS total_items_sold
                    FROM orders o JOIN order_items oi ON o.order_id = oi.order_id
                    WHERE o.deleted_at IS NULL AND oi.deleted_at IS NULL
                      AND o.created_at BETWEEN (SELECT start_date FROM date_range) AND (SELECT end_date FROM date_range)
                      AND o.merchant_id = $2
                    GROUP BY activity_month
                )
                SELECT TO_CHAR(mo.activity_month, 'Mon') AS month, mo.order_count, mo.total_revenue, mo.total_items_sold
                FROM monthly_orders mo ORDER BY mo.activity_month;
                """)
                .execute(Tuple.of(refTs, req.getMerchantId()))
                .map(this::mapOrderMonth);
    }

    @Override
    public Future<List<OrderYear>> getYearlyOrderByMerchant(YearOrderMerchant req) {
        Timestamp refTs = Timestamp.valueOf(LocalDateTime.of(req.getYear(), 1, 1, 0, 0));
        return client.preparedQuery(
                """
                WITH last_five_years AS (
                    SELECT EXTRACT(YEAR FROM o.created_at)::text AS year, COUNT(o.order_id) AS order_count, SUM(o.total_price)::NUMERIC AS total_revenue, SUM(oi.quantity) AS total_items_sold, COUNT(DISTINCT o.cashier_id) AS active_cashiers, COUNT(DISTINCT oi.product_id) AS unique_products_sold
                    FROM orders o JOIN order_items oi ON o.order_id = oi.order_id
                    WHERE o.deleted_at IS NULL AND oi.deleted_at IS NULL
                      AND EXTRACT(YEAR FROM o.created_at) BETWEEN (EXTRACT(YEAR FROM $1::timestamp) - 4) AND EXTRACT(YEAR FROM $1::timestamp)
                      AND o.merchant_id = $2
                    GROUP BY EXTRACT(YEAR FROM o.created_at)
                )
                SELECT year, order_count, total_revenue, total_items_sold, active_cashiers, unique_products_sold
                FROM last_five_years ORDER BY year;
                """)
                .execute(Tuple.of(refTs, req.getMerchantId()))
                .map(this::mapOrderYear);
    }

    private List<OrderMonthTotalRevenue> mapOrderMonthTotalRevenue(RowSet<Row> rows) {
        List<OrderMonthTotalRevenue> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new OrderMonthTotalRevenue(
                    r.getString("year"),
                    r.getString("month"),
                    r.getInteger("total_revenue")));
        }
        return list;
    }

    private List<OrderYearTotalRevenue> mapOrderYearTotalRevenue(RowSet<Row> rows) {
        List<OrderYearTotalRevenue> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new OrderYearTotalRevenue(
                    r.getString("year"),
                    r.getInteger("total_revenue")));
        }
        return list;
    }

    private List<OrderMonth> mapOrderMonth(RowSet<Row> rows) {
        List<OrderMonth> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new OrderMonth(
                    r.getString("month"),
                    r.getInteger("order_count"),
                    r.getLong("total_revenue"),
                    r.getInteger("total_items_sold")));
        }
        return list;
    }

    private List<OrderYear> mapOrderYear(RowSet<Row> rows) {
        List<OrderYear> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new OrderYear(
                    r.getString("year"),
                    r.getInteger("order_count"),
                    r.getLong("total_revenue"),
                    r.getInteger("total_items_sold"),
                    r.getInteger("active_cashiers"),
                    r.getInteger("unique_products_sold")));
        }
        return list;
    }
}
