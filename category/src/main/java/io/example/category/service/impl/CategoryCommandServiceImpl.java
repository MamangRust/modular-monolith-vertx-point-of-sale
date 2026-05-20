package io.example.category.service.impl;

import io.example.category.domain.requests.CreateCategoryRequest;
import io.example.category.domain.requests.UpdateCategoryRequest;
import io.example.category.model.Category;
import io.example.category.repository.CategoryCommandRepository;
import io.example.category.repository.CategoryQueryRepository;
import io.example.category.service.CategoryCommandService;
import io.example.common.exception.BadRequestException;
import io.example.common.exception.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CategoryCommandServiceImpl implements CategoryCommandService {
    private final CategoryCommandRepository commandRepository;
    private final CategoryQueryRepository queryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    public CategoryCommandServiceImpl(CategoryCommandRepository commandRepository, CategoryQueryRepository queryRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.commandRepository = commandRepository;
        this.queryRepository = queryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<Category> createCategory(CreateCategoryRequest req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.createCategory");
        String slug = generateSlug(req.getName());
        req.setSlugCategory(slug);

        return queryRepository.getCategoryByName(req.getName())
                .compose(existing -> {
                    if (existing != null) {
                        return Future.failedFuture(new BadRequestException("Category name already exists"));
                    }
                    return commandRepository.createCategory(req);
                })
                .map(cat -> {
                    invalidateCache(cat.getCategoryId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "create_category", "Created");
                    return cat;
                })
                .recover(err -> handleError(tracingCtx, "create_category", err));
    }

    @Override
    public Future<Category> updateCategory(UpdateCategoryRequest req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.updateCategory");
        String slug = generateSlug(req.getName());
        req.setSlugCategory(slug);

        return queryRepository.getCategoryById(req.getCategoryId().longValue())
                .compose(existing -> {
                    if (existing == null) {
                        return Future.failedFuture(new NotFoundException("Category not found"));
                    }
                    return queryRepository.getCategoryByName(req.getName())
                            .compose(checkName -> {
                                if (checkName != null && !checkName.getCategoryId().equals(req.getCategoryId().longValue())) {
                                    return Future.failedFuture(new BadRequestException("Category name already used by another category"));
                                }
                                return commandRepository.updateCategory(req);
                            });
                })
                .map(cat -> {
                    invalidateCache(req.getCategoryId().longValue());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "update_category", "Updated");
                    return cat;
                })
                .recover(err -> handleError(tracingCtx, "update_category", err));
    }

    @Override
    public Future<Category> trashCategory(Long categoryId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.trashCategory");

        return commandRepository.trashCategory(categoryId)
                .map(cat -> {
                    if (cat == null)
                        throw new NotFoundException("Category not found or already trashed");
                    invalidateCache(categoryId);
                    tracingMetrics.completeSpanSuccess(tracingCtx, "trash_category", "Trashed");
                    return cat;
                })
                .recover(err -> handleError(tracingCtx, "trash_category", err));
    }

    @Override
    public Future<Category> restoreCategory(Long categoryId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.restoreCategory");

        return commandRepository.restoreCategory(categoryId)
                .map(cat -> {
                    if (cat == null)
                        throw new NotFoundException("Category not found or not in trash");
                    invalidateCache(categoryId);
                    tracingMetrics.completeSpanSuccess(tracingCtx, "restore_category", "Restored");
                    return cat;
                })
                .recover(err -> handleError(tracingCtx, "restore_category", err));
    }

    @Override
    public Future<Boolean> deleteCategoryPermanently(Long categoryId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.deleteCategoryPermanently");

        return commandRepository.deleteCategoryPermanently(categoryId)
                .map(res -> {
                    invalidateCache(categoryId);
                    tracingMetrics.completeSpanSuccess(tracingCtx, "delete_permanent", "Deleted");
                    return res;
                })
                .recover(err -> handleError(tracingCtx, "delete_permanent", err));
    }

    @Override
    public Future<Boolean> restoreAllCategories() {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.restoreAllCategories");

        return commandRepository.restoreAllCategories()
                .map(res -> {
                    redisService.delete("categories:list:");
                    tracingMetrics.completeSpanSuccess(tracingCtx, "restore_all", "Success");
                    return res;
                })
                .recover(err -> handleError(tracingCtx, "restore_all", err));
    }

    @Override
    public Future<Boolean> deleteAllPermanentCategories() {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CategoryService.deleteAllPermanentCategories");

        return commandRepository.deleteAllPermanentCategories()
                .map(res -> {
                    redisService.delete("categories:list:");
                    tracingMetrics.completeSpanSuccess(tracingCtx, "delete_all", "Success");
                    return res;
                })
                .recover(err -> handleError(tracingCtx, "delete_all", err));
    }

    private void invalidateCache(Long categoryId) {
        if (categoryId != null) {
            redisService.delete("category:detail:" + categoryId);
        }
        redisService.delete("categories:list:");
    }

    private String generateSlug(String name) {
        if (name == null)
            return "";
        return name.toLowerCase().trim().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
    }

    private <T> Future<T> handleError(TracingMetrics.TracingContext ctx, String operation, Throwable err) {
        log.error("Category command service error in {}: {}", operation, err.getMessage());
        tracingMetrics.completeSpanError(ctx, operation, err.getMessage());
        return Future.failedFuture(err);
    }
}
