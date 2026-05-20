package io.example.order.service.impl;

import java.time.Duration;

import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.order.domain.requests.FindAllOrderMerchant;
import io.example.order.domain.requests.FindAllOrders;
import io.example.order.model.Order;
import io.example.order.repository.OrderQueryRepository;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderQueryServiceImpl implements io.example.order.service.OrderQueryService {
    private final OrderQueryRepository repository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    public OrderQueryServiceImpl(OrderQueryRepository repository, RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.repository = repository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<PagedResult<Order>> findAll(FindAllOrders req) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderQueryService.findAll");
        return repository.findAllOrders(req)
                .onSuccess(res -> tracingMetrics.completeSpanSuccess(ctx, "find_all", "Success"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "find_all", err.getMessage()));
    }

    @Override
    public Future<Order> findById(int orderId) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderQueryService.findById");
        String key = String.format("order:detail:%d", orderId);

        return redisService.get(key)
                .compose(cached -> {
                    if (cached != null) {
                        try {
                            Order order = Order.fromJson(new JsonObject(cached));
                            tracingMetrics.completeSpanSuccess(ctx, "find_by_id", "Cache Hit");
                            return Future.succeededFuture(order);
                        } catch (Exception e) {
                            log.warn("Cache parse error", e);
                        }
                    }
                    return repository.findById((long) orderId)
                            .compose(order -> {
                                if (order != null) {
                                    redisService.set(key, order.toJson().encode(), Duration.ofMinutes(30));
                                }
                                tracingMetrics.completeSpanSuccess(ctx, "find_by_id", "DB Fetch Success");
                                return Future.succeededFuture(order);
                            });
                })
                .recover(err -> {
                    tracingMetrics.completeSpanError(ctx, "find_by_id", err.getMessage());
                    return Future.failedFuture(err);
                });
    }

    @Override
    public Future<PagedResult<Order>> findByActive(FindAllOrders req) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderQueryService.findByActive");
        return repository.findByActive(req)
                .onSuccess(res -> tracingMetrics.completeSpanSuccess(ctx, "find_by_active", "Success"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "find_by_active", err.getMessage()));
    }

    @Override
    public Future<PagedResult<Order>> findByTrashed(FindAllOrders req) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderQueryService.findByTrashed");
        return repository.findByTrashed(req)
                .onSuccess(res -> tracingMetrics.completeSpanSuccess(ctx, "find_by_trashed", "Success"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "find_by_trashed", err.getMessage()));
    }

    @Override
    public Future<PagedResult<Order>> findByMerchant(FindAllOrderMerchant req) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderQueryService.findByMerchant");
        return repository.findByMerchant(req)
                .onSuccess(res -> tracingMetrics.completeSpanSuccess(ctx, "find_by_merchant", "Success"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "find_by_merchant", err.getMessage()));
    }
}
