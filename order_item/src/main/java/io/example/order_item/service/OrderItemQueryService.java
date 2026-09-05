package io.example.order_item.service;

import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.order_item.domain.requests.FindAllOrderItems;
import io.example.order_item.domain.response.order_item.OrderItemResponse;
import io.example.order_item.domain.response.order_item.OrderItemResponseDeleteAt;
import io.vertx.core.Future;

public interface OrderItemQueryService {
    Future<PagedResult<OrderItemResponse>> getAll(FindAllOrderItems req);

    Future<PagedResult<OrderItemResponseDeleteAt>> getActive(FindAllOrderItems req);

    Future<PagedResult<OrderItemResponseDeleteAt>> getTrashed(FindAllOrderItems req);

    Future<List<OrderItemResponse>> getByOrderId(Integer orderId);
}
