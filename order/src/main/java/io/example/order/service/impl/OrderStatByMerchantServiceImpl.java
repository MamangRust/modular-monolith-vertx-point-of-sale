package io.example.order.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import io.example.order.domain.requests.MonthOrderMerchant;
import io.example.order.domain.requests.MonthTotalRevenueMerchant;
import io.example.order.domain.requests.YearOrderMerchant;
import io.example.order.domain.requests.YearTotalRevenueMerchant;
import io.example.order.domain.response.order.OrderMonthlyResponse;
import io.example.order.domain.response.order.OrderMonthlyTotalRevenueResponse;
import io.example.order.domain.response.order.OrderYearlyResponse;
import io.example.order.domain.response.order.OrderYearlyTotalRevenueResponse;
import io.example.order.model.OrderMonth;
import io.example.order.model.OrderMonthTotalRevenue;
import io.example.order.model.OrderYear;
import io.example.order.model.OrderYearTotalRevenue;
import io.example.order.repository.OrderStatByMerchantRepository;
import io.example.order.service.OrderStatByMerchantService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OrderStatByMerchantServiceImpl implements OrderStatByMerchantService {
    private final OrderStatByMerchantRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "order:stats:merchant:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public Future<List<OrderMonthlyTotalRevenueResponse>> findMonthlyTotalRevenueByMerchant(
            MonthTotalRevenueMerchant req) {
        var ctx = tracingMetrics.startSpan("OrderStatByMerchantService.findMonthlyTotalRevenueByMerchant",
                Attributes.builder().put("merchant.id", req.getMerchantId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_total:" + req.getMerchantId() + ":" + req.getYear() + ":"
                + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<OrderMonthTotalRevenue> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(OrderMonthTotalRevenue.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(OrderMonthlyTotalRevenueResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getMonthlyTotalRevenueByMerchant(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(OrderMonthlyTotalRevenueResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyTotalRevenueByMerchant", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findMonthlyTotalRevenueByMerchant",
                        e.getMessage()));
    }

    @Override
    public Future<List<OrderYearlyTotalRevenueResponse>> findYearlyTotalRevenueByMerchant(
            YearTotalRevenueMerchant req) {
        var ctx = tracingMetrics.startSpan("OrderStatByMerchantService.findYearlyTotalRevenueByMerchant",
                Attributes.builder().put("merchant.id", req.getMerchantId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_total:" + req.getMerchantId() + ":" + req.getYear();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<OrderYearTotalRevenue> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(OrderYearTotalRevenue.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(OrderYearlyTotalRevenueResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getYearlyTotalRevenueByMerchant(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(OrderYearlyTotalRevenueResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyTotalRevenueByMerchant", "Success"))
                .onFailure(
                        e -> tracingMetrics.completeSpanError(ctx, "findYearlyTotalRevenueByMerchant", e.getMessage()));
    }

    @Override
    public Future<List<OrderMonthlyResponse>> findMonthlyOrderByMerchant(MonthOrderMerchant req) {
        var ctx = tracingMetrics.startSpan("OrderStatByMerchantService.findMonthlyOrderByMerchant",
                Attributes.builder().put("merchant.id", req.getMerchantId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_order:" + req.getMerchantId() + ":" + req.getYear();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<OrderMonth> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(OrderMonth.class))
                                .toList();
                        return Future.succeededFuture(data.stream().map(OrderMonthlyResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getMonthlyOrderByMerchant(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(OrderMonthlyResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyOrderByMerchant", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findMonthlyOrderByMerchant", e.getMessage()));
    }

    @Override
    public Future<List<OrderYearlyResponse>> findYearlyOrderByMerchant(YearOrderMerchant req) {
        var ctx = tracingMetrics.startSpan("OrderStatByMerchantService.findYearlyOrderByMerchant",
                Attributes.builder().put("merchant.id", req.getMerchantId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_order:" + req.getMerchantId() + ":" + req.getYear();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<OrderYear> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(OrderYear.class))
                                .toList();
                        return Future.succeededFuture(data.stream().map(OrderYearlyResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getYearlyOrderByMerchant(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(OrderYearlyResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyOrderByMerchant", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findYearlyOrderByMerchant", e.getMessage()));
    }
}