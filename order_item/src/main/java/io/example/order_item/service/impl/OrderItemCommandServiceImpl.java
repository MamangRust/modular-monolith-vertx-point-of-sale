package io.example.order_item.service.impl;

import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.order_item.domain.requests.CreateOrderItemRequest;
import io.example.order_item.domain.requests.UpdateOrderItemRequest;
import io.example.order_item.model.OrderItem;
import io.example.order_item.model.OrderItemResponse;
import io.example.order_item.model.OrderItemResponseDeleteAt;
import io.example.order_item.repository.OrderItemCommandRepository;
import io.example.order_item.service.OrderItemCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class OrderItemCommandServiceImpl implements OrderItemCommandService {
    private final OrderItemCommandRepository repo;
    private final RedisService redis;
    private final TracingMetrics metrics;

    public OrderItemCommandServiceImpl(
            OrderItemCommandRepository repo,
            RedisService redis,
            TracingMetrics metrics) {
        this.repo = repo;
        this.redis = redis;
        this.metrics = metrics;
    }

    private Future<Void> invalidateCaches() {
        return redis.deleteByPattern("order_items:*")
                .onSuccess(count -> log.debug("Invalidated {} order_items cache keys", count))
                .onFailure(err -> log.warn("Failed to invalidate order_items cache: {}", err.getMessage()))
                .mapEmpty();
    }

    @Override
    public Future<ApiResponse<OrderItemResponse>> create(CreateOrderItemRequest req) {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "OrderItemCommandService.create",
                Attributes.builder()
                        .put("order.id", Objects.requireNonNull(req.getOrderId()))
                        .put("product.id", Objects.requireNonNull(req.getProductId()))
                        .build());
        Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

        log.info("Creating order item for order: {}, product: {}", req.getOrderId(), req.getProductId());

        return repo.createOrderItem(req)
                .compose(created -> invalidateCaches().map(created))
                .map(created -> {
                    span.setAttribute("order_item.id", created.getOrderItemId());
                    metrics.completeSpanSuccess(tracingContext, "create", "Order item created successfully");
                    return ApiResponse.success("Order item created successfully", OrderItemResponse.from(created));
                })
                .recover(err -> {
                    log.error("Failed to create order item", err);
                    metrics.completeSpanError(tracingContext, "create", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to create order item: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<OrderItemResponse>> update(UpdateOrderItemRequest req) {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "OrderItemCommandService.update",
                Attributes.builder()
                        .put("order_item.id", Objects.requireNonNull(req.getOrderItemId()))
                        .build());

        log.info("Updating order item: {}", req.getOrderItemId());

        return repo.updateOrderItem(req)
                .compose(updated -> {
                    if (updated == null) {
                        return Future.failedFuture(new NotFoundException("Order item not found"));
                    }
                    return invalidateCaches().map(updated);
                })
                .map(updated -> {
                    metrics.completeSpanSuccess(tracingContext, "update", "Order item updated successfully");
                    return ApiResponse.success("Order item updated successfully", OrderItemResponse.from(updated));
                })
                .recover(err -> {
                    log.error("Failed to update order item: {}", req.getOrderItemId(), err);
                    metrics.completeSpanError(tracingContext, "update", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to update order item: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<OrderItemResponseDeleteAt>> trash(Long orderItemId) {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "OrderItemCommandService.trash",
                Attributes.builder()
                        .put("order_item.id", Objects.requireNonNull(orderItemId))
                        .build());

        log.info("Trashing order item: {}", orderItemId);

        return repo.trashOrderItem(orderItemId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.failedFuture(new NotFoundException("Order item not found"));
                    }
                    return invalidateCaches().map(trashed);
                })
                .map(trashed -> {
                    metrics.completeSpanSuccess(tracingContext, "trash", "Order item trashed successfully");
                    return ApiResponse.success("Order item trashed successfully", OrderItemResponseDeleteAt.from(trashed));
                })
                .recover(err -> {
                    log.error("Failed to trash order item: {}", orderItemId, err);
                    metrics.completeSpanError(tracingContext, "trash", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to trash order item: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<OrderItemResponseDeleteAt>> restore(Long orderItemId) {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "OrderItemCommandService.restore",
                Attributes.builder()
                        .put("order_item.id", Objects.requireNonNull(orderItemId))
                        .build());

        log.info("Restoring order item: {}", orderItemId);

        return repo.restoreOrderItem(orderItemId)
                .compose(restored -> {
                    if (restored == null) {
                        return Future.failedFuture(new NotFoundException("Order item not found"));
                    }
                    return invalidateCaches().map(restored);
                })
                .map(restored -> {
                    metrics.completeSpanSuccess(tracingContext, "restore", "Order item restored successfully");
                    return ApiResponse.success("Order item restored successfully", OrderItemResponseDeleteAt.from(restored));
                })
                .recover(err -> {
                    log.error("Failed to restore order item: {}", orderItemId, err);
                    metrics.completeSpanError(tracingContext, "restore", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to restore order item: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<Void>> deletePermanent(Long orderItemId) {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "OrderItemCommandService.deletePermanent",
                Attributes.builder()
                        .put("order_item.id", Objects.requireNonNull(orderItemId))
                        .build());

        log.info("Permanently deleting order item: {}", orderItemId);

        return repo.deleteOrderItemPermanently(orderItemId)
                .compose(v -> invalidateCaches())
                .map(v -> {
                    metrics.completeSpanSuccess(tracingContext, "deletePermanent", "Order item permanently deleted");
                    return ApiResponse.<Void>success("Order item permanently deleted", null);
                })
                .recover(err -> {
                    log.error("Failed to permanently delete order item: {}", orderItemId, err);
                    metrics.completeSpanError(tracingContext, "deletePermanent", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to delete order item permanently: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<Void>> restoreAll() {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan("OrderItemCommandService.restoreAll");

        log.info("Restoring all trashed order items");

        return repo.restoreAllOrdersItem()
                .compose(count -> invalidateCaches().map(count))
                .map(count -> {
                    metrics.completeSpanSuccess(tracingContext, "restoreAll", "All order items restored successfully");
                    return ApiResponse.<Void>success("All order items restored successfully (" + count + " items)", null);
                })
                .recover(err -> {
                    log.error("Failed to restore all order items", err);
                    metrics.completeSpanError(tracingContext, "restoreAll", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to restore all order items: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<Void>> deleteAllPermanent() {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan("OrderItemCommandService.deleteAllPermanent");

        log.info("Permanently deleting all trashed order items");

        return repo.deleteAllPermanentOrdersItem()
                .compose(count -> invalidateCaches().map(count))
                .map(count -> {
                    metrics.completeSpanSuccess(tracingContext, "deleteAllPermanent", "All trashed order items permanently deleted successfully");
                    return ApiResponse.<Void>success("All trashed order items permanently deleted successfully (" + count + " items)", null);
                })
                .recover(err -> {
                    log.error("Failed to permanently delete all order items", err);
                    metrics.completeSpanError(tracingContext, "deleteAllPermanent", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to delete all trashed order items permanently: " + err.getMessage()));
                });
    }
}
