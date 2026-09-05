package io.example.cashier.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import io.example.cashier.domain.requests.cashier.MonthCashierIdRequest;
import io.example.cashier.domain.requests.cashier.MonthTotalSalesCashier;
import io.example.cashier.domain.requests.cashier.YearCashierIdRequest;
import io.example.cashier.domain.requests.cashier.YearTotalSalesCashier;
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.CashierStatByIdRepository;
import io.example.cashier.service.CashierStatsByIdService;
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
public class CashierStatsByIdServiceImpl implements CashierStatsByIdService {
    private final CashierStatByIdRepository statByIdRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "cashier:stats:id:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public Future<List<CashierResponseMonthTotalSales>> getMonthlyTotalSalesById(MonthTotalSalesCashier req) {
        var ctx = tracingMetrics.startSpan("CashierStatsByIdService.getMonthlyTotalSalesById");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_total:" + req.getYear() + ":" + req.getMonth() + ":cashier:"
                + req.getCashierId();

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
                    return statByIdRepository.getMonthlyTotalSalesById(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseMonthTotalSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyTotalSalesById", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getMonthlyTotalSalesById", e.getMessage()));
    }

    @Override
    public Future<List<CashierResponseYearTotalSales>> getYearlyTotalSalesById(YearTotalSalesCashier req) {
        var ctx = tracingMetrics.startSpan("CashierStatsByIdService.getYearlyTotalSalesById");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_total:" + req.getYear() + ":cashier:" + req.getCashierId();

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
                    return statByIdRepository.getYearlyTotalSalesById(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseYearTotalSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyTotalSalesById", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyTotalSalesById", e.getMessage()));
    }

    @Override
    public Future<List<CashierResponseMonthSales>> getMonthlyCashierById(MonthCashierIdRequest req) {
        var ctx = tracingMetrics.startSpan("CashierStatsByIdService.getMonthlyCashierById");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_cashier:" + req.getYear() + ":cashier:" + req.getCashierId();

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
                    return statByIdRepository.getMonthlyCashierById(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseMonthSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyCashierById", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getMonthlyCashierById", e.getMessage()));
    }

    @Override
    public Future<List<CashierResponseYearSales>> getYearlyCashierById(YearCashierIdRequest req) {
        var ctx = tracingMetrics.startSpan("CashierStatsByIdService.getYearlyCashierById");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_cashier:" + req.getYear() + ":cashier:" + req.getCashierId();

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
                    return statByIdRepository.getYearlyCashierById(req)
                            .compose(list -> redisService
                                    .setJson(cacheKey, new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(CashierResponseYearSales::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyCashierById", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyCashierById", e.getMessage()));
    }
}