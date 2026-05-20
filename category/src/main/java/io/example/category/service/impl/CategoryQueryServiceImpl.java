package io.example.category.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.category.domain.requests.FindAllCategory;
import io.example.category.model.Category;
import io.example.category.repository.CategoryQueryRepository;
import io.example.category.service.CategoryQueryService;
import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class CategoryQueryServiceImpl implements CategoryQueryService {
    private final CategoryQueryRepository queryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CategoryQueryServiceImpl(CategoryQueryRepository queryRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.queryRepository = queryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<PagedResult<Category>> getCategories(FindAllCategory req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getCategories");
        String cacheKey = String.format("categories:list:%s:%d:%d", req.getSearch() != null ? req.getSearch() : "", req.getPage(), req.getPageSize());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getCategories(req),
                        new TypeReference<PagedResult<Category>>() {}, tracingCtx, "get_categories"))
                .recover(err -> handleError(tracingCtx, "get_categories", err));
    }

    @Override
    public Future<PagedResult<Category>> getCategoriesActive(FindAllCategory req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getCategoriesActive");
        String cacheKey = String.format("categories:active:%s:%d:%d", req.getSearch() != null ? req.getSearch() : "", req.getPage(), req.getPageSize());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getCategoriesActive(req),
                        new TypeReference<PagedResult<Category>>() {}, tracingCtx, "get_categories_active"))
                .recover(err -> handleError(tracingCtx, "get_categories_active", err));
    }

    @Override
    public Future<PagedResult<Category>> getTrashedCategories(FindAllCategory req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getTrashedCategories");
        String cacheKey = String.format("categories:trashed:%s:%d:%d", req.getSearch() != null ? req.getSearch() : "", req.getPage(), req.getPageSize());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getCategoriesTrashed(req),
                        new TypeReference<PagedResult<Category>>() {}, tracingCtx, "get_trashed_categories"))
                .recover(err -> handleError(tracingCtx, "get_trashed_categories", err));
    }

    @Override
    public Future<Category> getCategoryById(Long categoryId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.getCategoryById");
        String cacheKey = "category:detail:" + categoryId;

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getCategoryById(categoryId).compose(res -> {
                            if (res == null) {
                                return Future.failedFuture(new io.example.common.exception.NotFoundException("Category not found"));
                            }
                            return Future.succeededFuture(res);
                        }),
                        new TypeReference<Category>() {}, tracingCtx, "get_category_by_id"))
                .recover(err -> handleError(tracingCtx, "get_category_by_id", err));
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
        log.error("Category query service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
