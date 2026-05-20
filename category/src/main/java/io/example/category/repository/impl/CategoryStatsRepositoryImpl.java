package io.example.category.repository.impl;

import io.example.category.domain.requests.MonthTotalPrice;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.CategoryStatsRepository;
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

public class CategoryStatsRepositoryImpl implements CategoryStatsRepository {
    private final Pool client;

    public CategoryStatsRepositoryImpl(Pool client) {
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
    public Future<List<CategoryMonthTotalPrice>> getMonthlyTotalPrice(MonthTotalPrice req) {
        return client
                .preparedQuery("""
                        WITH
                            monthly_totals AS (
                                SELECT EXTRACT(YEAR FROM o.created_at)::TEXT AS year,
                                       EXTRACT(MONTH FROM o.created_at)::integer AS month,
                                       COALESCE(SUM(o.total_price), 0)::INTEGER AS total_revenue
                                FROM orders o
                                    JOIN order_items oi ON o.order_id = oi.order_id
                                    JOIN products p ON oi.product_id = p.product_id
                                    JOIN categories c ON p.category_id = c.category_id
                                WHERE o.deleted_at IS NULL AND oi.deleted_at IS NULL
                                  AND ( (o.created_at >= $1 AND o.created_at <= $2)
                                     OR (o.created_at >= $3 AND o.created_at <= $4) )
                                GROUP BY EXTRACT(YEAR FROM o.created_at), EXTRACT(MONTH FROM o.created_at)
                            ),
                            all_months AS (
                                SELECT EXTRACT(YEAR FROM $1)::TEXT AS year,
                                       EXTRACT(MONTH FROM $1)::integer AS month,
                                       TO_CHAR($1, 'FMMonth') AS month_name
                                UNION
                                SELECT EXTRACT(YEAR FROM $3)::TEXT AS year,
                                       EXTRACT(MONTH FROM $3)::integer AS month,
                                       TO_CHAR($3, 'FMMonth') AS month_name
                            )
                        SELECT COALESCE(am.year, EXTRACT(YEAR FROM $1)::TEXT) AS year,
                                COALESCE(am.month_name, TO_CHAR($1, 'FMMonth')) AS month,
                                COALESCE(mt.total_revenue, 0) AS total_revenue
                        FROM all_months am LEFT JOIN monthly_totals mt ON am.year = mt.year AND am.month = mt.month
                        ORDER BY am.year::INT DESC, am.month DESC;
                        """)
                .execute(getMonthlyTuple(req.getYear(), req.getMonth()))
                .map(this::mapCategoryMonthTotalPrice);
    }

    @Override
    public Future<List<CategoryYearTotalPrice>> getYearlyTotalPrice(int year) {
        return client
                .preparedQuery("""
                        WITH yearly_data AS (
                            SELECT EXTRACT(YEAR FROM o.created_at)::integer AS year,
                                   COALESCE(SUM(o.total_price), 0)::INTEGER AS total_revenue
                            FROM orders o
                                JOIN order_items oi ON o.order_id = oi.order_id
                                JOIN products p ON oi.product_id = p.product_id
                                JOIN categories c ON p.category_id = c.category_id
                            WHERE o.deleted_at IS NULL AND oi.deleted_at IS NULL
                              AND p.deleted_at IS NULL AND c.deleted_at IS NULL
                              AND (EXTRACT(YEAR FROM o.created_at) = $1::integer OR EXTRACT(YEAR FROM o.created_at) = $1::integer - 1)
                            GROUP BY EXTRACT(YEAR FROM o.created_at)
                        ),
                        all_years AS ( SELECT $1 AS year UNION SELECT $1 - 1 AS year )
                        SELECT a.year::text AS year, COALESCE(yd.total_revenue, 0) AS total_revenue
                        FROM all_years a LEFT JOIN yearly_data yd ON a.year = yd.year
                        ORDER BY a.year DESC;
                        """)
                .execute(Tuple.of(year))
                .map(this::mapCategoryYearTotalPrice);
    }

    @Override
    public Future<List<CategoryMonthPrice>> getMonthlyCategory(int year) {
        Timestamp refTs = Timestamp.valueOf(LocalDateTime.of(year, 1, 1, 0, 0));
        return client
                .preparedQuery("""
                        WITH date_range AS (
                            SELECT date_trunc('month', $1::timestamp) AS start_date,
                                   date_trunc('month', $1::timestamp) + interval '1 year' - interval '1 day' AS end_date
                        ), monthly_category_stats AS (
                            SELECT c.category_id, c.name AS category_name, date_trunc('month', o.created_at) AS activity_month,
                                   COUNT(DISTINCT o.order_id) AS order_count, SUM(oi.quantity) AS items_sold,
                                   COALESCE(SUM(o.total_price), 0)::INTEGER AS total_revenue
                            FROM orders o JOIN order_items oi ON o.order_id = oi.order_id
                                 JOIN products p ON oi.product_id = p.product_id JOIN categories c ON p.category_id = c.category_id
                            WHERE o.deleted_at IS NULL AND oi.deleted_at IS NULL AND p.deleted_at IS NULL AND c.deleted_at IS NULL
                              AND o.created_at BETWEEN (SELECT start_date FROM date_range) AND (SELECT end_date FROM date_range)
                            GROUP BY c.category_id, c.name, activity_month
                        )
                        SELECT TO_CHAR(mcs.activity_month, 'Mon') AS month, mcs.category_id, mcs.category_name,
                               mcs.order_count, mcs.items_sold, mcs.total_revenue
                        FROM monthly_category_stats mcs ORDER BY mcs.activity_month, mcs.total_revenue DESC;
                        """)
                .execute(Tuple.of(refTs))
                .map(this::mapCategoryMonthPrice);
    }

    @Override
    public Future<List<CategoryYearPrice>> getYearlyCategory(int year) {
        Timestamp refTs = Timestamp.valueOf(LocalDateTime.of(year, 1, 1, 0, 0));
        return client
                .preparedQuery("""
                        WITH last_five_years AS (
                            SELECT c.category_id, c.name AS category_name, EXTRACT(YEAR FROM o.created_at)::text AS year,
                                   COUNT(DISTINCT o.order_id) AS order_count, SUM(oi.quantity) AS items_sold,
                                   COALESCE(SUM(o.total_price), 0)::INTEGER AS total_revenue,
                                   COUNT(DISTINCT oi.product_id) AS unique_products_sold
                            FROM orders o JOIN order_items oi ON o.order_id = oi.order_id
                                 JOIN products p ON oi.product_id = p.product_id JOIN categories c ON p.category_id = c.category_id
                            WHERE o.deleted_at IS NULL AND oi.deleted_at IS NULL AND p.deleted_at IS NULL AND c.deleted_at IS NULL
                              AND EXTRACT(YEAR FROM o.created_at) BETWEEN (EXTRACT(YEAR FROM $1::timestamp) - 4) AND EXTRACT(YEAR FROM $1::timestamp)
                            GROUP BY c.category_id, c.name, EXTRACT(YEAR FROM o.created_at)
                        )
                        SELECT year, category_id, category_name, order_count, items_sold, total_revenue, unique_products_sold
                        FROM last_five_years ORDER BY year, total_revenue DESC;
                        """)
                .execute(Tuple.of(refTs))
                .map(this::mapCategoryYearPrice);
    }

    private List<CategoryMonthTotalPrice> mapCategoryMonthTotalPrice(RowSet<Row> rows) {
        List<CategoryMonthTotalPrice> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new CategoryMonthTotalPrice(
                    r.getString("year"),
                    r.getString("month"),
                    r.getLong("total_revenue")));
        }
        return list;
    }

    private List<CategoryYearTotalPrice> mapCategoryYearTotalPrice(RowSet<Row> rows) {
        List<CategoryYearTotalPrice> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new CategoryYearTotalPrice(
                    r.getString("year"),
                    r.getLong("total_revenue")));
        }
        return list;
    }

    private List<CategoryMonthPrice> mapCategoryMonthPrice(RowSet<Row> rows) {
        List<CategoryMonthPrice> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new CategoryMonthPrice(
                    r.getString("month"),
                    r.getInteger("category_id"),
                    r.getString("category_name"),
                    r.getInteger("order_count"),
                    r.getInteger("items_sold"),
                    r.getLong("total_revenue")));
        }
        return list;
    }

    private List<CategoryYearPrice> mapCategoryYearPrice(RowSet<Row> rows) {
        List<CategoryYearPrice> list = new ArrayList<>();
        for (Row r : rows) {
            list.add(new CategoryYearPrice(
                    r.getString("year"),
                    r.getInteger("category_id"),
                    r.getString("category_name"),
                    r.getInteger("order_count"),
                    r.getInteger("items_sold"),
                    r.getLong("total_revenue"),
                    r.getInteger("unique_products_sold")));
        }
        return list;
    }
}
