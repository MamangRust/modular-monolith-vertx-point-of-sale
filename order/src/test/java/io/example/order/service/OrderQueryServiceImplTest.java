package io.example.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.order.domain.requests.FindAllOrderMerchant;
import io.example.order.domain.requests.FindAllOrders;
import io.example.order.domain.response.order.OrderResponse;
import io.example.order.domain.response.order.OrderResponseDeleteAt;
import io.example.order.model.Order;
import io.example.order.repository.OrderQueryRepository;
import io.example.order.service.impl.OrderQueryServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceImplTest {

    @Mock
    private OrderQueryRepository queryRepository;

    @Mock
    private RedisService redis;

    @Mock
    private TracingMetrics metrics;

    @Mock
    private TracingMetrics.TracingContext tracingContext;

    private OrderQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        lenient().when(metrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redis.setJson(anyString(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));
        queryService = new OrderQueryServiceImpl(queryRepository, redis, metrics);
    }

    private Order createOrder() {
        return Order.builder().orderId(1L).merchantId(1L).cashierId(1L).totalPrice(5000L).build();
    }

    // ── findAll ────────────────────────────────────────────────────────

    @Test
    void findAll_shouldFetchFromDb() {
        FindAllOrders req = FindAllOrders.builder().page(1).pageSize(10).search("test").build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepository.findAllOrders(any())).thenReturn(
                Future.succeededFuture(new PagedResult<>(List.of(createOrder()), 1)));

        Future<PagedResult<OrderResponse>> result = queryService.findAll(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getId()).isEqualTo(1L);
        assertThat(result.result().getData().get(0).getTotalPrice()).isEqualTo(5000L);
        verify(queryRepository).findAllOrders(any());
    }

    // ── findById ──────────────────────────────────────────────────────

    @Test
    void findById_shouldFetchFromDb() {
        Long orderId = 1L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
        when(queryRepository.findById(orderId)).thenReturn(Future.succeededFuture(createOrder()));

        Future<OrderResponse> result = queryService.findById(orderId);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        assertThat(result.result().getTotalPrice()).isEqualTo(5000L);
        verify(queryRepository).findById(orderId);
    }

    @Test
    void findById_shouldReturnFromCache() {
        Long orderId = 1L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(createOrder()));

        Future<OrderResponse> result = queryService.findById(orderId);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        assertThat(result.result().getTotalPrice()).isEqualTo(5000L);
        verify(queryRepository, never()).findById(anyLong());
    }

    // ── findByActive ──────────────────────────────────────────────────

    @Test
    void findByActive_shouldFetchFromDb() {
        FindAllOrders req = FindAllOrders.builder().page(1).pageSize(10).search("active").build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepository.findByActive(any())).thenReturn(
                Future.succeededFuture(new PagedResult<>(List.of(createOrder()), 1)));

        Future<PagedResult<OrderResponseDeleteAt>> result = queryService.findByActive(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getId()).isEqualTo(1L);
        verify(queryRepository).findByActive(any());
    }

    // ── findByTrashed ─────────────────────────────────────────────────

    @Test
    void findByTrashed_shouldFetchFromDb() {
        FindAllOrders req = FindAllOrders.builder().page(1).pageSize(10).search("trashed").build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepository.findByTrashed(any())).thenReturn(
                Future.succeededFuture(new PagedResult<>(List.of(createOrder()), 1)));

        Future<PagedResult<OrderResponseDeleteAt>> result = queryService.findByTrashed(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getId()).isEqualTo(1L);
        verify(queryRepository).findByTrashed(any());
    }

    // ── findByMerchant ────────────────────────────────────────────────

    @Test
    void findByMerchant_shouldFetchFromDb() {
        FindAllOrderMerchant req = FindAllOrderMerchant.builder()
                .page(1).pageSize(10).search("merchant").merchantId(1L).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepository.findByMerchant(any())).thenReturn(
                Future.succeededFuture(new PagedResult<>(List.of(createOrder()), 1)));

        Future<PagedResult<OrderResponse>> result = queryService.findByMerchant(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getId()).isEqualTo(1L);
        verify(queryRepository).findByMerchant(any());
    }
}
