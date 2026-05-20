package io.example.order.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.order.domain.requests.FindAllOrders;
import io.example.order.domain.requests.FindAllOrderMerchant;
import io.example.order.model.Order;
import io.example.order.repository.OrderQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class OrderQueryRepositoryImpl implements OrderQueryRepository {
    private final Pool client;

    public OrderQueryRepositoryImpl(Pool client) {
        this.client = client;
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank())
            return null;
        return search;
    }

    private PagedResult<Order> mapPagedOrders(RowSet<Row> rows) {
        List<Order> list = new ArrayList<>();
        int total = 0;
        for (Row row : rows) {
            list.add(Order.fromRow(row));
            if (total == 0)
                total = row.getInteger("total_count");
        }
        return new PagedResult<>(list, total);
    }

    @Override
    public Future<PagedResult<Order>> findAllOrders(FindAllOrders req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client.preparedQuery(
                """
                SELECT
                    order_id,
                    merchant_id,
                    cashier_id,
                    total_price,
                    created_at,
                    updated_at,
                    COUNT(*) OVER () AS total_count
                FROM orders
                WHERE
                    deleted_at IS NULL
                    AND (
                        $1::TEXT IS NULL
                        OR order_id::TEXT ILIKE '%' || $1 || '%'
                        OR total_price::TEXT ILIKE '%' || $1 || '%'
                    )
                ORDER BY created_at DESC
                LIMIT $2
                OFFSET $3;
                """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedOrders);
    }

    @Override
    public Future<PagedResult<Order>> findByActive(FindAllOrders req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client.preparedQuery(
                """
                SELECT
                    order_id,
                    merchant_id,
                    cashier_id,
                    total_price,
                    created_at,
                    updated_at,
                    deleted_at,
                    COUNT(*) OVER () AS total_count
                FROM orders
                WHERE
                    deleted_at IS NULL
                    AND (
                        $1::TEXT IS NULL
                        OR order_id::TEXT ILIKE '%' || $1 || '%'
                        OR total_price::TEXT ILIKE '%' || $1 || '%'
                    )
                ORDER BY created_at DESC
                LIMIT $2
                OFFSET $3;
                """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedOrders);
    }

    @Override
    public Future<PagedResult<Order>> findByTrashed(FindAllOrders req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client.preparedQuery(
                """
                SELECT
                    order_id,
                    merchant_id,
                    cashier_id,
                    total_price,
                    created_at,
                    updated_at,
                    deleted_at,
                    COUNT(*) OVER () AS total_count
                FROM orders
                WHERE
                    deleted_at IS NOT NULL
                    AND (
                        $1::TEXT IS NULL
                        OR order_id::TEXT ILIKE '%' || $1 || '%'
                        OR total_price::TEXT ILIKE '%' || $1 || '%'
                    )
                ORDER BY deleted_at DESC
                LIMIT $2
                OFFSET $3;
                """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedOrders);
    }

    @Override
    public Future<PagedResult<Order>> findByMerchant(FindAllOrderMerchant req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client.preparedQuery(
                """
                SELECT *, COUNT(*) OVER () AS total_count
                FROM orders
                WHERE
                    deleted_at IS NULL
                    AND (
                        $1::TEXT IS NULL
                        OR order_id::TEXT ILIKE '%' || $1 || '%'
                        OR total_price::TEXT ILIKE '%' || $1 || '%'
                    )
                    AND merchant_id = $4
                ORDER BY created_at DESC
                LIMIT $2
                OFFSET $3;
                """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset, req.getMerchantId()))
                .map(this::mapPagedOrders);
    }

    @Override
    public Future<Order> findById(Long orderId) {
        return client.preparedQuery(
                """
                SELECT
                    order_id,
                    merchant_id,
                    cashier_id,
                    total_price,
                    created_at,
                    updated_at
                FROM orders
                WHERE
                    order_id = $1
                    AND deleted_at IS NULL;
                """)
                .execute(Tuple.of(orderId))
                .map(rows -> rows.iterator().hasNext() ? Order.fromRow(rows.iterator().next()) : null);
    }
}
