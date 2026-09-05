package io.example.category.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.category.domain.requests.MonthPriceMerchant;
import io.example.category.domain.requests.MonthTotalPriceMerchant;
import io.example.category.domain.requests.YearPriceMerchant;
import io.example.category.domain.requests.YearTotalPriceMerchant;
import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.CategoryStatsByMerchantRepository;
import io.example.category.service.CategoryStatsByMerchantService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CategoryStatsByMerchantServiceImpl implements CategoryStatsByMerchantService {
    private static final Logger logger = LoggerFactory.getLogger(CategoryStatsByMerchantServiceImpl.class);

    private final CategoryStatsByMerchantRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "category:stats:merchant:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public Future<List<CategoriesMonthlyTotalPriceResponse>> getMonthlyTotalPriceByMerchant(
            MonthTotalPriceMerchant req) {
        var ctx = tracingMetrics.startSpan("CategoryStatsByMerchantService.getMonthlyTotalPriceByMerchant",
                io.opentelemetry.api.common.Attributes.builder().put("merchant.id", req.getMerchantId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_total:" + req.getMerchantId() + ":" + req.getYear() + ":"
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
                    return statsRepository.getMonthlyTotalPriceByMerchant(req)
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
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyTotalPriceByMerchant", "Success"))
                .onFailure(
                        e -> tracingMetrics.completeSpanError(ctx, "getMonthlyTotalPriceByMerchant", e.getMessage()));
    }

    @Override
    public Future<List<CategoriesYearlyTotalPriceResponse>> getYearlyTotalPriceByMerchant(YearTotalPriceMerchant req) {
        var ctx = tracingMetrics.startSpan("CategoryStatsByMerchantService.getYearlyTotalPriceByMerchant",
                io.opentelemetry.api.common.Attributes.builder().put("merchant.id", req.getMerchantId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_total:" + req.getMerchantId() + ":" + req.getYear();

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
                    return statsRepository.getYearlyTotalPriceByMerchant(req)
                            .compose(list -> {
                                logger.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesYearlyTotalPriceResponse::from).toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyTotalPriceByMerchant", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyTotalPriceByMerchant", e.getMessage()));
    }

    @Override
    public Future<List<CategoriesMonthPriceResponse>> getMonthlyCategoryByMerchant(MonthPriceMerchant req) {
        var ctx = tracingMetrics.startSpan("CategoryStatsByMerchantService.getMonthlyCategoryByMerchant",
                io.opentelemetry.api.common.Attributes.builder().put("merchant.id", req.getMerchantId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_cat:" + req.getMerchantId() + ":" + req.getYear();

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
                    return statsRepository.getMonthlyCategoryByMerchant(req)
                            .compose(list -> {
                                logger.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesMonthPriceResponse::from).toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getMonthlyCategoryByMerchant", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getMonthlyCategoryByMerchant", e.getMessage()));
    }

    @Override
    public Future<List<CategoriesYearPriceResponse>> getYearlyCategoryByMerchant(YearPriceMerchant req) {
        var ctx = tracingMetrics.startSpan("CategoryStatsByMerchantService.getYearlyCategoryByMerchant",
                io.opentelemetry.api.common.Attributes.builder().put("merchant.id", req.getMerchantId()).build());
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_cat:" + req.getMerchantId() + ":" + req.getYear();

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
                    return statsRepository.getYearlyCategoryByMerchant(req)
                            .compose(list -> {
                                logger.debug("Data fetched from DB, caching key: {}", cacheKey);
                                return redisService
                                        .setJson(cacheKey,
                                                new JsonArray(list.stream().map(JsonObject::mapFrom).toList()),
                                                CACHE_TTL)
                                        .map(v -> list.stream().map(CategoriesYearPriceResponse::from).toList());
                            });
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "getYearlyCategoryByMerchant", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "getYearlyCategoryByMerchant", e.getMessage()));
    }
}