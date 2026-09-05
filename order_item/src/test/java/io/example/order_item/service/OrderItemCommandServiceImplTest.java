package io.example.order_item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.order_item.domain.requests.CreateOrderItemRequest;
import io.example.order_item.domain.requests.UpdateOrderItemRequest;
import io.example.order_item.domain.response.order_item.OrderItemResponse;
import io.example.order_item.domain.response.order_item.OrderItemResponseDeleteAt;
import io.example.order_item.model.OrderItem;
import io.example.order_item.repository.OrderItemCommandRepository;
import io.example.order_item.repository.OrderItemQueryRepository;
import io.example.order_item.service.impl.OrderItemCommandServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class OrderItemCommandServiceImplTest {

    @Mock private OrderItemCommandRepository commandRepo;
    @Mock private OrderItemQueryRepository queryRepo;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private OrderItemCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        commandService = new OrderItemCommandServiceImpl(
                commandRepo, queryRepo, redisService, tracingMetrics);
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

    // --- create ---

    @Test
    void create_shouldCreateSuccessfully() {
        CreateOrderItemRequest req = CreateOrderItemRequest.builder()
                .orderId(10L)
                .productId(100L)
                .quantity(2)
                .price(5000)
                .build();

        when(commandRepo.createOrderItem(req)).thenReturn(Future.succeededFuture(createOrderItem()));

        Future<OrderItemResponse> result = commandService.create(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        assertThat(result.result().getOrderId()).isEqualTo(10);
        assertThat(result.result().getQuantity()).isEqualTo(2);
        assertThat(result.result().getPrice()).isEqualTo(5000);
        verify(redisService, times(2)).deleteByPattern("order_item:list:*");
    }

    // --- update ---

    @Test
    void update_shouldUpdateSuccessfully() {
        UpdateOrderItemRequest req = UpdateOrderItemRequest.builder()
                .orderItemId(1L)
                .orderId(10L)
                .productId(100L)
                .quantity(5)
                .price(7500)
                .build();

        OrderItem updated = createOrderItem();
        updated.setQuantity(5);
        updated.setPrice(7500);

        when(commandRepo.updateOrderItem(req)).thenReturn(Future.succeededFuture(updated));

        Future<OrderItemResponse> result = commandService.update(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getQuantity()).isEqualTo(5);
        assertThat(result.result().getPrice()).isEqualTo(7500);
        verify(redisService, times(2)).deleteByPattern("order_item:list:*");
    }

    @Test
    void update_shouldFailWhenNotFound() {
        UpdateOrderItemRequest req = UpdateOrderItemRequest.builder()
                .orderItemId(999L)
                .orderId(10L)
                .productId(100L)
                .quantity(5)
                .price(7500)
                .build();

        when(commandRepo.updateOrderItem(req)).thenReturn(Future.succeededFuture(null));

        Future<OrderItemResponse> result = commandService.update(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- trash ---

    @Test
    void trash_shouldTrashSuccessfully() {
        Long id = 1L;
        OrderItem trashed = createOrderItem();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(commandRepo.trashOrderItem(id)).thenReturn(Future.succeededFuture(trashed));

        Future<OrderItemResponseDeleteAt> result = commandService.trash(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
        verify(redisService).delete("order_item:1");
    }

    @Test
    void trash_shouldFailWhenNotFound() {
        Long id = 999L;
        when(commandRepo.trashOrderItem(id)).thenReturn(Future.succeededFuture(null));

        Future<OrderItemResponseDeleteAt> result = commandService.trash(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- restore ---

    @Test
    void restore_shouldRestoreSuccessfully() {
        Long id = 1L;
        OrderItem trashed = createOrderItem();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
        OrderItem restored = createOrderItem();

        when(queryRepo.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepo.restoreOrderItem(id)).thenReturn(Future.succeededFuture(restored));

        Future<OrderItemResponseDeleteAt> result = commandService.restore(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNull();
        verify(redisService).delete("order_item:1");
    }

    @Test
    void restore_shouldFailWhenNotTrashed() {
        Long id = 1L;
        when(queryRepo.findByTrashedId(id)).thenReturn(Future.succeededFuture(null));

        Future<OrderItemResponseDeleteAt> result = commandService.restore(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
    }

    // --- deletePermanent ---

    @Test
    void deletePermanent_shouldDeleteSuccessfully() {
        Long id = 1L;
        OrderItem trashed = createOrderItem();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(queryRepo.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepo.deleteOrderItemPermanently(id)).thenReturn(Future.succeededFuture(true));

        Future<Void> result = commandService.deletePermanent(id);

        assertThat(result.succeeded()).isTrue();
        verify(redisService).delete("order_item:1");
    }

    @Test
    void deletePermanent_shouldFailWhenNotTrashed() {
        Long id = 1L;
        when(queryRepo.findByTrashedId(id)).thenReturn(Future.succeededFuture(null));

        Future<Void> result = commandService.deletePermanent(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
    }

    @Test
    void deletePermanent_shouldFailWhenRepoFails() {
        Long id = 1L;
        OrderItem trashed = createOrderItem();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(queryRepo.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepo.deleteOrderItemPermanently(id)).thenReturn(Future.succeededFuture(false));

        Future<Void> result = commandService.deletePermanent(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
    }

    // --- restoreAll ---

    @Test
    void restoreAll_shouldRestoreAll() {
        when(commandRepo.restoreAllOrdersItem()).thenReturn(Future.succeededFuture(5));

        Future<Void> result = commandService.restoreAll();

        assertThat(result.succeeded()).isTrue();
        verify(redisService, times(2)).deleteByPattern("order_item:list:*");
    }

    @Test
    void restoreAll_shouldFailWhenNoneTrashed() {
        when(commandRepo.restoreAllOrdersItem()).thenReturn(Future.succeededFuture(0));

        Future<Void> result = commandService.restoreAll();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- deleteAllPermanent ---

    @Test
    void deleteAllPermanent_shouldDeleteAll() {
        when(commandRepo.deleteAllPermanentOrdersItem()).thenReturn(Future.succeededFuture(3));

        Future<Void> result = commandService.deleteAllPermanent();

        assertThat(result.succeeded()).isTrue();
        verify(redisService, times(2)).deleteByPattern("order_item:list:*");
    }

    @Test
    void deleteAllPermanent_shouldFailWhenNoneTrashed() {
        when(commandRepo.deleteAllPermanentOrdersItem()).thenReturn(Future.succeededFuture(0));

        Future<Void> result = commandService.deleteAllPermanent();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }
}
