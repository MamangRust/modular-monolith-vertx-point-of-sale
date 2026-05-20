package io.example.order.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.order.domain.requests.MonthOrderMerchant;
import io.example.order.domain.requests.MonthTotalRevenueMerchant;
import io.example.order.domain.requests.YearOrderMerchant;
import io.example.order.domain.requests.YearTotalRevenueMerchant;
import io.example.order.model.OrderMonth;
import io.example.order.model.OrderMonthTotalRevenue;
import io.example.order.model.OrderYear;
import io.example.order.model.OrderYearTotalRevenue;
import io.example.order.repository.OrderStatByMerchantRepository;
import io.example.order.service.OrderStatByMerchantService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
public class OrderStatByMerchantServiceImpl implements OrderStatByMerchantService {
    private final OrderStatByMerchantRepository repository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderStatByMerchantServiceImpl(OrderStatByMerchantRepository repository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.repository = repository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<List<OrderMonthTotalRevenue>> findMonthlyTotalRevenueByMerchant(MonthTotalRevenueMerchant req) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderService.findMonthlyTotalRevenueByMerchant");
        String key = String.format("order:report:monthly_revenue_merchant:%d:%d:%d", req.getMerchantId(), req.getYear(), req.getMonth());

        return redisService.get(key)
                .compose(cached -> handleCacheOrRepo(cached, key,
                        () -> repository.getMonthlyTotalRevenueByMerchant(req),
                        new TypeReference<List<OrderMonthTotalRevenue>>() {}, ctx, "report_monthly_revenue_merchant"))
                .recover(err -> handleError(ctx, "report_monthly_revenue_merchant", err));
    }

    @Override
    public Future<List<OrderYearTotalRevenue>> findYearlyTotalRevenueByMerchant(YearTotalRevenueMerchant req) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderService.findYearlyTotalRevenueByMerchant");
        String key = String.format("order:report:yearly_revenue_merchant:%d:%d", req.getMerchantId(), req.getYear());

        return redisService.get(key)
                .compose(cached -> handleCacheOrRepo(cached, key,
                        () -> repository.getYearlyTotalRevenueByMerchant(req),
                        new TypeReference<List<OrderYearTotalRevenue>>() {}, ctx, "report_yearly_revenue_merchant"))
                .recover(err -> handleError(ctx, "report_yearly_revenue_merchant", err));
    }

    @Override
    public Future<List<OrderMonth>> findMonthlyOrderByMerchant(MonthOrderMerchant req) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderService.findMonthlyOrderByMerchant");
        String key = String.format("order:report:monthly_order_merchant:%d:%d", req.getMerchantId(), req.getYear());

        return redisService.get(key)
                .compose(cached -> handleCacheOrRepo(cached, key,
                        () -> repository.getMonthlyOrderByMerchant(req),
                        new TypeReference<List<OrderMonth>>() {}, ctx, "report_monthly_order_merchant"))
                .recover(err -> handleError(ctx, "report_monthly_order_merchant", err));
    }

    @Override
    public Future<List<OrderYear>> findYearlyOrderByMerchant(YearOrderMerchant req) {
        TracingMetrics.TracingContext ctx = tracingMetrics.startSpan("OrderService.findYearlyOrderByMerchant");
        String key = String.format("order:report:yearly_order_merchant:%d:%d", req.getMerchantId(), req.getYear());

        return redisService.get(key)
                .compose(cached -> handleCacheOrRepo(cached, key,
                        () -> repository.getYearlyOrderByMerchant(req),
                        new TypeReference<List<OrderYear>>() {}, ctx, "report_yearly_order_merchant"))
                .recover(err -> handleError(ctx, "report_yearly_order_merchant", err));
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
        log.error("Order stats by merchant service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
