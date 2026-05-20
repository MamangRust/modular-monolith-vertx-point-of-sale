package io.example.order.service.impl;

import java.util.List;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.order.domain.requests.CreateOrderItemRecordRequest;
import io.example.order.domain.requests.CreateOrderItemRequest;
import io.example.order.domain.requests.CreateOrderRecordRequest;
import io.example.order.domain.requests.CreateOrderRequest;
import io.example.order.domain.requests.UpdateOrderItemRecordRequest;
import io.example.order.domain.requests.UpdateOrderItemRequest;
import io.example.order.domain.requests.UpdateOrderRecordRequest;
import io.example.order.domain.requests.UpdateOrderRequest;
import io.example.order.model.Order;
import io.example.order.repository.CashierQueryRepository;
import io.example.order.repository.MerchantQueryRepository;
import io.example.order.repository.OrderCommandRepository;
import io.example.order.repository.OrderItemCommandRepository;
import io.example.order.repository.OrderItemQueryRepository;
import io.example.order.repository.OrderQueryRepository;
import io.example.order.repository.ProductCommandRepository;
import io.example.order.repository.ProductQueryRepository;
import io.example.order.service.OrderCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderCommandServiceImpl implements OrderCommandService {
    private final OrderCommandRepository orderCommandRepository;
    private final OrderQueryRepository orderQueryRepository;
    private final OrderItemCommandRepository orderItemCommandRepository;
    private final OrderItemQueryRepository orderItemQueryRepository;
    private final MerchantQueryRepository merchantQueryRepository;
    private final CashierQueryRepository cashierQueryRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductCommandRepository productCommandRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    public OrderCommandServiceImpl(
            OrderCommandRepository orderCommandRepository,
            OrderQueryRepository orderQueryRepository,
            OrderItemCommandRepository orderItemCommandRepository,
            OrderItemQueryRepository orderItemQueryRepository,
            MerchantQueryRepository merchantQueryRepository,
            CashierQueryRepository cashierQueryRepository,
            ProductQueryRepository productQueryRepository,
            ProductCommandRepository productCommandRepository,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.orderCommandRepository = orderCommandRepository;
        this.orderQueryRepository = orderQueryRepository;
        this.orderItemCommandRepository = orderItemCommandRepository;
        this.orderItemQueryRepository = orderItemQueryRepository;
        this.merchantQueryRepository = merchantQueryRepository;
        this.cashierQueryRepository = cashierQueryRepository;
        this.productQueryRepository = productQueryRepository;
        this.productCommandRepository = productCommandRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    private void invalidateCache(Long orderId) {
        if (orderId != null) {
            redisService.delete("order:detail:" + orderId);
        }
        redisService.delete("orders:list:");
    }

    private Future<Void> createAndProcessItem(Long orderId, CreateOrderItemRequest item) {
        return productQueryRepository.getProductById((long) item.getProductId())
                .<Void>compose(product -> {
                    if (product == null) {
                        return Future.<Void>failedFuture("Product not found");
                    }
                    if (product.getCountInStock() < item.getQuantity()) {
                        return Future.<Void>failedFuture("Insufficient product stock");
                    }

                    return orderItemCommandRepository.createOrderItem(
                            new CreateOrderItemRecordRequest(
                                    orderId,
                                    (long) item.getProductId(),
                                    item.getQuantity(),
                                    product.getPrice()))
                            .compose(orderItem -> {
                                int newStock = product.getCountInStock() - item.getQuantity();
                                return productCommandRepository.updateProductCountStock(product.getProductId(),
                                        newStock);
                            })
                            .mapEmpty();
                });
    }

    private Future<Void> processOrderItems(Order order, List<CreateOrderItemRequest> items) {
        Future<Void> future = Future.succeededFuture();
        for (CreateOrderItemRequest item : items) {
            future = future.compose(v -> createAndProcessItem(order.getOrderId(), item));
        }
        return future;
    }

    private Future<Void> updateOrCreateItem(Long orderId, UpdateOrderItemRequest item) {
        return productQueryRepository.getProductById((long) item.getProductId())
                .<Void>compose(product -> {
                    if (product == null) {
                        return Future.<Void>failedFuture("Product not found");
                    }

                    if (item.getOrderItemId() > 0) {
                        return orderItemCommandRepository.updateOrderItem(
                                new UpdateOrderItemRecordRequest(
                                        (long) item.getOrderItemId(),
                                        (long) item.getOrderItemId(),
                                        (long) item.getProductId(),
                                        item.getQuantity(),
                                        product.getPrice()))
                                .mapEmpty();
                    } else {
                        if (product.getCountInStock() < item.getQuantity()) {
                            return Future.<Void>failedFuture("Insufficient product stock");
                        }

                        return orderItemCommandRepository.createOrderItem(
                                new CreateOrderItemRecordRequest(
                                        orderId,
                                        (long) item.getProductId(),
                                        item.getQuantity(),
                                        product.getPrice()))
                                .compose(v -> {
                                    int newStock = product.getCountInStock() - item.getQuantity();
                                    return productCommandRepository.updateProductCountStock(product.getProductId(),
                                            newStock);
                                })
                                .mapEmpty();
                    }
                });
    }

    private Future<Void> processUpdateOrderItems(Long orderId, List<UpdateOrderItemRequest> items) {
        Future<Void> future = Future.succeededFuture();
        for (UpdateOrderItemRequest item : items) {
            future = future.compose(v -> updateOrCreateItem(orderId, item));
        }
        return future;
    }

    @Override
    public Future<Order> createOrder(CreateOrderRequest req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan(
                "OrderService.createOrder",
                Attributes.builder()
                        .put("merchant.id", req.getMerchantId())
                        .put("cashier.id", req.getCashierId())
                        .build());
        Span span = Span.fromContext(java.util.Objects.requireNonNull(tracingCtx.getContext()));

        log.info("Creating order for merchant: {} cashier: {}", req.getMerchantId(), req.getCashierId());

        return merchantQueryRepository.existsById(req.getMerchantId())
                .compose(merchantExists -> {
                    if (!merchantExists) {
                        return Future.failedFuture("Merchant not found");
                    }
                    return cashierQueryRepository.existsById(req.getCashierId());
                })
                .compose(cashierExists -> {
                    if (!cashierExists) {
                        return Future.failedFuture("Cashier not found");
                    }
                    return orderCommandRepository.createOrder(
                            new CreateOrderRecordRequest((long) req.getMerchantId(),
                                    (long) req.getCashierId(), 0));
                })
                .compose(order -> processOrderItems(order, req.getItems()).map(v -> order))
                .compose(order -> orderItemQueryRepository.calculateTotalPrice(order.getOrderId())
                        .compose(totalPrice -> orderCommandRepository.updateOrder(
                                new UpdateOrderRecordRequest(order.getOrderId(), totalPrice))))
                .map(updatedOrder -> {
                    span.setAttribute("order.id", updatedOrder.getOrderId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "create", "Order created successfully");
                    invalidateCache(updatedOrder.getOrderId());
                    return updatedOrder;
                })
                .recover(err -> {
                    log.error("Failed to create order", err);
                    tracingMetrics.completeSpanError(tracingCtx, "create", err.getMessage());
                    return Future.failedFuture(err);
                });
    }

    @Override
    public Future<Order> updateOrder(UpdateOrderRequest req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan(
                "OrderService.updateOrder",
                Attributes.builder()
                        .put("order.id", req.getOrderId())
                        .build());
        Span span = Span.fromContext(java.util.Objects.requireNonNull(tracingCtx.getContext()));

        log.info("Updating order: {}", req.getOrderId());

        return orderQueryRepository.findById((long) req.getOrderId())
                .compose(order -> {
                    if (order == null) {
                        return Future.failedFuture("Order not found");
                    }
                    return cashierQueryRepository.existsById(req.getCashierId());
                })
                .compose(cashierExists -> {
                    if (!cashierExists) {
                        return Future.failedFuture("Cashier not found");
                    }
                    return processUpdateOrderItems((long) req.getOrderId(), req.getItems());
                })
                .compose(v -> orderItemQueryRepository.calculateTotalPrice((long) req.getOrderId())
                        .compose(totalPrice -> orderCommandRepository.updateOrder(
                                new UpdateOrderRecordRequest((long) req.getOrderId(), totalPrice))))
                .map(updatedOrder -> {
                    span.setAttribute("order.id", updatedOrder.getOrderId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "update", "Order updated successfully");
                    invalidateCache(updatedOrder.getOrderId());
                    return updatedOrder;
                })
                .recover(err -> {
                    log.error("Failed to update order", err);
                    tracingMetrics.completeSpanError(tracingCtx, "update", err.getMessage());
                    return Future.failedFuture(err);
                });
    }

    @Override
    public Future<Order> trashedOrder(int orderId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan(
                "OrderService.trashedOrder",
                Attributes.builder().put("order.id", orderId).build());

        return orderCommandRepository.trashedOrder((long) orderId)
                .map(order -> {
                    if (order == null) {
                        throw new RuntimeException("Order not found or already trashed");
                    }
                    invalidateCache((long) orderId);
                    tracingMetrics.completeSpanSuccess(tracingCtx, "trash_order", "Order trashed");
                    return order;
                })
                .recover(err -> {
                    tracingMetrics.completeSpanError(tracingCtx, "trash_order", err.getMessage());
                    return Future.failedFuture(err);
                });
    }

    @Override
    public Future<Order> restoreOrder(int orderId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan(
                "OrderService.restoreOrder",
                Attributes.builder().put("order.id", orderId).build());

        return orderCommandRepository.restoreOrder((long) orderId)
                .map(order -> {
                    if (order == null) {
                        throw new RuntimeException("Order not found or not in trash");
                    }
                    invalidateCache((long) orderId);
                    tracingMetrics.completeSpanSuccess(tracingCtx, "restore_order", "Order restored");
                    return order;
                })
                .recover(err -> {
                    tracingMetrics.completeSpanError(tracingCtx, "restore_order", err.getMessage());
                    return Future.failedFuture(err);
                });
    }

    @Override
    public Future<Boolean> deleteOrderPermanent(int orderId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan(
                "OrderService.deleteOrderPermanently",
                Attributes.builder().put("order.id", orderId).build());

        log.info("Permanently deleting order: {}", orderId);

        return orderCommandRepository.deleteOrderPermanent((long) orderId)
                .map(res -> {
                    invalidateCache((long) orderId);
                    tracingMetrics.completeSpanSuccess(tracingCtx, "delete_permanent", "Order deleted permanently");
                    return res;
                })
                .recover(err -> {
                    tracingMetrics.completeSpanError(tracingCtx, "delete_permanent", err.getMessage());
                    return Future.failedFuture(err);
                });
    }

    @Override
    public Future<Boolean> restoreAllOrder() {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("OrderService.restoreAllOrders");
        return orderCommandRepository.restoreAllOrder()
                .map(res -> {
                    tracingMetrics.completeSpanSuccess(tracingCtx, "restore_all", "Success");
                    return res;
                })
                .recover(err -> {
                    tracingMetrics.completeSpanError(tracingCtx, "restore_all", err.getMessage());
                    return Future.failedFuture(err);
                });
    }

    @Override
    public Future<Boolean> deleteAllOrderPermanent() {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("OrderService.deleteAllPermanentOrders");
        return orderCommandRepository.deleteAllOrderPermanent()
                .map(res -> {
                    tracingMetrics.completeSpanSuccess(tracingCtx, "delete_all", "Success");
                    return res;
                })
                .recover(err -> {
                    tracingMetrics.completeSpanError(tracingCtx, "delete_all", err.getMessage());
                    return Future.failedFuture(err);
                });
    }
}
