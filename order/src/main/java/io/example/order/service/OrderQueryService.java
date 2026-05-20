package io.example.order.service;

import io.example.common.domain.PagedResult;
import io.example.order.domain.requests.FindAllOrders;
import io.example.order.domain.requests.FindAllOrderMerchant;
import io.example.order.model.Order;
import io.vertx.core.Future;

public interface OrderQueryService {
    Future<PagedResult<Order>> findAll(FindAllOrders req);
    Future<Order> findById(int orderId);
    Future<PagedResult<Order>> findByActive(FindAllOrders req);
    Future<PagedResult<Order>> findByTrashed(FindAllOrders req);
    Future<PagedResult<Order>> findByMerchant(FindAllOrderMerchant req);
}
