package io.example.order_item.service;

import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.order_item.domain.requests.FindAllOrderItems;
import io.example.order_item.model.OrderItemResponse;
import io.example.order_item.model.OrderItemResponseDeleteAt;
import io.vertx.core.Future;

import java.util.List;

public interface OrderItemQueryService {
    Future<ApiResponsePagination<List<OrderItemResponse>>> getAll(FindAllOrderItems req);
    Future<ApiResponsePagination<List<OrderItemResponseDeleteAt>>> getActive(FindAllOrderItems req);
    Future<ApiResponsePagination<List<OrderItemResponseDeleteAt>>> getTrashed(FindAllOrderItems req);
    Future<ApiResponse<List<OrderItemResponse>>> getByOrderId(Integer orderId);
}
