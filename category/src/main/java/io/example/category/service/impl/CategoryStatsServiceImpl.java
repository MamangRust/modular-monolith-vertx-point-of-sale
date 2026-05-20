package io.example.category.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.category.domain.requests.MonthTotalPrice;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.CategoryStatsRepository;
import io.example.category.service.CategoryStatsService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
public class CategoryStatsServiceImpl implements CategoryStatsService {
    private final CategoryStatsRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryStatsServiceImpl(CategoryStatsRepository statsRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.statsRepository = statsRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<List<CategoryMonthTotalPrice>> getMonthlyTotalPrice(MonthTotalPrice req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getMonthlyTotalPrice");
        String cacheKey = String.format("category:report:monthly_total:%d:%d", req.getYear(), req.getMonth());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getMonthlyTotalPrice(req),
                        new TypeReference<List<CategoryMonthTotalPrice>>() {}, tracingCtx, "report_monthly_total"))
                .recover(err -> handleError(tracingCtx, "report_monthly_total", err));
    }

    @Override
    public Future<List<CategoryYearTotalPrice>> getYearlyTotalPrice(int year) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getYearlyTotalPrice");
        String cacheKey = String.format("category:report:yearly_total:%d", year);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getYearlyTotalPrice(year),
                        new TypeReference<List<CategoryYearTotalPrice>>() {}, tracingCtx, "report_yearly_total"))
                .recover(err -> handleError(tracingCtx, "report_yearly_total", err));
    }

    @Override
    public Future<List<CategoryMonthPrice>> getMonthlyCategory(int year) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getMonthlyCategory");
        String cacheKey = String.format("category:report:monthly_category:%d", year);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getMonthlyCategory(year),
                        new TypeReference<List<CategoryMonthPrice>>() {}, tracingCtx, "report_monthly_category"))
                .recover(err -> handleError(tracingCtx, "report_monthly_category", err));
    }

    @Override
    public Future<List<CategoryYearPrice>> getYearlyCategory(int year) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getYearlyCategory");
        String cacheKey = String.format("category:report:yearly_category:%d", year);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getYearlyCategory(year),
                        new TypeReference<List<CategoryYearPrice>>() {}, tracingCtx, "report_yearly_category"))
                .recover(err -> handleError(tracingCtx, "report_yearly_category", err));
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
        log.error("Category stats service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
