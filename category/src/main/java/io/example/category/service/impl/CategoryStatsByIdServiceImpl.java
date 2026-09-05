package io.example.category.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.category.domain.requests.MonthTotalPriceCategory;
import io.example.category.domain.requests.YearPriceId;
import io.example.category.domain.requests.YearTotalPriceCategory;
import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.CategoryStatsByIdRepository;
import io.example.category.service.CategoryStatsByIdService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CategoryStatsByIdServiceImpl implements CategoryStatsByIdService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryStatsByIdServiceImpl.class);

    private final CategoryStatsByIdRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "category:stats:id:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public Future<List<CategoriesMonthlyTotalPriceResponse>> getMonthlyTotalPriceById(MonthTotalPriceCategory req) {
        var ctx = tracingMetrics.startSpan("CategoryStatsByIdService.getMonthlyTotalPriceById",
                io.opentelemetry.api.common.Attributes.builder().put("category.id", req.getCategoryId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_total:" + req.getCategoryId() + ":" + req.getYear() + ":"
                + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("category.cache_hit", true);
                        logger.info("Cache HIT for key: {}", cacheKey);
                        JsonArray arr = new JsonArray(cached);
                        List<CategoryMonthTotalPrice> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CategoryMonthTotalPrice.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(CategoriesMonthlyTotalPriceResponse::from).toList());
                    }
                    span.setAttribute("category.cache_hit", false);
                    logger.debug("Cache MISS for key: {}, fetching from DB", cacheKey);
                    return statsRepository.getMonthlyTotalPriceById(req)
                            .compose(list -> {
                                logger.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesMonthlyTotalPriceResponse::from)
                                                .toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyTotalPriceById", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getMonthlyTotalPriceById", e.getMessage()));
    }

    @Override
    public Future<List<CategoriesYearlyTotalPriceResponse>> getYearlyTotalPriceById(YearTotalPriceCategory req) {
        var ctx = tracingMetrics.startSpan("CategoryStatsByIdService.getYearlyTotalPriceById",
                io.opentelemetry.api.common.Attributes.builder().put("category.id", req.getCategoryId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_total:" + req.getCategoryId() + ":" + req.getYear();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("category.cache_hit", true);
                        logger.info("Cache HIT for key: {}", cacheKey);
                        JsonArray arr = new JsonArray(cached);
                        List<CategoryYearTotalPrice> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CategoryYearTotalPrice.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(CategoriesYearlyTotalPriceResponse::from).toList());
                    }
                    span.setAttribute("category.cache_hit", false);
                    logger.debug("Cache MISS for key: {}, fetching from DB", cacheKey);
                    return statsRepository.getYearlyTotalPriceById(req)
                            .compose(list -> {
                                logger.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesYearlyTotalPriceResponse::from).toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyTotalPriceById", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyTotalPriceById", e.getMessage()));
    }

    @Override
    public Future<List<CategoriesMonthPriceResponse>> getMonthlyCategoryById(YearPriceId req) {
        var ctx = tracingMetrics.startSpan("CategoryStatsByIdService.getMonthlyCategoryById",
                io.opentelemetry.api.common.Attributes.builder().put("category.id", req.getCategoryId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_cat:" + req.getCategoryId() + ":" + req.getYear();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("category.cache_hit", true);
                        logger.info("Cache HIT for key: {}", cacheKey);
                        JsonArray arr = new JsonArray(cached);
                        List<CategoryMonthPrice> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CategoryMonthPrice.class))
                                .toList();
                        return Future.succeededFuture(data.stream().map(CategoriesMonthPriceResponse::from).toList());
                    }
                    span.setAttribute("category.cache_hit", false);
                    logger.debug("Cache MISS for key: {}, fetching from DB", cacheKey);
                    return statsRepository.getMonthlyCategoryById(req)
                            .compose(list -> {
                                logger.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesMonthPriceResponse::from).toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyCategoryById", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getMonthlyCategoryById", e.getMessage()));
    }

    @Override
    public Future<List<CategoriesYearPriceResponse>> getYearlyCategoryById(YearPriceId req) {
        var ctx = tracingMetrics.startSpan("CategoryStatsByIdService.getYearlyCategoryById",
                io.opentelemetry.api.common.Attributes.builder().put("category.id", req.getCategoryId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_cat:" + req.getCategoryId() + ":" + req.getYear();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("category.cache_hit", true);
                        logger.info("Cache HIT for key: {}", cacheKey);
                        JsonArray arr = new JsonArray(cached);
                        List<CategoryYearPrice> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(CategoryYearPrice.class))
                                .toList();
                        return Future.succeededFuture(data.stream().map(CategoriesYearPriceResponse::from).toList());
                    }
                    span.setAttribute("category.cache_hit", false);
                    logger.debug("Cache MISS for key: {}, fetching from DB", cacheKey);
                    return statsRepository.getYearlyCategoryById(req)
                            .compose(list -> {
                                logger.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesYearPriceResponse::from).toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyCategoryById", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyCategoryById", e.getMessage()));
    }
}