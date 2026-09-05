package io.example.order.service;

import io.example.common.domain.PagedResult;
import io.example.order.domain.requests.FindAllOrders;
import io.example.order.domain.response.order.OrderResponse;
import io.example.order.domain.response.order.OrderResponseDeleteAt;
import io.example.order.domain.requests.FindAllOrderMerchant;
import io.vertx.core.Future;

public interface OrderQueryService {
    Future<PagedResult<OrderResponse>> findAll(FindAllOrders req);

    Future<OrderResponse> findById(Long orderId);

    Future<PagedResult<OrderResponseDeleteAt>> findByActive(FindAllOrders req);

    Future<PagedResult<OrderResponseDeleteAt>> findByTrashed(FindAllOrders req);

    Future<PagedResult<OrderResponse>> findByMerchant(FindAllOrderMerchant req);
}
