package io.example.category.service.impl;

import io.example.category.domain.requests.CreateCategoryRequest;
import io.example.category.domain.requests.UpdateCategoryRequest;
import io.example.category.domain.response.category.CategoryResponse;
import io.example.category.domain.response.category.CategoryResponseDeleteAt;
import io.example.category.model.Category;
import io.example.category.repository.CategoryCommandRepository;
import io.example.category.repository.CategoryQueryRepository;
import io.example.category.service.CategoryCommandService;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CategoryCommandServiceImpl implements CategoryCommandService {
    private final CategoryCommandRepository commandRepository;
    private final CategoryQueryRepository queryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "category:";
    private static final String CACHE_LIST_PREFIX = "categories:list:";

    @Override
    public Future<CategoryResponse> createCategory(CreateCategoryRequest req) {
        var ctx = tracingMetrics.startSpan("CategoryCommandService.createCategory");

        String slug = generateSlug(req.getName());
        req.setSlugCategory(slug);

        return queryRepository.getCategoryByName(req.getName())
                .compose(existing -> {
                    if (existing != null) {
                        return Future.failedFuture(new BadRequestException("Category name already exists"));
                    }
                    return commandRepository.createCategory(req);
                })
                .compose(cat -> invalidateListCache().<Category>map(v -> cat))
                .map(CategoryResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "create", "Category created successfully"))
                .onFailure(err -> {
                    log.error("Failed to create category", err);
                    tracingMetrics.completeSpanError(ctx, "create", err.getMessage());
                });
    }

    @Override
    public Future<CategoryResponse> updateCategory(UpdateCategoryRequest req) {
        var ctx = tracingMetrics.startSpan(
                "CategoryCommandService.updateCategory",
                Attributes.builder().put("category.id", req.getCategoryId()).build());

        // Only generate slug and validate name uniqueness when name is provided
        if (req.getName() != null && !req.getName().isBlank()) {
            req.setSlugCategory(generateSlug(req.getName()));
        }

        return queryRepository.getCategoryById(req.getCategoryId().longValue())
                .compose(existing -> {
                    if (existing == null) {
                        return Future.failedFuture(new NotFoundException("Category not found"));
                    }
                    // Only check name uniqueness when name is being changed
                    if (req.getName() == null || req.getName().isBlank()) {
                        return commandRepository.updateCategory(req);
                    }
                    return queryRepository.getCategoryByName(req.getName())
                            .compose(checkName -> {
                                if (checkName != null && !checkName.getCategoryId().equals(req.getCategoryId())) {
                                    return Future.failedFuture(
                                            new BadRequestException("Category name already used by another category"));
                                }
                                return commandRepository.updateCategory(req);
                            });
                })
                .compose(cat -> invalidateCache(req.getCategoryId().longValue()).<Category>map(v -> cat))
                .map(CategoryResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update", "Category updated successfully"))
                .onFailure(err -> {
                    log.error("Failed to update category: {}", req.getCategoryId(), err);
                    tracingMetrics.completeSpanError(ctx, "update", err.getMessage());
                });
    }

    @Override
    public Future<CategoryResponseDeleteAt> trashCategory(Long categoryId) {
        var ctx = tracingMetrics.startSpan(
                "CategoryCommandService.trashCategory",
                Attributes.builder().put("category.id", categoryId).build());

        return commandRepository.trashCategory(categoryId)
                .compose(cat -> {
                    if (cat == null) {
                        return Future.failedFuture(new NotFoundException("Category not found or already trashed"));
                    }
                    return invalidateCache(categoryId).<Category>map(v -> cat);
                })
                .map(CategoryResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "trash", "Category trashed successfully"))
                .onFailure(err -> {
                    log.error("Failed to trash category: {}", categoryId, err);
                    tracingMetrics.completeSpanError(ctx, "trash", err.getMessage());
                });
    }

    @Override
    public Future<CategoryResponseDeleteAt> restoreCategory(Long categoryId) {
        var ctx = tracingMetrics.startSpan(
                "CategoryCommandService.restoreCategory",
                Attributes.builder().put("category.id", categoryId).build());

        return queryRepository.findByTrashedId(categoryId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.failedFuture(
                                new BadRequestException("Category not found or must be trashed first"));
                    }
                    return commandRepository.restoreCategory(categoryId);
                })
                .compose(r -> {
                    if (r == null) {
                        return Future.<Category>failedFuture(new NotFoundException("Category not found"));
                    }
                    return invalidateCache(categoryId).<Category>map(v -> r);
                })
                .map(CategoryResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restoreCategory", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreCategory", e.getMessage()));
    }

    @Override
    public Future<Boolean> deleteCategoryPermanently(Long categoryId) {
        var ctx = tracingMetrics.startSpan(
                "CategoryCommandService.deleteCategoryPermanently",
                Attributes.builder().put("category.id", categoryId).build());

        return queryRepository.findByTrashedId(categoryId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.<Boolean>failedFuture(
                                new BadRequestException(
                                        "Category not found or must be trashed before permanent deletion"));
                    }
                    return commandRepository.deleteCategoryPermanently(categoryId)
                            .compose(deleted -> {
                                if (!deleted) {
                                    return Future.<Boolean>failedFuture(
                                            new BadRequestException("Failed to delete category permanently"));
                                }
                                return invalidateCache(categoryId).<Boolean>map(v -> deleted);
                            });
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deleteCategoryPermanently",
                        "Category permanently deleted"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "deleteCategoryPermanently", err.getMessage()));
    }

    @Override
    public Future<Void> restoreAllCategories() {
        var ctx = tracingMetrics.startSpan("CategoryCommandService.restoreAllCategories");

        return commandRepository.restoreAllCategories()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed categories found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all",
                        "All categories restored successfully"))
                .onFailure(err -> {
                    log.error("Failed to restore all categories", err);
                    tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
                });
    }

    @Override
    public Future<Void> deleteAllPermanentCategories() {
        var ctx = tracingMetrics
                .startSpan("CategoryCommandService.deleteAllPermanentCategories");

        return commandRepository.deleteAllPermanentCategories()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed categories found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "delete_all",
                        "All categories permanently deleted"))
                .onFailure(err -> {
                    log.error("Failed to permanently delete all categories", err);
                    tracingMetrics.completeSpanError(ctx, "delete_all", err.getMessage());
                });
    }

    private Future<Void> invalidateCache(Long categoryId) {
        return redisService.delete(CACHE_PREFIX + categoryId)
                .compose(v -> redisService.deleteByPattern(CACHE_LIST_PREFIX + "*"))
                .<Void>mapEmpty();
    }

    private Future<Void> invalidateListCache() {
        return redisService.deleteByPattern(CACHE_LIST_PREFIX + "*").<Void>mapEmpty();
    }

    private String generateSlug(String name) {
        if (name == null)
            return "";
        return name.toLowerCase().trim().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
    }
}