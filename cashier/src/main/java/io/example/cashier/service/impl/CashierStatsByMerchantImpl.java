package io.example.cashier.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import io.example.cashier.domain.requests.cashier.MonthCashierMerchantRequest;
import io.example.cashier.domain.requests.cashier.MonthTotalSalesMerchant;
import io.example.cashier.domain.requests.cashier.YearCashierMerchantRequest;
import io.example.cashier.domain.requests.cashier.YearTotalSalesMerchant;
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.CashierStatByMerchantRepository;
import io.example.cashier.service.CashierStatsByMerchant;
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
public class CashierStatsByMerchantImpl implements CashierStatsByMerchant {
    private final CashierStatByMerchantRepository statByMerchantRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "cashier:stats:merchant:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public Future<List<CashierResponseMonthTotalSales>> getMonthlyTotalSalesByMerchant(MonthTotalSalesMerchant req) {
        var ctx = tracingMetrics.startSpan("CashierStatsByMerchant.getMonthlyTotalSalesByMerchant");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_total:" + req.getYear() + ":" + req.getMonth() + ":merchant:"
                + req.getMerchantId();

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
                    return statByMerchantRepository.getMonthlyTotalSalesByMerchant(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseMonthTotalSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyTotalSalesByMerchant", "Success"))
                .onFailure(
                        e -> tracingMetrics.completeSpanError(ctx, "getMonthlyTotalSalesByMerchant", e.getMessage()));
    }

    @Override
    public Future<List<CashierResponseYearTotalSales>> getYearlyTotalSalesByMerchant(YearTotalSalesMerchant req) {
        var ctx = tracingMetrics.startSpan("CashierStatsByMerchant.getYearlyTotalSalesByMerchant");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_total:" + req.getYear() + ":merchant:" + req.getMerchantId();

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
                    return statByMerchantRepository.getYearlyTotalSalesByMerchant(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseYearTotalSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyTotalSalesByMerchant", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyTotalSalesByMerchant", e.getMessage()));
    }

    @Override
    public Future<List<CashierResponseMonthSales>> getMonthlyCashierByMerchant(MonthCashierMerchantRequest req) {
        var ctx = tracingMetrics.startSpan("CashierStatsByMerchant.getMonthlyCashierByMerchant");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_cashier:" + req.getYear() + ":merchant:"
                + req.getMerchantId();

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
                    return statByMerchantRepository.getMonthlyCashierByMerchant(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseMonthSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyCashierByMerchant", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getMonthlyCashierByMerchant", e.getMessage()));
    }

    @Override
    public Future<List<CashierResponseYearSales>> getYearlyCashierByMerchant(YearCashierMerchantRequest req) {
        var ctx = tracingMetrics.startSpan("CashierStatsByMerchant.getYearlyCashierByMerchant");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_cashier:" + req.getYear() + ":merchant:" + req.getMerchantId();

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
                    return statByMerchantRepository.getYearlyCashierByMerchant(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseYearSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyCashierByMerchant", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyCashierByMerchant", e.getMessage()));
    }
}