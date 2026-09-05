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

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.order.domain.requests.CreateOrderItemRequest;
import io.example.order.domain.requests.CreateOrderRequest;
import io.example.order.domain.requests.UpdateOrderItemRequest;
import io.example.order.domain.requests.UpdateOrderRequest;
import io.example.order.domain.response.order.OrderResponse;
import io.example.order.domain.response.order.OrderResponseDeleteAt;
import io.example.order.model.Order;
import io.example.order.model.Product;
import io.example.order.repository.CashierQueryRepository;
import io.example.order.repository.MerchantQueryRepository;
import io.example.order.repository.OrderCommandRepository;
import io.example.order.repository.OrderItemCommandRepository;
import io.example.order.repository.OrderItemQueryRepository;
import io.example.order.repository.OrderQueryRepository;
import io.example.order.repository.ProductCommandRepository;
import io.example.order.repository.ProductQueryRepository;
import io.example.order.service.impl.OrderCommandServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class OrderCommandServiceImplTest {

    @Mock
    private OrderCommandRepository orderCommandRepository;

    @Mock
    private OrderQueryRepository orderQueryRepository;

    @Mock
    private OrderItemCommandRepository orderItemCommandRepository;

    @Mock
    private OrderItemQueryRepository orderItemQueryRepository;

    @Mock
    private MerchantQueryRepository merchantQueryRepository;

    @Mock
    private CashierQueryRepository cashierQueryRepository;

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private ProductCommandRepository productCommandRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    @Mock
    private TracingMetrics.TracingContext tracingContext;

    private OrderCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));
        commandService = new OrderCommandServiceImpl(
                orderCommandRepository, orderQueryRepository,
                orderItemCommandRepository, orderItemQueryRepository,
                merchantQueryRepository, cashierQueryRepository,
                productQueryRepository, productCommandRepository,
                redisService, tracingMetrics);
    }

    private Order createOrder() {
        return Order.builder().orderId(1L).merchantId(1L).cashierId(1L).totalPrice(5000L).build();
    }

    private Product createProduct(Long id, int stock, int price) {
        return Product.builder().productId(id).name("Product " + id).price(price).countInStock(stock).build();
    }

    private io.example.order.model.OrderItem createOrderItem(Long orderItemId, Long productId, int qty) {
        return io.example.order.model.OrderItem.builder()
                .orderItemId(orderItemId).orderId(1L).productId(productId).quantity(qty).price(2500)
                .build();
    }

    // ── createOrder (stock compensation) ───────────────────────────────────

    @Test
    void createOrder_shouldCompensateStockWhenItemFails() {
        CreateOrderRequest req = CreateOrderRequest.builder()
                .merchantId(1)
                .cashierId(1)
                .items(List.of(
                        new CreateOrderItemRequest(1L, 2),
                        new CreateOrderItemRequest(2L, 99)))
                .build();

        when(merchantQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(cashierQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(orderCommandRepository.createOrder(any())).thenReturn(Future.succeededFuture(createOrder()));
        // Item 1 passes all checks.
        when(productQueryRepository.getProductById(1L))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(orderItemCommandRepository.createOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(1L).orderId(1L).productId(1L).quantity(2).price(2500)
                        .build()));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        // Item 2 has insufficient stock → the whole order must fail.
        when(productQueryRepository.getProductById(2L))
                .thenReturn(Future.succeededFuture(createProduct(2L, 5, 5000)));
        // Compensation increment for item 1.
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));

        Future<OrderResponse> result = commandService.createOrder(req);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("Insufficient product stock");
        // The stock decremented for item 1 must be rolled back.
        verify(productCommandRepository).incrementStock(1L, 2);
        verify(productCommandRepository, never()).incrementStock(2L, 99);
    }

    @Test
    void createOrder_shouldNotCompensateWhenItSucceeds() {
        CreateOrderRequest req = CreateOrderRequest.builder()
                .merchantId(1)
                .cashierId(1)
                .items(List.of(new CreateOrderItemRequest(1L, 2)))
                .build();

        when(merchantQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(cashierQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(orderCommandRepository.createOrder(any())).thenReturn(Future.succeededFuture(createOrder()));
        when(productQueryRepository.getProductById(1L))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(orderItemCommandRepository.createOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(1L).orderId(1L).productId(1L).quantity(2).price(2500)
                        .build()));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(orderItemQueryRepository.calculateTotalPrice(1L)).thenReturn(Future.succeededFuture(5000));
        when(orderCommandRepository.updateOrder(any())).thenReturn(Future.succeededFuture(createOrder()));

        Future<OrderResponse> result = commandService.createOrder(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : ""))
                .isTrue();
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
    }

    @Test
    void createOrder_shouldCompensateEvenWhenStockDecrementFails() {
        CreateOrderRequest req = CreateOrderRequest.builder()
                .merchantId(1)
                .cashierId(1)
                .items(List.of(
                        new CreateOrderItemRequest(1L, 2),
                        new CreateOrderItemRequest(2L, 1)))
                .build();

        when(merchantQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(cashierQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(orderCommandRepository.createOrder(any())).thenReturn(Future.succeededFuture(createOrder()));
        // Item 1 decrements successfully.
        when(productQueryRepository.getProductById(1L))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(orderItemCommandRepository.createOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(1L).orderId(1L).productId(1L).quantity(2).price(2500)
                        .build()));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        // Item 2 exists with enough stock, but the atomic decrement fails
        // (e.g. concurrent oversell) → order must fail.
        when(productQueryRepository.getProductById(2L))
                .thenReturn(Future.succeededFuture(createProduct(2L, 10, 5000)));
        when(orderItemCommandRepository.createOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(2L).orderId(1L).productId(2L).quantity(1).price(5000)
                        .build()));
        when(productCommandRepository.decrementStock(2L, 1))
                .thenReturn(Future.failedFuture(new BadRequestException("Insufficient product stock")));
        // Compensation increment for item 1.
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));

        Future<OrderResponse> result = commandService.createOrder(req);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        verify(productCommandRepository).incrementStock(1L, 2);
        // Item 2's decrement failed → it must NOT be compensated.
        verify(productCommandRepository, never()).incrementStock(2L, 1);
    }

    @Test
    void createOrder_shouldPreserveOriginalErrorWhenCompensationFails() {
        CreateOrderRequest req = CreateOrderRequest.builder()
                .merchantId(1)
                .cashierId(1)
                .items(List.of(
                        new CreateOrderItemRequest(1L, 2),
                        new CreateOrderItemRequest(2L, 99)))
                .build();

        when(merchantQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(cashierQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(orderCommandRepository.createOrder(any())).thenReturn(Future.succeededFuture(createOrder()));
        when(productQueryRepository.getProductById(1L))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(orderItemCommandRepository.createOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(1L).orderId(1L).productId(1L).quantity(2).price(2500)
                        .build()));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(productQueryRepository.getProductById(2L))
                .thenReturn(Future.succeededFuture(createProduct(2L, 5, 5000)));
        // Compensation itself fails (e.g. product service down) — the original
        // BadRequestException must still win.
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.failedFuture(new RuntimeException("product service down")));

        Future<OrderResponse> result = commandService.createOrder(req);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("Insufficient product stock");
        verify(productCommandRepository).incrementStock(1L, 2);
    }

    // ── updateOrder (stock compensation) ───────────────────────────────────

    @Test
    void updateOrder_shouldCompensateNewItemStockWhenLaterItemFails() {
        UpdateOrderRequest req = UpdateOrderRequest.builder()
                .orderId(1)
                .cashierId(1)
                .items(List.of(
                        // New item: quantity 2 → stock decremented (tracked).
                        new UpdateOrderItemRequest(0L, 1L, 2),
                        // Another new item with insufficient stock → fail.
                        new UpdateOrderItemRequest(0L, 2L, 99)))
                .build();

        when(orderQueryRepository.findById(1L)).thenReturn(Future.succeededFuture(createOrder()));
        when(cashierQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(productQueryRepository.getProductById(1L))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(orderItemCommandRepository.createOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(1L).orderId(1L).productId(1L).quantity(2).price(2500)
                        .build()));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(productQueryRepository.getProductById(2L))
                .thenReturn(Future.succeededFuture(createProduct(2L, 5, 5000)));
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));

        Future<OrderResponse> result = commandService.updateOrder(req);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("Insufficient product stock");
        // Stock decremented for the first new item must be rolled back.
        verify(productCommandRepository).incrementStock(1L, 2);
        verify(productCommandRepository, never()).incrementStock(2L, 99);
    }

    @Test
    void updateOrder_shouldCompensateWhenDecrementFailsForNewItem() {
        UpdateOrderRequest req = UpdateOrderRequest.builder()
                .orderId(1)
                .cashierId(1)
                .items(List.of(
                        // Existing item — updated, no stock change.
                        new UpdateOrderItemRequest(5L, 1L, 3),
                        // New item: stock check passes but atomic decrement fails.
                        new UpdateOrderItemRequest(0L, 2L, 1)))
                .build();

        when(orderQueryRepository.findById(1L)).thenReturn(Future.succeededFuture(createOrder()));
        when(cashierQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(productQueryRepository.getProductById(1L))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(orderItemCommandRepository.updateOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(5L).orderId(1L).productId(1L).quantity(3).price(2500)
                        .build()));
        when(productQueryRepository.getProductById(2L))
                .thenReturn(Future.succeededFuture(createProduct(2L, 10, 5000)));
        when(orderItemCommandRepository.createOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(2L).orderId(1L).productId(2L).quantity(1).price(5000)
                        .build()));
        when(productCommandRepository.decrementStock(2L, 1))
                .thenReturn(Future.failedFuture(new BadRequestException("Insufficient product stock")));

        Future<OrderResponse> result = commandService.updateOrder(req);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        // The failed decrement was never applied → nothing to compensate.
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
        // Existing item was updated before the failure (no rollback for it).
        verify(orderItemCommandRepository).updateOrderItem(any());
    }

    @Test
    void updateOrder_shouldNotCompensateWhenItSucceeds() {
        UpdateOrderRequest req = UpdateOrderRequest.builder()
                .orderId(1)
                .cashierId(1)
                .items(List.of(new UpdateOrderItemRequest(0L, 1L, 2)))
                .build();

        when(orderQueryRepository.findById(1L)).thenReturn(Future.succeededFuture(createOrder()));
        when(cashierQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(productQueryRepository.getProductById(1L))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(orderItemCommandRepository.createOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(1L).orderId(1L).productId(1L).quantity(2).price(2500)
                        .build()));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(orderItemQueryRepository.calculateTotalPrice(1L)).thenReturn(Future.succeededFuture(5000));
        when(orderCommandRepository.updateOrder(any())).thenReturn(Future.succeededFuture(createOrder()));

        Future<OrderResponse> result = commandService.updateOrder(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : ""))
                .isTrue();
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
    }

    @Test
    void updateOrder_shouldPreserveOriginalErrorWhenCompensationFails() {
        UpdateOrderRequest req = UpdateOrderRequest.builder()
                .orderId(1)
                .cashierId(1)
                .items(List.of(
                        // New item 1: stock decremented (tracked).
                        new UpdateOrderItemRequest(0L, 1L, 2),
                        // New item 2: insufficient stock → fail.
                        new UpdateOrderItemRequest(0L, 2L, 99)))
                .build();

        when(orderQueryRepository.findById(1L)).thenReturn(Future.succeededFuture(createOrder()));
        when(cashierQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(productQueryRepository.getProductById(1L))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(orderItemCommandRepository.createOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(1L).orderId(1L).productId(1L).quantity(2).price(2500)
                        .build()));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(productQueryRepository.getProductById(2L))
                .thenReturn(Future.succeededFuture(createProduct(2L, 5, 5000)));
        // Compensation itself fails (e.g. product service down) — the original
        // BadRequestException must still win.
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.failedFuture(new RuntimeException("product service down")));

        Future<OrderResponse> result = commandService.updateOrder(req);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("Insufficient product stock");
        verify(productCommandRepository).incrementStock(1L, 2);
        // Item 2 never had its stock changed → never compensated.
        verify(productCommandRepository, never()).incrementStock(2L, 99);
    }

    @Test
    void createOrder_shouldNotCompensateWhenFailureHappensAfterItemsProcessed() {
        CreateOrderRequest req = CreateOrderRequest.builder()
                .merchantId(1)
                .cashierId(1)
                .items(List.of(new CreateOrderItemRequest(1L, 2)))
                .build();

        when(merchantQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(cashierQueryRepository.existsById(1L)).thenReturn(Future.succeededFuture(true));
        when(orderCommandRepository.createOrder(any())).thenReturn(Future.succeededFuture(createOrder()));
        when(productQueryRepository.getProductById(1L))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(orderItemCommandRepository.createOrderItem(any())).thenReturn(Future.succeededFuture(
                io.example.order.model.OrderItem.builder()
                        .orderItemId(1L).orderId(1L).productId(1L).quantity(2).price(2500)
                        .build()));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        // All items succeeded, but the total recompute fails afterwards — the
        // committed order/items are already in the DB, so stock must NOT be
        // rolled back.
        when(orderItemQueryRepository.calculateTotalPrice(1L))
                .thenReturn(Future.failedFuture(new RuntimeException("postgres down")));

        Future<OrderResponse> result = commandService.createOrder(req);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
    }

    // ── trashedOrder (stock restore + revert compensation) ──────────────

    @Test
    void trashOrder_shouldRestoreStockAndTrash() {
        Long orderId = 1L;
        Order trashed = createOrder();
        when(orderQueryRepository.findById(orderId)).thenReturn(Future.succeededFuture(createOrder()));
        when(orderItemQueryRepository.findOrderItemByOrder(orderId)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2),
                createOrderItem(2L, 2L, 1))));
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(productCommandRepository.incrementStock(2L, 1))
                .thenReturn(Future.succeededFuture(createProduct(2L, 8, 2500)));
        when(orderCommandRepository.trashedOrder(orderId)).thenReturn(Future.succeededFuture(trashed));

        Future<OrderResponseDeleteAt> result = commandService.trashedOrder(orderId);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        assertThat(result.result().getTotalPrice()).isEqualTo(5000L);
        // Stock restored for every active item, then the order trashed.
        verify(productCommandRepository).incrementStock(1L, 2);
        verify(productCommandRepository).incrementStock(2L, 1);
        verify(orderCommandRepository).trashedOrder(orderId);
        // Success → nothing reverted.
        verify(productCommandRepository, never()).decrementStock(anyLong(), anyInt());
        verify(redisService, atLeastOnce()).delete(anyString());
    }

    @Test
    void trashOrder_shouldRevertStockWhenTrashFails() {
        Long orderId = 1L;
        when(orderQueryRepository.findById(orderId)).thenReturn(Future.succeededFuture(createOrder()));
        when(orderItemQueryRepository.findOrderItemByOrder(orderId)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        // Trash fails (order already trashed / not found) AFTER stock restored.
        when(orderCommandRepository.trashedOrder(orderId)).thenReturn(Future.succeededFuture(null));
        // Revert: decrement back the restored stock.
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));

        Future<OrderResponseDeleteAt> result = commandService.trashedOrder(orderId);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).contains("Order not found");
        verify(productCommandRepository).incrementStock(1L, 2);
        // The restored stock must be reverted (decrement back).
        verify(productCommandRepository).decrementStock(1L, 2);
    }

    @Test
    void trashOrder_shouldPreserveOriginalErrorWhenRevertFails() {
        Long orderId = 1L;
        when(orderQueryRepository.findById(orderId)).thenReturn(Future.succeededFuture(createOrder()));
        when(orderItemQueryRepository.findOrderItemByOrder(orderId)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));
        when(orderCommandRepository.trashedOrder(orderId)).thenReturn(Future.succeededFuture(null));
        // Revert itself fails — the original NotFound must still win.
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.failedFuture(new RuntimeException("product service down")));

        Future<OrderResponseDeleteAt> result = commandService.trashedOrder(orderId);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).contains("Order not found");
        verify(productCommandRepository).decrementStock(1L, 2);
    }

    @Test
    void trashOrder_shouldTrashWhenNoActiveItems() {
        Long orderId = 1L;
        Order trashed = createOrder();
        when(orderQueryRepository.findById(orderId)).thenReturn(Future.succeededFuture(createOrder()));
        when(orderItemQueryRepository.findOrderItemByOrder(orderId)).thenReturn(Future.succeededFuture(List.of()));
        when(orderCommandRepository.trashedOrder(orderId)).thenReturn(Future.succeededFuture(trashed));

        Future<OrderResponseDeleteAt> result = commandService.trashedOrder(orderId);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        verify(orderCommandRepository).trashedOrder(orderId);
        // No active items → no stock touched.
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
        verify(productCommandRepository, never()).decrementStock(anyLong(), anyInt());
    }

    @Test
    void trashOrder_shouldFailWhenNotFound() {
        Long orderId = 99L;
        when(orderQueryRepository.findById(orderId)).thenReturn(Future.succeededFuture(null));

        Future<OrderResponseDeleteAt> result = commandService.trashedOrder(orderId);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).contains("Order not found");
        // Nothing fetched/restored when the order does not exist.
        verify(orderQueryRepository).findById(orderId);
        verifyNoInteractions(orderCommandRepository);
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
    }

    // ── restoreOrder (stock decrement + compensation) ──────────────────

    @Test
    void restoreOrder_shouldDecrementStockAndRestore() {
        Long orderId = 1L;
        Order trashed = createOrder();
        when(orderQueryRepository.findByTrashedId(orderId)).thenReturn(Future.succeededFuture(trashed));
        when(orderItemQueryRepository.findOrderItemByOrder(orderId)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(orderCommandRepository.restoreOrder(orderId)).thenReturn(Future.succeededFuture(trashed));

        Future<OrderResponseDeleteAt> result = commandService.restoreOrder(orderId);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        assertThat(result.result().getTotalPrice()).isEqualTo(5000L);
        // Stock decremented again, then the order restored.
        verify(productCommandRepository).decrementStock(1L, 2);
        verify(orderCommandRepository).restoreOrder(orderId);
        // Success → nothing compensated.
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
        verify(redisService, atLeastOnce()).delete(anyString());
    }

    @Test
    void restoreOrder_shouldCompensateStockWhenRestoreFails() {
        Long orderId = 1L;
        when(orderQueryRepository.findByTrashedId(orderId)).thenReturn(Future.succeededFuture(createOrder()));
        when(orderItemQueryRepository.findOrderItemByOrder(orderId)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        // Restore fails AFTER stock was decremented.
        when(orderCommandRepository.restoreOrder(orderId)).thenReturn(Future.succeededFuture(null));
        // Compensation: increment back the decremented stock.
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));

        Future<OrderResponseDeleteAt> result = commandService.restoreOrder(orderId);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        verify(productCommandRepository).decrementStock(1L, 2);
        // The decremented stock must be rolled back (increment).
        verify(productCommandRepository).incrementStock(1L, 2);
    }

    @Test
    void restoreOrder_shouldPreserveOriginalErrorWhenCompensationFails() {
        Long orderId = 1L;
        when(orderQueryRepository.findByTrashedId(orderId)).thenReturn(Future.succeededFuture(createOrder()));
        when(orderItemQueryRepository.findOrderItemByOrder(orderId)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(orderCommandRepository.restoreOrder(orderId)).thenReturn(Future.succeededFuture(null));
        // Compensation itself fails — the original NotFound must still win.
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.failedFuture(new RuntimeException("product service down")));

        Future<OrderResponseDeleteAt> result = commandService.restoreOrder(orderId);

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).contains("Order not found");
        verify(productCommandRepository).incrementStock(1L, 2);
    }

    @Test
    void restoreOrder_shouldRestoreWhenNoActiveItems() {
        Long orderId = 1L;
        Order trashed = createOrder();
        when(orderQueryRepository.findByTrashedId(orderId)).thenReturn(Future.succeededFuture(trashed));
        when(orderItemQueryRepository.findOrderItemByOrder(orderId)).thenReturn(Future.succeededFuture(List.of()));
        when(orderCommandRepository.restoreOrder(orderId)).thenReturn(Future.succeededFuture(trashed));

        Future<OrderResponseDeleteAt> result = commandService.restoreOrder(orderId);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        verify(orderCommandRepository).restoreOrder(orderId);
        // No active items → no stock touched.
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
        verify(productCommandRepository, never()).decrementStock(anyLong(), anyInt());
    }

    @Test
    void restoreOrder_shouldFailWhenNotTrashed() {
        Long orderId = 99L;
        when(orderQueryRepository.findByTrashedId(orderId)).thenReturn(Future.succeededFuture(null));

        Future<OrderResponseDeleteAt> result = commandService.restoreOrder(orderId);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("must be trashed first");
        verify(orderQueryRepository).findByTrashedId(orderId);
        verifyNoInteractions(orderCommandRepository);
    }

    // ── restoreAllOrder (per-order stock symmetry, race-free) ───────────

    @Test
    void restoreAllOrder_shouldRestoreAll() {
        Order trashed1 = Order.builder().orderId(1L).merchantId(1L).cashierId(1L).totalPrice(5000L).build();
        Order trashed2 = Order.builder().orderId(2L).merchantId(1L).cashierId(1L).totalPrice(7000L).build();
        when(orderQueryRepository.findAllTrashed()).thenReturn(Future.succeededFuture(List.of(trashed1, trashed2)));
        when(orderItemQueryRepository.findOrderItemByOrder(1L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(orderItemQueryRepository.findOrderItemByOrder(2L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(2L, 2L, 1))));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(productCommandRepository.decrementStock(2L, 1))
                .thenReturn(Future.succeededFuture(createProduct(2L, 9, 2500)));
        // Per-order atomic restore (UPDATE ... WHERE deleted_at IS NOT NULL).
        when(orderCommandRepository.restoreOrder(1L)).thenReturn(Future.succeededFuture(trashed1));
        when(orderCommandRepository.restoreOrder(2L)).thenReturn(Future.succeededFuture(trashed2));

        Future<Boolean> result = commandService.restoreAllOrder();

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result()).isTrue();
        // Each trashed order's active items decremented, then restored one-by-one.
        verify(productCommandRepository).decrementStock(1L, 2);
        verify(productCommandRepository).decrementStock(2L, 1);
        verify(orderCommandRepository).restoreOrder(1L);
        verify(orderCommandRepository).restoreOrder(2L);
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
        verify(redisService).deleteByPattern("order:list:*");
    }

    @Test
    void restoreAllOrder_shouldCompensateStockWhenDecrementFails() {
        Order trashed1 = Order.builder().orderId(1L).merchantId(1L).cashierId(1L).totalPrice(5000L).build();
        Order trashed2 = Order.builder().orderId(2L).merchantId(1L).cashierId(1L).totalPrice(7000L).build();
        when(orderQueryRepository.findAllTrashed()).thenReturn(Future.succeededFuture(List.of(trashed1, trashed2)));
        when(orderItemQueryRepository.findOrderItemByOrder(1L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(orderItemQueryRepository.findOrderItemByOrder(2L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(2L, 2L, 1))));
        // Order 1 decrements + restores fine, order 2's atomic decrement fails.
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(orderCommandRepository.restoreOrder(1L)).thenReturn(Future.succeededFuture(trashed1));
        when(productCommandRepository.decrementStock(2L, 1))
                .thenReturn(Future.failedFuture(new BadRequestException("Insufficient product stock")));

        Future<Boolean> result = commandService.restoreAllOrder();

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("Insufficient product stock");
        // Order 1 stays restored (partial completion); order 2's failed decrement
        // was never applied so nothing is compensated.
        verify(orderCommandRepository).restoreOrder(1L);
        verify(orderCommandRepository, never()).restoreOrder(2L);
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
    }

    @Test
    void restoreAllOrder_shouldSkipWhenAlreadyRestoredConcurrently() {
        // Race: between findAllTrashed() and our restore, another request
        // restored order 1 — restoreOrder returns null, so we undo our own
        // decrement and skip (not an error). Order 2 is still restored by us.
        Order trashed1 = Order.builder().orderId(1L).merchantId(1L).cashierId(1L).totalPrice(5000L).build();
        Order trashed2 = Order.builder().orderId(2L).merchantId(1L).cashierId(1L).totalPrice(7000L).build();
        when(orderQueryRepository.findAllTrashed()).thenReturn(Future.succeededFuture(List.of(trashed1, trashed2)));
        when(orderItemQueryRepository.findOrderItemByOrder(1L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(orderItemQueryRepository.findOrderItemByOrder(2L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(2L, 2L, 1))));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(productCommandRepository.decrementStock(2L, 1))
                .thenReturn(Future.succeededFuture(createProduct(2L, 9, 2500)));
        when(orderCommandRepository.restoreOrder(1L)).thenReturn(Future.succeededFuture(null)); // lost race
        when(orderCommandRepository.restoreOrder(2L)).thenReturn(Future.succeededFuture(trashed2));
        // Undo our decrement for the concurrently-restored order 1.
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));

        Future<Boolean> result = commandService.restoreAllOrder();

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result()).isTrue();
        // No double-decrement: the lost-race decrement is undone.
        verify(productCommandRepository).incrementStock(1L, 2);
        verify(orderCommandRepository).restoreOrder(2L);
    }

    @Test
    void restoreAllOrder_shouldFailWhenAllSkippedConcurrently() {
        // Every order was concurrently restored before this call could claim
        // any of them → nothing restored by us → NotFound.
        Order trashed1 = Order.builder().orderId(1L).merchantId(1L).cashierId(1L).totalPrice(5000L).build();
        when(orderQueryRepository.findAllTrashed()).thenReturn(Future.succeededFuture(List.of(trashed1)));
        when(orderItemQueryRepository.findOrderItemByOrder(1L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(orderCommandRepository.restoreOrder(1L)).thenReturn(Future.succeededFuture(null));
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 10, 2500)));

        Future<Boolean> result = commandService.restoreAllOrder();

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).isEqualTo("No trashed orders found");
        // The lost-race decrement is still undone even though we fail overall.
        verify(productCommandRepository).incrementStock(1L, 2);
    }

    @Test
    void restoreAllOrder_shouldPreserveOriginalErrorWhenCompensationFails() {
        // Order 2 has two items: the first decrements (tracked), the second
        // fails with the REAL error, and compensating order 2 also fails — the
        // original BadRequest must still win.
        Order trashed1 = Order.builder().orderId(1L).merchantId(1L).cashierId(1L).totalPrice(5000L).build();
        Order trashed2 = Order.builder().orderId(2L).merchantId(1L).cashierId(1L).totalPrice(7000L).build();
        when(orderQueryRepository.findAllTrashed()).thenReturn(Future.succeededFuture(List.of(trashed1, trashed2)));
        when(orderItemQueryRepository.findOrderItemByOrder(1L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(orderItemQueryRepository.findOrderItemByOrder(2L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(2L, 2L, 1),
                createOrderItem(3L, 3L, 1))));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(orderCommandRepository.restoreOrder(1L)).thenReturn(Future.succeededFuture(trashed1));
        // Order 2: first item's decrement succeeds, second fails (real error).
        when(productCommandRepository.decrementStock(2L, 1))
                .thenReturn(Future.succeededFuture(createProduct(2L, 9, 2500)));
        when(productCommandRepository.decrementStock(3L, 1))
                .thenReturn(Future.failedFuture(new BadRequestException("Insufficient product stock")));
        // Compensation for order 2's applied decrement fails too — swallowed,
        // the original BadRequest must still win.
        when(productCommandRepository.incrementStock(2L, 1))
                .thenReturn(Future.failedFuture(new RuntimeException("product service down")));

        Future<Boolean> result = commandService.restoreAllOrder();

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("Insufficient product stock");
        // Compensation was attempted for order 2's applied decrement.
        verify(productCommandRepository).incrementStock(2L, 1);
        verify(productCommandRepository, never()).incrementStock(3L, 1);
        // Order 1 stays restored (partial completion), order 2 never restored.
        verify(orderCommandRepository).restoreOrder(1L);
        verify(orderCommandRepository, never()).restoreOrder(2L);
    }

    @Test
    void restoreAllOrder_shouldFailWhenUndoAfterConcurrentRestoreFails() {
        // Order 1 loses the race (restoreOrder null) and undoing OUR decrement
        // fails — stock would be inconsistent, so the whole call must FAIL
        // instead of silently reporting success.
        Order trashed1 = Order.builder().orderId(1L).merchantId(1L).cashierId(1L).totalPrice(5000L).build();
        when(orderQueryRepository.findAllTrashed()).thenReturn(Future.succeededFuture(List.of(trashed1)));
        when(orderItemQueryRepository.findOrderItemByOrder(1L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(orderCommandRepository.restoreOrder(1L)).thenReturn(Future.succeededFuture(null)); // lost race
        // Undo of order 1's decrement fails → stock inconsistent → fail.
        when(productCommandRepository.incrementStock(1L, 2))
                .thenReturn(Future.failedFuture(new RuntimeException("product service down")));

        Future<Boolean> result = commandService.restoreAllOrder();

        assertThat(result.failed()).as("Expected failure: " + result.cause()).isTrue();
        assertThat(result.cause()).isInstanceOf(IllegalStateException.class);
        assertThat(result.cause().getMessage()).contains("Failed to undo stock");
        // Nothing further happens after order 1's undo failure.
        verify(orderCommandRepository, never()).restoreOrder(2L);
    }

    @Test
    void restoreAllOrder_shouldRestoreOrdersWithoutActiveItems() {
        // One order has active items, another has none — the empty one must
        // not touch stock, both must still be restored.
        Order trashed1 = Order.builder().orderId(1L).merchantId(1L).cashierId(1L).totalPrice(5000L).build();
        Order trashed2 = Order.builder().orderId(2L).merchantId(1L).cashierId(1L).totalPrice(7000L).build();
        when(orderQueryRepository.findAllTrashed()).thenReturn(Future.succeededFuture(List.of(trashed1, trashed2)));
        when(orderItemQueryRepository.findOrderItemByOrder(1L)).thenReturn(Future.succeededFuture(List.of(
                createOrderItem(1L, 1L, 2))));
        when(orderItemQueryRepository.findOrderItemByOrder(2L)).thenReturn(Future.succeededFuture(List.of()));
        when(productCommandRepository.decrementStock(1L, 2))
                .thenReturn(Future.succeededFuture(createProduct(1L, 8, 2500)));
        when(orderCommandRepository.restoreOrder(1L)).thenReturn(Future.succeededFuture(trashed1));
        when(orderCommandRepository.restoreOrder(2L)).thenReturn(Future.succeededFuture(trashed2));

        Future<Boolean> result = commandService.restoreAllOrder();

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result()).isTrue();
        verify(productCommandRepository).decrementStock(1L, 2);
        // Order 2 has no active items → no stock interaction for it.
        verify(productCommandRepository, never()).decrementStock(eq(2L), anyInt());
        verify(orderCommandRepository).restoreOrder(2L);
        verify(productCommandRepository, never()).incrementStock(anyLong(), anyInt());
    }

    @Test
    void restoreAllOrder_shouldFailWhenNoneTrashed() {
        when(orderQueryRepository.findAllTrashed()).thenReturn(Future.succeededFuture(List.of()));

        Future<Boolean> result = commandService.restoreAllOrder();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).isEqualTo("No trashed orders found");
        verify(orderQueryRepository).findAllTrashed();
        // Nothing decremented / restored when there are no trashed orders.
        verifyNoInteractions(orderCommandRepository);
        verify(productCommandRepository, never()).decrementStock(anyLong(), anyInt());
    }

    // ── deleteAllOrderPermanent ───────────────────────────────────────

    @Test
    void deleteAllOrderPermanent_shouldDeleteAll() {
        when(orderCommandRepository.deleteAllOrderPermanent()).thenReturn(Future.succeededFuture(3));

        Future<Boolean> result = commandService.deleteAllOrderPermanent();

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result()).isTrue();
        verify(orderCommandRepository).deleteAllOrderPermanent();
        verify(redisService).deleteByPattern("order:list:*");
    }

    @Test
    void deleteAllOrderPermanent_shouldFailWhenNoneTrashed() {
        when(orderCommandRepository.deleteAllOrderPermanent()).thenReturn(Future.succeededFuture(0));

        Future<Boolean> result = commandService.deleteAllOrderPermanent();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).isEqualTo("No trashed orders found");
        verify(orderCommandRepository).deleteAllOrderPermanent();
    }
}
