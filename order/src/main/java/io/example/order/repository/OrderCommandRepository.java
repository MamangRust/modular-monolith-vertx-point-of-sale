package io.example.order.repository;

import io.example.order.domain.requests.CreateOrderRecordRequest;
import io.example.order.domain.requests.UpdateOrderRecordRequest;
import io.example.order.model.Order;
import io.vertx.core.Future;

public interface OrderCommandRepository {
    Future<Order> createOrder(CreateOrderRecordRequest request);
    Future<Order> updateOrder(UpdateOrderRecordRequest request);
    Future<Order> trashedOrder(Long orderId);
    Future<Order> restoreOrder(Long orderId);
    Future<Boolean> deleteOrderPermanent(Long orderId);
    Future<Boolean> restoreAllOrder();
    Future<Boolean> deleteAllOrderPermanent();
}
