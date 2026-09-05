package io.example.order_item.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.domain.PagedResult;
import io.example.order_item.domain.requests.FindAllOrderItems;
import io.example.order_item.domain.response.order_item.OrderItemResponse;
import io.example.order_item.domain.response.order_item.OrderItemResponseDeleteAt;
import io.example.order_item.service.OrderItemQueryService;
import io.vertx.core.Future;

import pb.order_item.OrderItem.ApiResponsesOrderItem;
import pb.order_item.OrderItem.FindAllOrderItemRequest;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;
import pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItem;
import pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt;

@ExtendWith(MockitoExtension.class)
class OrderItemQueryHandlerTest {

    @Mock private OrderItemQueryService queryService;

    private OrderItemQueryHandler queryHandler;

    @BeforeEach
    void setUp() {
        queryHandler = new OrderItemQueryHandler(queryService);
    }

    @Test
    void findAll_shouldReturnPagedResponse() {
        FindAllOrderItemRequest request = FindAllOrderItemRequest.newBuilder()
                .setSearch("test")
                .setPage(1)
                .setPageSize(10)
                .build();

        OrderItemResponse responseDto = OrderItemResponse.builder()
                .id(1L)
                .orderId(10)
                .productId(100)
                .quantity(2)
                .price(5000)
                .createdAt("2024-01-01")
                .updatedAt("2024-01-01")
                .build();

        PagedResult<OrderItemResponse> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getAll(any(FindAllOrderItems.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationOrderItem> result = queryHandler.findAll(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
        assertThat(result.result().getData(0).getOrderId()).isEqualTo(10);
    }

    @Test
    void findByActive_shouldReturnPagedDeleteAtResponse() {
        FindAllOrderItemRequest request = FindAllOrderItemRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        OrderItemResponseDeleteAt responseDto = OrderItemResponseDeleteAt.builder()
                .id(1L)
                .orderId(10)
                .productId(100)
                .quantity(2)
                .price(5000)
                .build();

        PagedResult<OrderItemResponseDeleteAt> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getActive(any(FindAllOrderItems.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationOrderItemDeleteAt> result = queryHandler.findByActive(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
    }

    @Test
    void findByTrashed_shouldReturnPagedDeleteAtResponse() {
        FindAllOrderItemRequest request = FindAllOrderItemRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        OrderItemResponseDeleteAt responseDto = OrderItemResponseDeleteAt.builder()
                .id(1L)
                .orderId(10)
                .productId(100)
                .quantity(2)
                .price(5000)
                .build();

        PagedResult<OrderItemResponseDeleteAt> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getTrashed(any(FindAllOrderItems.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationOrderItemDeleteAt> result = queryHandler.findByTrashed(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
    }

    @Test
    void findOrderItemByOrder_shouldReturnListResponse() {
        FindByIdOrderItemRequest request = FindByIdOrderItemRequest.newBuilder()
                .setOrderItemId(10)
                .build();

        OrderItemResponse responseDto = OrderItemResponse.builder()
                .id(1L)
                .orderId(10)
                .productId(100)
                .quantity(2)
                .price(5000)
                .createdAt("2024-01-01")
                .updatedAt("2024-01-01")
                .build();

        when(queryService.getByOrderId(10)).thenReturn(Future.succeededFuture(List.of(responseDto)));

        Future<ApiResponsesOrderItem> result = queryHandler.findOrderItemByOrder(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
    }
}
