package io.example.category.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.category.domain.requests.MonthPriceMerchant;
import io.example.category.domain.requests.MonthTotalPriceMerchant;
import io.example.category.domain.requests.YearPriceMerchant;
import io.example.category.domain.requests.YearTotalPriceMerchant;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.CategoryStatsByMerchantRepository;
import io.example.category.service.CategoryStatsByMerchantService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
public class CategoryStatsByMerchantServiceImpl implements CategoryStatsByMerchantService {
    private final CategoryStatsByMerchantRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryStatsByMerchantServiceImpl(CategoryStatsByMerchantRepository statsRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.statsRepository = statsRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<List<CategoryMonthTotalPrice>> getMonthlyTotalPriceByMerchant(MonthTotalPriceMerchant req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getMonthlyTotalPriceByMerchant");
        String cacheKey = String.format("category:report:monthly_total_merchant:%d:%d:%d", req.getMerchantId(), req.getYear(), req.getMonth());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getMonthlyTotalPriceByMerchant(req),
                        new TypeReference<List<CategoryMonthTotalPrice>>() {}, tracingCtx, "report_monthly_total_merchant"))
                .recover(err -> handleError(tracingCtx, "report_monthly_total_merchant", err));
    }

    @Override
    public Future<List<CategoryYearTotalPrice>> getYearlyTotalPriceByMerchant(YearTotalPriceMerchant req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getYearlyTotalPriceByMerchant");
        String cacheKey = String.format("category:report:yearly_total_merchant:%d:%d", req.getMerchantId(), req.getYear());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getYearlyTotalPriceByMerchant(req),
                        new TypeReference<List<CategoryYearTotalPrice>>() {}, tracingCtx, "report_yearly_total_merchant"))
                .recover(err -> handleError(tracingCtx, "report_yearly_total_merchant", err));
    }

    @Override
    public Future<List<CategoryMonthPrice>> getMonthlyCategoryByMerchant(MonthPriceMerchant req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getMonthlyCategoryByMerchant");
        String cacheKey = String.format("category:report:monthly_category_merchant:%d:%d", req.getMerchantId(), req.getYear());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getMonthlyCategoryByMerchant(req),
                        new TypeReference<List<CategoryMonthPrice>>() {}, tracingCtx, "report_monthly_category_merchant"))
                .recover(err -> handleError(tracingCtx, "report_monthly_category_merchant", err));
    }

    @Override
    public Future<List<CategoryYearPrice>> getYearlyCategoryByMerchant(YearPriceMerchant req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getYearlyCategoryByMerchant");
        String cacheKey = String.format("category:report:yearly_category_merchant:%d:%d", req.getMerchantId(), req.getYear());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getYearlyCategoryByMerchant(req),
                        new TypeReference<List<CategoryYearPrice>>() {}, tracingCtx, "report_yearly_category_merchant"))
                .recover(err -> handleError(tracingCtx, "report_yearly_category_merchant", err));
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
        log.error("Category stats by merchant service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
