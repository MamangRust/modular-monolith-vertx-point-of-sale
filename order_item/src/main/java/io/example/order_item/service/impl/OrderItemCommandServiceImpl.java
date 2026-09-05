package io.example.order_item.service.impl;

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
import io.example.order_item.service.OrderItemCommandService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OrderItemCommandServiceImpl implements OrderItemCommandService {
    private final OrderItemCommandRepository repo;
    private final OrderItemQueryRepository queryRepo;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "order_item:";
    private static final String CACHE_LIST_PREFIX = "order_item:list:";

    @Override
    public Future<OrderItemResponse> create(CreateOrderItemRequest req) {
        var ctx = tracingMetrics.startSpan("OrderItemCommandService.create",
                Attributes.builder()
                        .put("order.id", req.getOrderId())
                        .put("product.id", req.getProductId())
                        .build());

        return repo.createOrderItem(req)
                .compose(created -> invalidateListCache().<OrderItem>map(v -> created))
                .map(OrderItemResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "create", "Order item created successfully"))
                .onFailure(err -> {
                    log.error("Failed to create order item", err);
                    tracingMetrics.completeSpanError(ctx, "create", err.getMessage());
                });
    }

    @Override
    public Future<OrderItemResponse> update(UpdateOrderItemRequest req) {
        var ctx = tracingMetrics.startSpan("OrderItemCommandService.update",
                Attributes.builder().put("order_item.id", req.getOrderItemId()).build());

        return repo.updateOrderItem(req)
                .compose(updated -> {
                    if (updated == null) {
                        return Future.failedFuture(new NotFoundException("Order item not found"));
                    }
                    return invalidateListCache().<OrderItem>map(v -> updated);
                })
                .map(OrderItemResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update", "Order item updated successfully"))
                .onFailure(err -> {
                    log.error("Failed to update order item: {}", req.getOrderItemId(), err);
                    tracingMetrics.completeSpanError(ctx, "update", err.getMessage());
                });
    }

    @Override
    public Future<OrderItemResponseDeleteAt> trash(Long orderItemId) {
        var ctx = tracingMetrics.startSpan("OrderItemCommandService.trash",
                Attributes.builder().put("order_item.id", orderItemId).build());

        return repo.trashOrderItem(orderItemId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.failedFuture(new NotFoundException("Order item not found"));
                    }
                    return invalidateCache(orderItemId).<OrderItem>map(v -> trashed);
                })
                .map(OrderItemResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "trash", "Order item trashed successfully"))
                .onFailure(err -> {
                    log.error("Failed to trash order item: {}", orderItemId, err);
                    tracingMetrics.completeSpanError(ctx, "trash", err.getMessage());
                });
    }

    @Override
    public Future<OrderItemResponseDeleteAt> restore(Long orderItemId) {
        var ctx = tracingMetrics.startSpan("OrderItemCommandService.restore",
                Attributes.builder().put("order_item.id", orderItemId).build());

        return queryRepo.findByTrashedId(orderItemId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.failedFuture(
                                new BadRequestException("Order item not found or must be trashed first"));
                    }
                    return repo.restoreOrderItem(orderItemId);
                })
                .compose(r -> {
                    if (r == null) {
                        return Future.<OrderItem>failedFuture(new NotFoundException("Order item not found"));
                    }
                    return invalidateCache(orderItemId).<OrderItem>map(v -> r);
                })
                .map(OrderItemResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore", "Order item restored successfully"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restore", e.getMessage()));
    }

    @Override
    public Future<Void> deletePermanent(Long orderItemId) {
        var ctx = tracingMetrics.startSpan("OrderItemCommandService.deletePermanent",
                Attributes.builder().put("order_item.id", orderItemId).build());

        return queryRepo.findByTrashedId(orderItemId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.<Void>failedFuture(
                                new BadRequestException(
                                        "Order item not found or must be trashed before permanent deletion"));
                    }
                    return repo.deleteOrderItemPermanently(orderItemId)
                            .compose(deleted -> {
                                if (!deleted) {
                                    return Future.<Void>failedFuture(
                                            new BadRequestException("Failed to delete order item permanently"));
                                }
                                return invalidateCache(orderItemId);
                            });
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deletePermanent",
                        "Order item permanently deleted"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "deletePermanent", err.getMessage()));
    }

    @Override
    public Future<Void> restoreAll() {
        var ctx = tracingMetrics.startSpan("OrderItemCommandService.restoreAll");

        return repo.restoreAllOrdersItem()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed order items found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all",
                        "All order items restored successfully"))
                .onFailure(err -> {
                    log.error("Failed to restore all order items", err);
                    tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
                });
    }

    @Override
    public Future<Void> deleteAllPermanent() {
        var ctx = tracingMetrics.startSpan("OrderItemCommandService.deleteAllPermanent");

        return repo.deleteAllPermanentOrdersItem()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed order items found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "delete_all",
                        "All trashed order items permanently deleted"))
                .onFailure(err -> {
                    log.error("Failed to permanently delete all order items", err);
                    tracingMetrics.completeSpanError(ctx, "delete_all", err.getMessage());
                });
    }

    private Future<Void> invalidateCache(Long orderItemId) {
        return redisService.delete(CACHE_PREFIX + orderItemId)
                .compose(v -> redisService.delete(CACHE_PREFIX + "order:" + orderItemId))
                .compose(v -> redisService.deleteByPattern(CACHE_LIST_PREFIX + "*"))
                .<Void>mapEmpty();
    }

    private Future<Void> invalidateListCache() {
        return redisService.deleteByPattern(CACHE_LIST_PREFIX + "*")
                .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "list:*"))
                .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "active:*"))
                .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "trashed:*"))
                .<Void>mapEmpty();
    }
}