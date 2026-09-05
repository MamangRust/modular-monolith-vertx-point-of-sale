package io.example.order.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import io.example.order.domain.requests.MonthTotalRevenue;
import io.example.order.domain.response.order.OrderMonthlyResponse;
import io.example.order.domain.response.order.OrderMonthlyTotalRevenueResponse;
import io.example.order.domain.response.order.OrderYearlyResponse;
import io.example.order.domain.response.order.OrderYearlyTotalRevenueResponse;
import io.example.order.model.OrderMonth;
import io.example.order.model.OrderMonthTotalRevenue;
import io.example.order.model.OrderYear;
import io.example.order.model.OrderYearTotalRevenue;
import io.example.order.repository.OrderStatsRepository;
import io.example.order.service.OrderStatsService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OrderStatsServiceImpl implements OrderStatsService {
    private final OrderStatsRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "order:stats:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public Future<List<OrderMonthlyTotalRevenueResponse>> findMonthlyTotalRevenue(MonthTotalRevenue req) {
        var ctx = tracingMetrics.startSpan("OrderStatsService.findMonthlyTotalRevenue");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_total:" + req.getYear() + ":" + req.getMonth();

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
                    return statsRepository.getMonthlyTotalRevenue(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(OrderMonthlyTotalRevenueResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyTotalRevenue", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findMonthlyTotalRevenue", e.getMessage()));
    }

    @Override
    public Future<List<OrderYearlyTotalRevenueResponse>> findYearlyTotalRevenue(int year) {
        var ctx = tracingMetrics.startSpan("OrderStatsService.findYearlyTotalRevenue");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_total:" + year;

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
                    return statsRepository.getYearlyTotalRevenue(year)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(OrderYearlyTotalRevenueResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyTotalRevenue", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findYearlyTotalRevenue", e.getMessage()));
    }

    @Override
    public Future<List<OrderMonthlyResponse>> findMonthlyOrder(int year) {
        var ctx = tracingMetrics.startSpan("OrderStatsService.findMonthlyOrder");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_order:" + year;

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
                    return statsRepository.getMonthlyOrder(year)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(OrderMonthlyResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyOrder", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findMonthlyOrder", e.getMessage()));
    }

    @Override
    public Future<List<OrderYearlyResponse>> findYearlyOrder(int year) {
        var ctx = tracingMetrics.startSpan("OrderStatsService.findYearlyOrder");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_order:" + year;

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
                    return statsRepository.getYearlyOrder(year)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(OrderYearlyResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyOrder", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findYearlyOrder", e.getMessage()));
    }
}