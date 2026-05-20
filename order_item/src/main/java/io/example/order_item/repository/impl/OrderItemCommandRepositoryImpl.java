package io.example.order_item.repository.impl;

import io.example.order_item.domain.requests.CreateOrderItemRequest;
import io.example.order_item.domain.requests.UpdateOrderItemRequest;
import io.example.order_item.model.OrderItem;
import io.example.order_item.repository.OrderItemCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderItemCommandRepositoryImpl implements OrderItemCommandRepository {
    private final Pool client;

    @Override
    public Future<OrderItem> createOrderItem(CreateOrderItemRequest req) {
        return client
                .preparedQuery("""
                        INSERT INTO
                            order_items (
                                order_id,
                                product_id,
                                quantity,
                                price
                            )
                        VALUES ($1, $2, $3, $4)
                        RETURNING
                            order_item_id,
                            order_id,
                            product_id,
                            quantity,
                            price,
                            created_at,
                            updated_at;
                        """)
                .execute(Tuple.of(req.getOrderId(), req.getProductId(), req.getQuantity(), req.getPrice()))
                .map(rows -> OrderItem.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<OrderItem> updateOrderItem(UpdateOrderItemRequest req) {
        return client
                .preparedQuery("""
                        UPDATE order_items
                        SET
                            quantity = $2,
                            price = $3,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE
                            order_item_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            order_item_id,
                            order_id,
                            product_id,
                            quantity,
                            price,
                            created_at,
                            updated_at;
                        """)
                .execute(Tuple.of(req.getOrderItemId(), req.getQuantity(), req.getPrice()))
                .map(rows -> rows.iterator().hasNext() ? OrderItem.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<OrderItem> trashOrderItem(Long orderItemId) {
        return client
                .preparedQuery("""
                        UPDATE order_items
                        SET
                            deleted_at = current_timestamp
                        WHERE
                            order_item_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            order_item_id,
                            order_id,
                            product_id,
                            quantity,
                            price,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(orderItemId))
                .map(rows -> rows.iterator().hasNext() ? OrderItem.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<OrderItem> restoreOrderItem(Long orderItemId) {
        return client
                .preparedQuery("""
                        UPDATE order_items
                        SET
                            deleted_at = NULL
                        WHERE
                            order_item_id = $1
                            AND deleted_at IS NOT NULL
                        RETURNING
                            order_item_id,
                            order_id,
                            product_id,
                            quantity,
                            price,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(orderItemId))
                .map(rows -> rows.iterator().hasNext() ? OrderItem.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Void> deleteOrderItemPermanently(Long orderItemId) {
        return client
                .preparedQuery("DELETE FROM order_items WHERE order_item_id = $1 AND deleted_at IS NOT NULL")
                .execute(Tuple.of(orderItemId))
                .mapEmpty();
    }

    @Override
    public Future<Integer> restoreAllOrdersItem() {
        return client
                .preparedQuery("UPDATE order_items SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount);
    }

    @Override
    public Future<Integer> deleteAllPermanentOrdersItem() {
        return client
                .preparedQuery("DELETE FROM order_items WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount);
    }
}
