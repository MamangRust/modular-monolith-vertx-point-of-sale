package io.example.category.service.impl;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.category.domain.requests.FindAllCategory;
import io.example.category.domain.response.category.CategoryResponse;
import io.example.category.domain.response.category.CategoryResponseDeleteAt;
import io.example.category.model.Category;
import io.example.category.repository.CategoryQueryRepository;
import io.example.category.service.CategoryQueryService;
import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CategoryQueryServiceImpl implements CategoryQueryService {
    private static final Logger log = LoggerFactory.getLogger(CategoryQueryServiceImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final CategoryQueryRepository queryRepository;
    private final RedisService redis;
    private final TracingMetrics metrics;

    private static final String CACHE_PREFIX = "category:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    // Helper Methods untuk Mapping
    private PagedResult<CategoryResponse> mapPagination(PagedResult<Category> res) {
        List<CategoryResponse> data = res.getData().stream().map(CategoryResponse::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    private PagedResult<CategoryResponseDeleteAt> mapPaginationDeleteAt(PagedResult<Category> res) {
        List<CategoryResponseDeleteAt> data = res.getData().stream().map(CategoryResponseDeleteAt::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    @Override
    public Future<PagedResult<CategoryResponse>> getCategories(FindAllCategory req) {
        var ctx = metrics.startSpan("CategoryQueryService.getCategories");
        String cacheKey = CACHE_PREFIX + "list:all:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Category> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Category>>() {
                                    });
                            return Future.succeededFuture(mapPagination(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached categories: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getCategories(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPagination);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCategories", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getCategories", e.getMessage()));
    }

    @Override
    public Future<CategoryResponse> getCategoryById(Long categoryId) {
        var ctx = metrics.startSpan("CategoryQueryService.getCategoryById",
                Attributes.builder().put("category.id", categoryId).build());
        String key = CACHE_PREFIX + categoryId;

        return redis.getJson(key, Category.class)
                .compose(cached -> {
                    if (cached != null) {
                        return Future.succeededFuture(CategoryResponse.from(cached));
                    }
                    return queryRepository.getCategoryById(categoryId)
                            .compose(db -> {
                                if (db == null) {
                                    return Future.<Category>failedFuture(new NotFoundException("Category not found"));
                                }
                                return redis.setJson(key, db, CACHE_TTL).<Category>map(v -> db);
                            })
                            .map(CategoryResponse::from);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCategoryById", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getCategoryById", e.getMessage()));
    }

    @Override
    public Future<PagedResult<CategoryResponseDeleteAt>> getCategoriesActive(FindAllCategory req) {
        var ctx = metrics.startSpan("CategoryQueryService.getCategoriesActive");
        String cacheKey = CACHE_PREFIX + "list:active:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Category> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Category>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached active categories: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getCategoriesActive(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPaginationDeleteAt);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCategoriesActive", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getCategoriesActive", e.getMessage()));
    }

    @Override
    public Future<PagedResult<CategoryResponseDeleteAt>> getTrashedCategories(FindAllCategory req) {
        var ctx = metrics.startSpan("CategoryQueryService.getTrashedCategories");
        String cacheKey = CACHE_PREFIX + "list:trashed:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Category> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Category>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached trashed categories: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getCategoriesTrashed(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPaginationDeleteAt);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTrashedCategories", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getTrashedCategories", e.getMessage()));
    }
}