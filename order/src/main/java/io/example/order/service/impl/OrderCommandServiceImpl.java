package io.example.order.service.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
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
import io.example.order.domain.response.order.OrderResponse;
import io.example.order.domain.response.order.OrderResponseDeleteAt;
import io.example.order.model.Order;
import io.example.order.model.OrderItem;
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
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
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

    private static final String CACHE_PREFIX = "order:";
    private static final String CACHE_LIST_PREFIX = "order:list:";

    @Override
    public Future<OrderResponse> createOrder(CreateOrderRequest req) {
        var ctx = tracingMetrics.startSpan("OrderCommandService.createOrder",
                Attributes.builder()
                        .put("merchant.id", req.getMerchantId())
                        .put("cashier.id", req.getCashierId())
                        .build());

        return validateMerchantAndCashier(req.getMerchantId().longValue(), req.getCashierId().longValue())
                .compose(v -> orderCommandRepository.createOrder(
                        new CreateOrderRecordRequest(req.getMerchantId().longValue(), req.getCashierId().longValue(),
                                0)))
                // Stock compensation is scoped to the item-processing phase:
                // once all items are created and stock is decremented, later
                // failures (total recompute, cache evict) must NOT restore stock
                // because the order + items are already committed in the DB.
                .compose(order -> processOrderItemsWithCompensation(order.getOrderId(), req.getItems())
                        .map(v -> order))
                .compose(order -> orderItemQueryRepository.calculateTotalPrice(order.getOrderId())
                        .compose(totalPrice -> orderCommandRepository.updateOrder(
                                new UpdateOrderRecordRequest(order.getOrderId(), totalPrice))
                                .map(v -> order)))
                .compose(order -> invalidateCache(order.getOrderId()).<Order>map(v -> order))
                .map(OrderResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "create", "Order created successfully"))
                .onFailure(err -> {
                    log.error("Failed to create order", err);
                    tracingMetrics.completeSpanError(ctx, "create", err.getMessage());
                });
    }

    @Override
    public Future<OrderResponse> updateOrder(UpdateOrderRequest req) {
        var ctx = tracingMetrics.startSpan("OrderCommandService.updateOrder",
                Attributes.builder().put("order.id", req.getOrderId()).build());

        return orderQueryRepository.findById(req.getOrderId().longValue())
                .compose(order -> {
                    if (order == null) {
                        return Future.failedFuture(new NotFoundException("Order not found"));
                    }
                    return validateCashier(req.getCashierId().longValue())
                            // Stock compensation is scoped to the item-update
                            // phase, mirroring createOrder: once the items are
                            // updated/created and stock is decremented, later
                            // failures (total recompute, cache evict) must NOT
                            // restore stock.
                            .compose(v -> processUpdateOrderItemsWithCompensation(req.getOrderId().longValue(),
                                    req.getItems()))
                            .map(v -> order);
                })
                .compose(order -> orderItemQueryRepository.calculateTotalPrice(req.getOrderId().longValue())
                        .compose(totalPrice -> orderCommandRepository.updateOrder(
                                new UpdateOrderRecordRequest(req.getOrderId().longValue(), totalPrice))))
                .compose(order -> invalidateCache(order.getOrderId()).<Order>map(v -> order))
                .map(OrderResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update", "Order updated successfully"))
                .onFailure(err -> {
                    log.error("Failed to update order: {}", req.getOrderId(), err);
                    tracingMetrics.completeSpanError(ctx, "update", err.getMessage());
                });
    }

    @Override
    public Future<OrderResponseDeleteAt> trashedOrder(Long orderId) {
        var ctx = tracingMetrics.startSpan("OrderCommandService.trashedOrder",
                Attributes.builder().put("order.id", orderId).build());
        // Tracks every stock restore applied to the order's active items so a
        // failed trash can roll them back (decrement again).
        List<StockDecrement> appliedRestores = new ArrayList<>();

        return orderQueryRepository.findById(orderId)
                .compose(order -> {
                    if (order == null) {
                        return Future.failedFuture(new NotFoundException("Order not found or already trashed"));
                    }
                    return trashOrderWithStockRestore(orderId, appliedRestores);
                })
                .compose(trashed -> invalidateCache(trashed.getOrderId()).<Order>map(v -> trashed))
                .map(OrderResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "trash", "Order trashed successfully"))
                .onFailure(err -> {
                    log.error("Failed to trash order: {}", orderId, err);
                    tracingMetrics.completeSpanError(ctx, "trash", err.getMessage());
                });
    }

    @Override
    public Future<OrderResponseDeleteAt> restoreOrder(Long orderId) {
        var ctx = tracingMetrics.startSpan("OrderCommandService.restoreOrder",
                Attributes.builder().put("order.id", orderId).build());
        // Tracks every stock decrement applied to the order's active items so a
        // failed restore can roll them back (increment again).
        List<StockDecrement> appliedDecrements = new ArrayList<>();

        return orderQueryRepository.findByTrashedId(orderId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.failedFuture(new BadRequestException("Order not found or must be trashed first"));
                    }
                    return restoreOrderWithStockDecrement(orderId, appliedDecrements);
                })
                .compose(r -> invalidateCache(orderId).<Order>map(v -> r))
                .map(OrderResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restoreOrder", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreOrder", e.getMessage()));
    }

    @Override
    public Future<Boolean> deleteOrderPermanent(Long orderId) {
        var ctx = tracingMetrics.startSpan("OrderCommandService.deleteOrderPermanent",
                Attributes.builder().put("order.id", orderId).build());

        return orderQueryRepository.findByTrashedId(orderId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.<Boolean>failedFuture(
                                new BadRequestException(
                                        "Order not found or must be trashed before permanent deletion"));
                    }
                    return orderCommandRepository.deleteOrderPermanent(orderId)
                            .compose(deleted -> {
                                if (!deleted) {
                                    return Future.<Boolean>failedFuture(
                                            new BadRequestException("Failed to delete order permanently"));
                                }
                                return invalidateCache(orderId).map(v -> true);
                            });
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deleteOrderPermanent",
                        "Order deleted permanently"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "deleteOrderPermanent", err.getMessage()));
    }

    @Override
    public Future<Boolean> restoreAllOrder() {
        var ctx = tracingMetrics.startSpan("OrderCommandService.restoreAllOrder");

        return orderQueryRepository.findAllTrashed()
                .compose(trashedOrders -> {
                    if (trashedOrders == null || trashedOrders.isEmpty()) {
                        return Future.failedFuture(new NotFoundException("No trashed orders found"));
                    }
                    return restoreAllOrdersOneByOne(trashedOrders);
                })
                .compose(restoredCount -> {
                    if (restoredCount == 0) {
                        // Race: every order was concurrently restored by other
                        // requests before this one could claim them.
                        return Future.failedFuture(new NotFoundException("No trashed orders found"));
                    }
                    return invalidateListCache().map(v -> true);
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all",
                        "All orders restored successfully"))
                .onFailure(err -> {
                    log.error("Failed to restore all orders", err);
                    tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
                });
    }

    @Override
    public Future<Boolean> deleteAllOrderPermanent() {
        var ctx = tracingMetrics.startSpan("OrderCommandService.deleteAllOrderPermanent");

        return orderCommandRepository.deleteAllOrderPermanent()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed orders found"));
                    }
                    return invalidateListCache().map(v -> true);
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "delete_all",
                        "All orders permanently deleted"))
                .onFailure(err -> {
                    log.error("Failed to permanently delete all orders", err);
                    tracingMetrics.completeSpanError(ctx, "delete_all", err.getMessage());
                });
    }

    private Future<Void> validateMerchantAndCashier(Long merchantId, Long cashierId) {
        return merchantQueryRepository.existsById(merchantId)
                .compose(merchantExists -> {
                    if (!merchantExists) {
                        return Future.failedFuture(new NotFoundException("Merchant not found"));
                    }
                    return validateCashier(cashierId);
                });
    }

    private Future<Void> validateCashier(Long cashierId) {
        return cashierQueryRepository.existsById(cashierId)
                .compose(exists -> {
                    if (!exists) {
                        return Future.failedFuture(new NotFoundException("Cashier not found"));
                    }
                    return Future.succeededFuture();
                });
    }

    /**
     * Processes every order item while tracking each successful stock
     * decrement. If any item fails, all already-decremented stock is rolled
     * back via {@code restoreStockDecrements(..., true)}, then the ORIGINAL
     * failure is re-surfaced — a compensation error never masks the real one.
     */
    private Future<Void> processOrderItemsWithCompensation(Long orderId, List<CreateOrderItemRequest> items) {
        List<StockDecrement> appliedDecrements = new ArrayList<>();
        Future<Void> future = Future.succeededFuture();
        for (CreateOrderItemRequest item : items) {
            final CreateOrderItemRequest currentItem = item;
            future = future.compose(v -> createAndProcessItem(orderId, currentItem, appliedDecrements));
        }
        // restoreStockDecrements(..., true) always succeeds (errors swallowed),
        // so re-fail with the original order error.
        return future.recover(err -> restoreStockDecrements(appliedDecrements, true)
                .compose(v -> Future.failedFuture(err)));
    }

    private Future<Void> createAndProcessItem(Long orderId, CreateOrderItemRequest item,
            List<StockDecrement> appliedDecrements) {
        return productQueryRepository.getProductById(item.getProductId())
                .compose(product -> {
                    if (product == null) {
                        return Future.failedFuture(new NotFoundException("Product not found"));
                    }
                    if (product.getCountInStock() < item.getQuantity()) {
                        return Future.failedFuture(new BadRequestException("Insufficient product stock"));
                    }

                    return orderItemCommandRepository.createOrderItem(
                            new CreateOrderItemRecordRequest(orderId, item.getProductId(),
                                    item.getQuantity(), product.getPrice()))
                            .compose(createdItem -> decrementStockGuarded(product.getProductId(), item.getQuantity())
                                    // Record the successful decrement so it can
                                    // be rolled back if a later item fails.
                                    .map(v -> {
                                        appliedDecrements
                                                .add(new StockDecrement(product.getProductId(), item.getQuantity()));
                                        return v;
                                    }));
                });
    }

    /**
     * Restores (increments back) every successfully-decremented stock, one
     * product at a time.
     *
     * @param swallowErrors {@code true} for regular compensation, where each
     *                      failure is logged and swallowed so compensation
     *                      never masks the original order failure; {@code false}
     *                      to undo our own decrement after losing a restore
     *                      race, where no original error exists to preserve — a
     *                      failed undo leaves stock inconsistent, so the
     *                      failure must propagate (best-effort per product,
     *                      then abort).
     */
    private Future<Void> restoreStockDecrements(List<StockDecrement> appliedDecrements, boolean swallowErrors) {
        Future<Void> restore = Future.succeededFuture();
        for (StockDecrement decrement : appliedDecrements) {
            restore = restore.compose(v -> productCommandRepository
                    .incrementStock(decrement.productId(), decrement.quantity())
                    .recover(err -> {
                        if (swallowErrors) {
                            log.error(
                                    "Failed to compensate stock for product {} (qty {}): {}",
                                    decrement.productId(), decrement.quantity(), err.getMessage());
                            return Future.succeededFuture(null);
                        }
                        return Future.failedFuture(err);
                    })
                    .mapEmpty());
        }
        return restore;
    }

    private record StockDecrement(Long productId, int quantity) {
    }

    private Future<Void> decrementStockGuarded(Long productId, int quantity) {
        return productCommandRepository.decrementStock(productId, quantity)
                .compose(updated -> {
                    if (updated == null) {
                        // Defensive: an empty gRPC response (no data) must never be
                        // treated as success — the order item was already created.
                        return Future.failedFuture(new BadRequestException("Insufficient product stock"));
                    }
                    return Future.succeededFuture();
                });
    }

    private Future<Void> incrementStockGuarded(Long productId, int quantity) {
        return productCommandRepository.incrementStock(productId, quantity)
                .compose(updated -> {
                    if (updated == null) {
                        // Defensive: an empty gRPC response must never be treated
                        // as success — otherwise the stock would silently stay low.
                        return Future.failedFuture(new BadRequestException("Failed to restore product stock"));
                    }
                    return Future.succeededFuture();
                });
    }

    /**
     * Restores (increments) the stock of every ACTIVE order item BEFORE the
     * order is trashed, tracking each successful restore. If the trash fails,
     * every restore is reverted and the ORIGINAL failure is re-surfaced — a
     * revert error never masks the real one. Reverts are scoped to this phase:
     * once the order is trashed, later failures (cache evict) must NOT touch
     * stock again.
     */
    private Future<Order> trashOrderWithStockRestore(Long orderId, List<StockDecrement> appliedRestores) {
        return restoreStockForOrderItems(orderId, appliedRestores)
                .compose(v -> orderCommandRepository.trashedOrder(orderId))
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.<Order>failedFuture(new NotFoundException("Order not found or already trashed"));
                    }
                    return Future.succeededFuture(trashed);
                })
                .recover(err -> revertStockRestores(appliedRestores)
                        .recover(compErr -> {
                            log.error("Failed to revert stock restore after trash failure", compErr);
                            return Future.succeededFuture();
                        })
                        .compose(v -> Future.failedFuture(err)));
    }

    private Future<Void> restoreStockForOrderItems(Long orderId, List<StockDecrement> appliedRestores) {
        return orderItemQueryRepository.findOrderItemByOrder(orderId)
                .compose(items -> {
                    if (items == null || items.isEmpty()) {
                        return Future.succeededFuture();
                    }
                    Future<Void> future = Future.succeededFuture();
                    for (OrderItem item : items) {
                        final OrderItem currentItem = item;
                        future = future.compose(v -> incrementStockGuarded(currentItem.getProductId(),
                                currentItem.getQuantity())
                                // Record the successful restore so it can be
                                // reverted if the trash itself fails.
                                .map(vv -> {
                                    appliedRestores.add(new StockDecrement(currentItem.getProductId(),
                                            currentItem.getQuantity()));
                                    return vv;
                                }));
                    }
                    return future;
                });
    }

    /**
     * Reverts every successfully-restored stock (decrement back). Failures are
     * logged and swallowed so a revert never masks the original trash failure.
     */
    private Future<Void> revertStockRestores(List<StockDecrement> appliedRestores) {
        Future<Void> revert = Future.succeededFuture();
        for (StockDecrement restore : appliedRestores) {
            revert = revert.compose(v -> productCommandRepository
                    .decrementStock(restore.productId(), restore.quantity())
                    .recover(err -> {
                        log.error(
                                "Failed to revert restored stock for product {} (qty {}): {}",
                                restore.productId(), restore.quantity(), err.getMessage());
                        return Future.succeededFuture(null);
                    })
                    .mapEmpty());
        }
        return revert;
    }

    /**
     * Decrements the stock of every ACTIVE order item BEFORE the order is
     * restored, tracking each successful decrement (mirror image of the trash
     * restore). If the restore fails, every decrement is rolled back and the
     * ORIGINAL failure is re-surfaced.
     */
    private Future<Order> restoreOrderWithStockDecrement(Long orderId, List<StockDecrement> appliedDecrements) {
        return decrementStockForOrderItems(orderId, appliedDecrements)
                .compose(v -> orderCommandRepository.restoreOrder(orderId))
                .compose(restored -> {
                    if (restored == null) {
                        return Future.<Order>failedFuture(new NotFoundException("Order not found"));
                    }
                    return Future.succeededFuture(restored);
                })
                .recover(err -> restoreStockDecrements(appliedDecrements, true)
                        .compose(v -> Future.failedFuture(err)));
    }

    private Future<Void> decrementStockForOrderItems(Long orderId, List<StockDecrement> appliedDecrements) {
        return orderItemQueryRepository.findOrderItemByOrder(orderId)
                .compose(items -> {
                    if (items == null || items.isEmpty()) {
                        return Future.succeededFuture();
                    }
                    Future<Void> future = Future.succeededFuture();
                    for (OrderItem item : items) {
                        final OrderItem currentItem = item;
                        future = future.compose(v -> decrementStockGuarded(currentItem.getProductId(),
                                currentItem.getQuantity())
                                // Record the successful decrement so it can be
                                // rolled back if the restore itself fails.
                                .map(vv -> {
                                    appliedDecrements.add(new StockDecrement(currentItem.getProductId(),
                                            currentItem.getQuantity()));
                                    return vv;
                                }));
                    }
                    return future;
                });
    }

    /**
     * Restores every trashed order ONE BY ONE (no bulk UPDATE), which closes
     * the double-decrement race of the old bulk flow. For each order:
     *   1. decrement the active items' stock (tracked per order);
     *   2. restoreOrder(orderId) — the repo's UPDATE ... WHERE deleted_at IS
     *      NOT NULL is atomic: if it returns null, another request already
     *      restored (and already decremented) this order, so OUR decrement is
     *      compensated back and the order is skipped — not an error, the goal
     *      is already met (unless the undo itself fails: stock would then be
     *      inconsistent, so the whole call FAILS instead of masking it).
     * If a decrement fails, that order's decrements are compensated and the
     * ORIGINAL error is re-surfaced. Partial completion: orders restored
     * before a later failure stay restored (their stock decrement is already
     * committed and consistent). Returns how many orders this call actually
     * restored.
     */
    private Future<Integer> restoreAllOrdersOneByOne(List<Order> trashedOrders) {
        Future<Integer> restoredCount = Future.succeededFuture(0);
        for (Order order : trashedOrders) {
            final Long orderId = order.getOrderId();
            restoredCount = restoredCount.compose(count -> restoreOneTrashedOrder(orderId)
                    .map(restored -> count + (restored ? 1 : 0)));
        }
        return restoredCount;
    }

    private Future<Boolean> restoreOneTrashedOrder(Long orderId) {
        List<StockDecrement> appliedDecrements = new ArrayList<>();
        return decrementStockForOrderItems(orderId, appliedDecrements)
                .compose(v -> orderCommandRepository.restoreOrder(orderId))
                // Compensate only failures of the decrement/restore phase. An
                // undo failure in the null-branch below must NOT re-enter here
                // (it would compensate twice → over-increment).
                .recover(err -> restoreStockDecrements(appliedDecrements, true)
                        .compose(v -> Future.failedFuture(err)))
                .compose(restored -> {
                    if (restored == null) {
                        // Concurrent restoreOrder already restored this order
                        // (and already decremented its stock) — undo OUR
                        // decrement so we never double-decrement, then skip.
                        // swallowErrors=false: a failed undo leaves stock
                        // inconsistent, which must fail loudly rather than
                        // report success.
                        return restoreStockDecrements(appliedDecrements, false)
                                .recover(compErr -> {
                                    log.error(
                                            "Failed to undo stock after concurrent restore of order {}: {}",
                                            orderId, compErr.getMessage());
                                    return Future.<Void>failedFuture(
                                            new IllegalStateException(
                                                    "Failed to undo stock for concurrently restored order "
                                                            + orderId,
                                                    compErr));
                                })
                                .map(v -> false);
                    }
                    return Future.succeededFuture(true);
                });
    }

    /**
     * Updates/creates every order item while tracking each successful stock
     * decrement (new items only). If any item fails, all already-decremented
     * stock is rolled back, then the ORIGINAL failure is re-surfaced.
     */
    private Future<Void> processUpdateOrderItemsWithCompensation(Long orderId, List<UpdateOrderItemRequest> items) {
        List<StockDecrement> appliedDecrements = new ArrayList<>();
        Future<Void> future = Future.succeededFuture();
        for (UpdateOrderItemRequest item : items) {
            final UpdateOrderItemRequest currentItem = item;
            future = future.compose(v -> updateOrCreateItem(orderId, currentItem, appliedDecrements));
        }
        return future.recover(err -> restoreStockDecrements(appliedDecrements, true)
                .compose(v -> Future.failedFuture(err)));
    }

    private Future<Void> updateOrCreateItem(Long orderId, UpdateOrderItemRequest item,
            List<StockDecrement> appliedDecrements) {
        return productQueryRepository.getProductById(item.getProductId())
                .compose(product -> {
                    if (product == null) {
                        return Future.failedFuture(new NotFoundException("Product not found"));
                    }

                    if (item.getOrderItemId() > 0) {
                        // Existing item: only the quantity/price snapshot is
                        // updated — no stock adjustment, so nothing to track.
                        return orderItemCommandRepository.updateOrderItem(
                                new UpdateOrderItemRecordRequest(
                                        item.getOrderItemId(), orderId, item.getProductId(),
                                        item.getQuantity(), product.getPrice()))
                                .<Void>mapEmpty();
                    } else {
                        if (product.getCountInStock() < item.getQuantity()) {
                            return Future.failedFuture(new BadRequestException("Insufficient product stock"));
                        }

                        return orderItemCommandRepository.createOrderItem(
                                new CreateOrderItemRecordRequest(orderId, item.getProductId(),
                                        item.getQuantity(), product.getPrice()))
                                .compose(createdItem -> decrementStockGuarded(product.getProductId(), item.getQuantity())
                                        // Record the successful decrement so it
                                        // can be rolled back if a later item fails.
                                        .map(v -> {
                                            appliedDecrements.add(
                                                    new StockDecrement(product.getProductId(), item.getQuantity()));
                                            return v;
                                        }));
                    }
                });
    }

    private Future<Void> invalidateCache(Long orderId) {
        return redisService.delete(CACHE_PREFIX + orderId)
                .compose(v -> redisService.deleteByPattern(CACHE_LIST_PREFIX + "*"))
                .<Void>mapEmpty();
    }

    private Future<Void> invalidateListCache() {
        return redisService.deleteByPattern(CACHE_LIST_PREFIX + "*").<Void>mapEmpty();
    }
}