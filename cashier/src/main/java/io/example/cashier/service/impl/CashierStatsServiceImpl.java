package io.example.cashier.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import io.example.cashier.domain.requests.cashier.MonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.CashierStatsRepository;
import io.example.cashier.service.CashierStatsService;
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
public class CashierStatsServiceImpl implements CashierStatsService {
    private final CashierStatsRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "cashier:stats:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public Future<List<CashierResponseMonthTotalSales>> getMonthlyTotalSales(MonthTotalSales req) {
        var ctx = tracingMetrics.startSpan("CashierStatsService.getMonthlyTotalSales");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_total:" + req.getYear() + ":" + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<CashierMonthTotalSales> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CashierMonthTotalSales.class))
                                .toList();
                        return Future.succeededFuture(data.stream().map(CashierResponseMonthTotalSales::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getMonthlyTotalSales(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseMonthTotalSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyTotalSales", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getMonthlyTotalSales", e.getMessage()));
    }

    @Override
    public Future<List<CashierResponseYearTotalSales>> getYearlyTotalSales(int year) {
        var ctx = tracingMetrics.startSpan("CashierStatsService.getYearlyTotalSales");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_total:" + year;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<CashierYearTotalSales> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CashierYearTotalSales.class))
                                .toList();
                        return Future.succeededFuture(data.stream().map(CashierResponseYearTotalSales::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getYearlyTotalSales(year)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseYearTotalSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyTotalSales", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyTotalSales", e.getMessage()));
    }

    @Override
    public Future<List<CashierResponseMonthSales>> getMonthlyCashier(int year) {
        var ctx = tracingMetrics.startSpan("CashierStatsService.getMonthlyCashier");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_cashier:" + year;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<CashierMonthSales> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CashierMonthSales.class))
                                .toList();
                        return Future.succeededFuture(data.stream().map(CashierResponseMonthSales::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getMonthlyCashier(year)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseMonthSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyCashier", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getMonthlyCashier", e.getMessage()));
    }

    @Override
    public Future<List<CashierResponseYearSales>> getYearlyCashier(int year) {
        var ctx = tracingMetrics.startSpan("CashierStatsService.getYearlyCashier");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_cashier:" + year;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<CashierYearSales> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CashierYearSales.class))
                                .toList();
                        return Future.succeededFuture(data.stream().map(CashierResponseYearSales::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getYearlyCashier(year)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseYearSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyCashier", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyCashier", e.getMessage()));
    }
}