package io.example.order.service;

import io.example.order.domain.requests.CreateOrderRequest;
import io.example.order.domain.requests.UpdateOrderRequest;
import io.example.order.domain.response.order.OrderResponse;
import io.example.order.domain.response.order.OrderResponseDeleteAt;
import io.vertx.core.Future;

public interface OrderCommandService {
    Future<OrderResponse> createOrder(CreateOrderRequest req);

    Future<OrderResponse> updateOrder(UpdateOrderRequest req);

    Future<OrderResponseDeleteAt> trashedOrder(Long orderId);

    Future<OrderResponseDeleteAt> restoreOrder(Long orderId);

    Future<Boolean> deleteOrderPermanent(Long orderId);

    Future<Boolean> restoreAllOrder();

    Future<Boolean> deleteAllOrderPermanent();
}
