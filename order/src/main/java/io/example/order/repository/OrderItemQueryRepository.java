package io.example.order.repository;

import java.util.List;
import io.example.order.model.OrderItem;
import io.vertx.core.Future;

public interface OrderItemQueryRepository {
    Future<List<OrderItem>> findOrderItemByOrder(Long orderId);
    Future<Integer> calculateTotalPrice(Long orderId);
}
