package io.example.order.repository.impl;

import io.example.order.domain.requests.CreateOrderRecordRequest;
import io.example.order.domain.requests.UpdateOrderRecordRequest;
import io.example.order.model.Order;
import io.example.order.repository.OrderCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class OrderCommandRepositoryImpl implements OrderCommandRepository {
    private final Pool client;

    public OrderCommandRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<Order> createOrder(CreateOrderRecordRequest req) {
        return client.preparedQuery(
                """
                INSERT INTO
                    orders (
                        merchant_id,
                        cashier_id,
                        total_price
                    )
                VALUES ($1, $2, $3)
                RETURNING
                    order_id,
                    merchant_id,
                    cashier_id,
                    total_price,
                    created_at,
                    updated_at;
                """)
                .execute(Tuple.of(req.getMerchantId(), req.getCashierId(), req.getTotalPrice()))
                .map(rows -> Order.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<Order> updateOrder(UpdateOrderRecordRequest req) {
        return client.preparedQuery(
                """
                UPDATE orders
                SET
                    total_price = $2,
                    updated_at = CURRENT_TIMESTAMP
                WHERE
                    order_id = $1
                    AND deleted_at IS NULL
                RETURNING
                    order_id,
                    merchant_id,
                    cashier_id,
                    total_price,
                    created_at,
                    updated_at;
                """)
                .execute(Tuple.of(req.getOrderId(), req.getTotalPrice()))
                .map(rows -> rows.iterator().hasNext() ? Order.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Order> trashedOrder(Long orderId) {
        return client.preparedQuery(
                """
                UPDATE orders
                SET
                    deleted_at = current_timestamp
                WHERE
                    order_id = $1
                    AND deleted_at IS NULL
                RETURNING
                    order_id,
                    merchant_id,
                    cashier_id,
                    total_price,
                    created_at,
                    updated_at,
                    deleted_at;
                """)
                .execute(Tuple.of(orderId))
                .map(rows -> rows.iterator().hasNext() ? Order.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Order> restoreOrder(Long orderId) {
        return client.preparedQuery(
                """
                UPDATE orders
                SET
                    deleted_at = NULL
                WHERE
                    order_id = $1
                    AND deleted_at IS NOT NULL
                RETURNING
                    order_id,
                    merchant_id,
                    cashier_id,
                    total_price,
                    created_at,
                    updated_at,
                    deleted_at;
                """)
                .execute(Tuple.of(orderId))
                .map(rows -> rows.iterator().hasNext() ? Order.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Boolean> deleteOrderPermanent(Long orderId) {
        return client.preparedQuery("DELETE FROM orders WHERE order_id = $1 AND deleted_at IS NOT NULL")
                .execute(Tuple.of(orderId))
                .map(res -> res.rowCount() > 0);
    }

    @Override
    public Future<Boolean> restoreAllOrder() {
        return client.preparedQuery("UPDATE orders SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
                .execute()
                .map(res -> res.rowCount() > 0);
    }

    @Override
    public Future<Boolean> deleteAllOrderPermanent() {
        return client.preparedQuery("DELETE FROM orders WHERE deleted_at IS NOT NULL")
                .execute()
                .map(res -> res.rowCount() > 0);
    }
}
