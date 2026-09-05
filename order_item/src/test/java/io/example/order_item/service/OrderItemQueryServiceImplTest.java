package io.example.order_item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.order_item.domain.requests.FindAllOrderItems;
import io.example.order_item.domain.response.order_item.OrderItemResponse;
import io.example.order_item.domain.response.order_item.OrderItemResponseDeleteAt;
import io.example.order_item.model.OrderItem;
import io.example.order_item.repository.OrderItemQueryRepository;
import io.example.order_item.service.impl.OrderItemQueryServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class OrderItemQueryServiceImplTest {

    @Mock private OrderItemQueryRepository queryRepo;
    @Mock private RedisService redis;
    @Mock private TracingMetrics metrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private OrderItemQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        lenient().when(metrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redis.setJson(anyString(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

        queryService = new OrderItemQueryServiceImpl(queryRepo, redis, metrics);
    }

    private OrderItem createOrderItem() {
        return OrderItem.builder()
                .orderItemId(1L)
                .orderId(10L)
                .productId(100L)
                .quantity(2)
                .price(5000)
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .build();
    }

    // --- getAll ---

    @Test
    void getAll_shouldFetchFromDbWhenCacheMiss() {
        FindAllOrderItems req = FindAllOrderItems.builder().search("test").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getOrderItems(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createOrderItem()), 1)));

        Future<PagedResult<OrderItemResponse>> result = queryService.getAll(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getId()).isEqualTo(1L);
        assertThat(result.result().getData().get(0).getOrderId()).isEqualTo(10);
        verify(queryRepo).getOrderItems(req);
    }

    @Test
    void getAll_shouldReturnFromCacheWhenCacheHit() {
        FindAllOrderItems req = FindAllOrderItems.builder().search("test").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"orderItemId\":1,\"orderId\":10,\"productId\":100,\"quantity\":2,\"price\":5000,\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<OrderItemResponse>> result = queryService.getAll(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getId()).isEqualTo(1L);
        verify(queryRepo, never()).getOrderItems(any());
    }

    // --- getActive ---

    @Test
    void getActive_shouldFetchFromDbWhenCacheMiss() {
        FindAllOrderItems req = FindAllOrderItems.builder().search("").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getOrderItemsActive(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createOrderItem()), 1)));

        Future<PagedResult<OrderItemResponseDeleteAt>> result = queryService.getActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).getOrderItemsActive(req);
    }

    @Test
    void getActive_shouldReturnFromCacheWhenCacheHit() {
        FindAllOrderItems req = FindAllOrderItems.builder().search("").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"orderItemId\":1,\"orderId\":10,\"productId\":100,\"quantity\":2,\"price\":5000,\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<OrderItemResponseDeleteAt>> result = queryService.getActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo, never()).getOrderItemsActive(any());
    }

    // --- getTrashed ---

    @Test
    void getTrashed_shouldFetchFromDbWhenCacheMiss() {
        FindAllOrderItems req = FindAllOrderItems.builder().search("").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getOrderItemsTrashed(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createOrderItem()), 1)));

        Future<PagedResult<OrderItemResponseDeleteAt>> result = queryService.getTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).getOrderItemsTrashed(req);
    }

    @Test
    void getTrashed_shouldReturnFromCacheWhenCacheHit() {
        FindAllOrderItems req = FindAllOrderItems.builder().search("").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"orderItemId\":1,\"orderId\":10,\"productId\":100,\"quantity\":2,\"price\":5000,\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<OrderItemResponseDeleteAt>> result = queryService.getTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo, never()).getOrderItemsTrashed(any());
    }

    // --- getByOrderId ---

    @Test
    void getByOrderId_shouldFetchFromDbWhenCacheMiss() {
        Integer orderId = 10;
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getOrderItemsByOrder(10L)).thenReturn(Future.succeededFuture(List.of(createOrderItem())));

        Future<List<OrderItemResponse>> result = queryService.getByOrderId(orderId);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getId()).isEqualTo(1L);
        verify(queryRepo).getOrderItemsByOrder(10L);
    }

    @Test
    void getByOrderId_shouldReturnFromCacheWhenCacheHit() {
        Integer orderId = 10;
        String json = "[{\"orderItemId\":1,\"orderId\":10,\"productId\":100,\"quantity\":2,\"price\":5000,\"createdAt\":1704067200000,\"updatedAt\":1704067200000}]";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<List<OrderItemResponse>> result = queryService.getByOrderId(orderId);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        verify(queryRepo, never()).getOrderItemsByOrder(any());
    }

    @Test
    void getByOrderId_shouldFailWhenNotFound() {
        Integer orderId = 999;
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getOrderItemsByOrder(999L)).thenReturn(Future.succeededFuture(List.of()));

        Future<List<OrderItemResponse>> result = queryService.getByOrderId(orderId);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }
}
