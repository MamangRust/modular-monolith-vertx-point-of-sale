package io.example.category.service;

import io.example.category.domain.requests.CreateCategoryRequest;
import io.example.category.domain.requests.UpdateCategoryRequest;
import io.example.category.domain.response.category.CategoryResponse;
import io.example.category.domain.response.category.CategoryResponseDeleteAt;
import io.vertx.core.Future;

public interface CategoryCommandService {
    Future<CategoryResponse> createCategory(CreateCategoryRequest req);

    Future<CategoryResponse> updateCategory(UpdateCategoryRequest req);

    Future<CategoryResponseDeleteAt> trashCategory(Long categoryId);

    Future<CategoryResponseDeleteAt> restoreCategory(Long categoryId);

    Future<Boolean> deleteCategoryPermanently(Long categoryId);

    Future<Void> restoreAllCategories();

    Future<Void> deleteAllPermanentCategories();
}
