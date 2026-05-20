package io.example.order_item.repository;

import io.example.order_item.domain.requests.CreateOrderItemRequest;
import io.example.order_item.domain.requests.UpdateOrderItemRequest;
import io.example.order_item.model.OrderItem;
import io.vertx.core.Future;

public interface OrderItemCommandRepository {
    Future<OrderItem> createOrderItem(CreateOrderItemRequest req);
    Future<OrderItem> updateOrderItem(UpdateOrderItemRequest req);
    Future<OrderItem> trashOrderItem(Long orderItemId);
    Future<OrderItem> restoreOrderItem(Long orderItemId);
    Future<Void> deleteOrderItemPermanently(Long orderItemId);
    Future<Integer> restoreAllOrdersItem();
    Future<Integer> deleteAllPermanentOrdersItem();
}
