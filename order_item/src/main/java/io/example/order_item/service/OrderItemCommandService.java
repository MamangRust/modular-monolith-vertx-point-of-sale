package io.example.order_item.service;

import io.example.order_item.domain.requests.CreateOrderItemRequest;
import io.example.order_item.domain.requests.UpdateOrderItemRequest;
import io.example.order_item.domain.response.order_item.OrderItemResponse;
import io.example.order_item.domain.response.order_item.OrderItemResponseDeleteAt;
import io.vertx.core.Future;

public interface OrderItemCommandService {
    Future<OrderItemResponse> create(CreateOrderItemRequest req);

    Future<OrderItemResponse> update(UpdateOrderItemRequest req);

    Future<OrderItemResponseDeleteAt> trash(Long orderItemId);

    Future<OrderItemResponseDeleteAt> restore(Long orderItemId);

    Future<Void> deletePermanent(Long orderItemId);

    Future<Void> restoreAll();

    Future<Void> deleteAllPermanent();
}
