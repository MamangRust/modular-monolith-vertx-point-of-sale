package io.example.order_item.repository;

import io.example.common.domain.PagedResult;
import io.example.order_item.domain.requests.FindAllOrderItems;
import io.example.order_item.model.OrderItem;
import io.vertx.core.Future;

import java.util.List;

public interface OrderItemQueryRepository {
    Future<PagedResult<OrderItem>> getOrderItems(FindAllOrderItems req);
    Future<PagedResult<OrderItem>> getOrderItemsActive(FindAllOrderItems req);
    Future<PagedResult<OrderItem>> getOrderItemsTrashed(FindAllOrderItems req);
    Future<List<OrderItem>> getOrderItemsByOrder(Long orderId);
}
