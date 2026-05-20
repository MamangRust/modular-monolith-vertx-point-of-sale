package io.example.order.repository;

import io.example.common.domain.PagedResult;
import io.example.order.domain.requests.FindAllOrders;
import io.example.order.domain.requests.FindAllOrderMerchant;
import io.example.order.model.Order;
import io.vertx.core.Future;

public interface OrderQueryRepository {
    Future<PagedResult<Order>> findAllOrders(FindAllOrders req);
    Future<PagedResult<Order>> findByActive(FindAllOrders req);
    Future<PagedResult<Order>> findByTrashed(FindAllOrders req);
    Future<PagedResult<Order>> findByMerchant(FindAllOrderMerchant req);
    Future<Order> findById(Long orderId);
}
