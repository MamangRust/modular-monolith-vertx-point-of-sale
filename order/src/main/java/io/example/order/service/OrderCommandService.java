package io.example.order.service;

import io.example.order.domain.requests.CreateOrderRequest;
import io.example.order.domain.requests.UpdateOrderRequest;
import io.example.order.model.Order;
import io.vertx.core.Future;

public interface OrderCommandService {
    Future<Order> createOrder(CreateOrderRequest req);
    Future<Order> updateOrder(UpdateOrderRequest req);
    Future<Order> trashedOrder(int orderId);
    Future<Order> restoreOrder(int orderId);
    Future<Boolean> deleteOrderPermanent(int orderId);
    Future<Boolean> restoreAllOrder();
    Future<Boolean> deleteAllOrderPermanent();
}
