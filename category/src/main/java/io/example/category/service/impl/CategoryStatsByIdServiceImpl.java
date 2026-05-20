package io.example.category.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.category.domain.requests.MonthTotalPriceCategory;
import io.example.category.domain.requests.YearPriceId;
import io.example.category.domain.requests.YearTotalPriceCategory;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.CategoryStatsByIdRepository;
import io.example.category.service.CategoryStatsByIdService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
public class CategoryStatsByIdServiceImpl implements CategoryStatsByIdService {
    private final CategoryStatsByIdRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryStatsByIdServiceImpl(CategoryStatsByIdRepository statsRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.statsRepository = statsRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<List<CategoryMonthTotalPrice>> getMonthlyTotalPriceById(MonthTotalPriceCategory req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getMonthlyTotalPriceById");
        String cacheKey = String.format("category:report:monthly_total_id:%d:%d:%d", req.getCategoryId(), req.getYear(), req.getMonth());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getMonthlyTotalPriceById(req),
                        new TypeReference<List<CategoryMonthTotalPrice>>() {}, tracingCtx, "report_monthly_total_id"))
                .recover(err -> handleError(tracingCtx, "report_monthly_total_id", err));
    }

    @Override
    public Future<List<CategoryYearTotalPrice>> getYearlyTotalPriceById(YearTotalPriceCategory req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getYearlyTotalPriceById");
        String cacheKey = String.format("category:report:yearly_total_id:%d:%d", req.getCategoryId(), req.getYear());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getYearlyTotalPriceById(req),
                        new TypeReference<List<CategoryYearTotalPrice>>() {}, tracingCtx, "report_yearly_total_id"))
                .recover(err -> handleError(tracingCtx, "report_yearly_total_id", err));
    }

    @Override
    public Future<List<CategoryMonthPrice>> getMonthlyCategoryById(YearPriceId req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getMonthlyCategoryById");
        String cacheKey = String.format("category:report:monthly_category_id:%d:%d", req.getCategoryId(), req.getYear());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getMonthlyCategoryById(req),
                        new TypeReference<List<CategoryMonthPrice>>() {}, tracingCtx, "report_monthly_category_id"))
                .recover(err -> handleError(tracingCtx, "report_monthly_category_id", err));
    }

    @Override
    public Future<List<CategoryYearPrice>> getYearlyCategoryById(YearPriceId req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getYearlyCategoryById");
        String cacheKey = String.format("category:report:yearly_category_id:%d:%d", req.getCategoryId(), req.getYear());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getYearlyCategoryById(req),
                        new TypeReference<List<CategoryYearPrice>>() {}, tracingCtx, "report_yearly_category_id"))
                .recover(err -> handleError(tracingCtx, "report_yearly_category_id", err));
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
        log.error("Category stats by ID service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
