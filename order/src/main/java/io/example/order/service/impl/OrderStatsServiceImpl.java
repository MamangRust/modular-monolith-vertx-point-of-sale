package io.example.order.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.order.domain.requests.MonthTotalRevenue;
import io.example.order.model.OrderMonth;
import io.example.order.model.OrderMonthTotalRevenue;
import io.example.order.model.OrderYear;
import io.example.order.model.OrderYearTotalRevenue;
import io.example.order.repository.OrderStatsRepository;
import io.example.order.service.OrderStatsService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
public class OrderStatsServiceImpl implements OrderStatsService {
    private final OrderStatsRepository repository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderStatsServiceImpl(OrderStatsRepository repository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.repository = repository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<List<OrderMonthTotalRevenue>> findMonthlyTotalRevenue(MonthTotalRevenue req) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderService.findMonthlyTotalRevenue");
        String key = String.format("order:report:monthly_revenue:%d:%d", req.getYear(), req.getMonth());

        return redisService.get(key)
                .compose(cached -> handleCacheOrRepo(cached, key,
                        () -> repository.getMonthlyTotalRevenue(req),
                        new TypeReference<List<OrderMonthTotalRevenue>>() {}, ctx, "report_monthly_revenue"))
                .recover(err -> handleError(ctx, "report_monthly_revenue", err));
    }

    @Override
    public Future<List<OrderYearTotalRevenue>> findYearlyTotalRevenue(int year) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderService.findYearlyTotalRevenue");
        String key = String.format("order:report:yearly_revenue:%d", year);

        return redisService.get(key)
                .compose(cached -> handleCacheOrRepo(cached, key,
                        () -> repository.getYearlyTotalRevenue(year),
                        new TypeReference<List<OrderYearTotalRevenue>>() {}, ctx, "report_yearly_revenue"))
                .recover(err -> handleError(ctx, "report_yearly_revenue", err));
    }

    @Override
    public Future<List<OrderMonth>> findMonthlyOrder(int year) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderService.findMonthlyOrder");
        String key = String.format("order:report:monthly_order:%d", year);

        return redisService.get(key)
                .compose(cached -> handleCacheOrRepo(cached, key,
                        () -> repository.getMonthlyOrder(year),
                        new TypeReference<List<OrderMonth>>() {}, ctx, "report_monthly_order"))
                .recover(err -> handleError(ctx, "report_monthly_order", err));
    }

    @Override
    public Future<List<OrderYear>> findYearlyOrder(int year) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderService.findYearlyOrder");
        String key = String.format("order:report:yearly_order:%d", year);

        return redisService.get(key)
                .compose(cached -> handleCacheOrRepo(cached, key,
                        () -> repository.getYearlyOrder(year),
                        new TypeReference<List<OrderYear>>() {}, ctx, "report_yearly_order"))
                .recover(err -> handleError(ctx, "report_yearly_order", err));
    }

    private <T> Future<T> handleCacheOrRepo(String cached, String cacheKey,
                                            java.util.concurrent.Callable<Future<T>> repoCall, TypeReference<T> typeRef,
                                            TracingMetrics.TracingContext tracingCtx, String operation) {
        if (cached != null) {
            try {
                T data = objectMapper.readValue(cached, typeRef);
                return Future.succeededFuture(data);
            } catch (Exception e) {
                log.warn("Cache parse error", e);
            }
        }
        try {
            return repoCall.call().compose(res -> {
                if (res != null) {
                    redisService.set(cacheKey, Json.encode(res), Duration.ofMinutes(30));
                }
                tracingMetrics.completeSpanSuccess(tracingCtx, operation, "Success");
                return Future.succeededFuture(res);
            });
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    private <T> Future<T> handleError(TracingMetrics.TracingContext ctx, String methodName, Throwable err) {
        log.error("Order stats service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
