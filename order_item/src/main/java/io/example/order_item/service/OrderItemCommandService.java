package io.example.order_item.service;

import io.example.common.model.ApiResponse;
import io.example.order_item.domain.requests.CreateOrderItemRequest;
import io.example.order_item.domain.requests.UpdateOrderItemRequest;
import io.example.order_item.model.OrderItemResponse;
import io.example.order_item.model.OrderItemResponseDeleteAt;
import io.vertx.core.Future;

public interface OrderItemCommandService {
    Future<ApiResponse<OrderItemResponse>> create(CreateOrderItemRequest req);
    Future<ApiResponse<OrderItemResponse>> update(UpdateOrderItemRequest req);
    Future<ApiResponse<OrderItemResponseDeleteAt>> trash(Long orderItemId);
    Future<ApiResponse<OrderItemResponseDeleteAt>> restore(Long orderItemId);
    Future<ApiResponse<Void>> deletePermanent(Long orderItemId);
    Future<ApiResponse<Void>> restoreAll();
    Future<ApiResponse<Void>> deleteAllPermanent();
}
