package io.example.category.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.category.domain.requests.MonthTotalPrice;
import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.CategoryStatsRepository;
import io.example.category.service.CategoryStatsService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CategoryStatsServiceImpl implements CategoryStatsService {
    private static final Logger log = LoggerFactory.getLogger(CategoryStatsServiceImpl.class);

    private final CategoryStatsRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "category:stats:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public Future<List<CategoriesMonthlyTotalPriceResponse>> getMonthlyTotalPrice(MonthTotalPrice req) {
        var ctx = tracingMetrics.startSpan("CategoryStatsService.getMonthlyTotalPrice");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_total:" + req.getYear() + ":" + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("category.cache_hit", true);
                        log.info("Cache HIT for key: {}", cacheKey);
                        JsonArray arr = new JsonArray(cached);
                        List<CategoryMonthTotalPrice> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CategoryMonthTotalPrice.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(CategoriesMonthlyTotalPriceResponse::from).toList());
                    }
                    span.setAttribute("category.cache_hit", false);
                    log.debug("Cache MISS for key: {}, fetching from DB", cacheKey);
                    return statsRepository.getMonthlyTotalPrice(req)
                            .compose(list -> {
                                log.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesMonthlyTotalPriceResponse::from)
                                                .toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyTotalPrice", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getMonthlyTotalPrice", e.getMessage()));
    }

    @Override
    public Future<List<CategoriesYearlyTotalPriceResponse>> getYearlyTotalPrice(int year) {
        var ctx = tracingMetrics.startSpan("CategoryStatsService.getYearlyTotalPrice");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_total:" + year;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("category.cache_hit", true);
                        log.info("Cache HIT for key: {}", cacheKey);
                        JsonArray arr = new JsonArray(cached);
                        List<CategoryYearTotalPrice> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CategoryYearTotalPrice.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(CategoriesYearlyTotalPriceResponse::from).toList());
                    }
                    span.setAttribute("category.cache_hit", false);
                    log.debug("Cache MISS for key: {}, fetching from DB", cacheKey);
                    return statsRepository.getYearlyTotalPrice(year)
                            .compose(list -> {
                                log.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesYearlyTotalPriceResponse::from).toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyTotalPrice", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyTotalPrice", e.getMessage()));
    }

    @Override
    public Future<List<CategoriesMonthPriceResponse>> getMonthlyCategory(int year) {
        var ctx = tracingMetrics.startSpan("CategoryStatsService.getMonthlyCategory");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_category:" + year;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("category.cache_hit", true);
                        log.info("Cache HIT for key: {}", cacheKey);
                        JsonArray arr = new JsonArray(cached);
                        List<CategoryMonthPrice> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CategoryMonthPrice.class))
                                .toList();
                        return Future.succeededFuture(data.stream().map(CategoriesMonthPriceResponse::from).toList());
                    }
                    span.setAttribute("category.cache_hit", false);
                    log.debug("Cache MISS for key: {}, fetching from DB", cacheKey);
                    return statsRepository.getMonthlyCategory(year)
                            .compose(list -> {
                                log.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesMonthPriceResponse::from).toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyCategory", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getMonthlyCategory", e.getMessage()));
    }

    @Override
    public Future<List<CategoriesYearPriceResponse>> getYearlyCategory(int year) {
        var ctx = tracingMetrics.startSpan("CategoryStatsService.getYearlyCategory");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_category:" + year;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("category.cache_hit", true);
                        log.info("Cache HIT for key: {}", cacheKey);
                        JsonArray arr = new JsonArray(cached);
                        List<CategoryYearPrice> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CategoryYearPrice.class))
                                .toList();
                        return Future.succeededFuture(data.stream().map(CategoriesYearPriceResponse::from).toList());
                    }
                    span.setAttribute("category.cache_hit", false);
                    log.debug("Cache MISS for key: {}, fetching from DB", cacheKey);
                    return statsRepository.getYearlyCategory(year)
                            .compose(list -> {
                                log.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesYearPriceResponse::from).toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyCategory", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyCategory", e.getMessage()));
    }
}