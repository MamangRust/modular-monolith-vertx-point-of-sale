package io.example.category.service;

import io.example.category.domain.requests.CreateCategoryRequest;
import io.example.category.domain.requests.UpdateCategoryRequest;
import io.example.category.model.Category;
import io.vertx.core.Future;

public interface CategoryCommandService {
    Future<Category> createCategory(CreateCategoryRequest req);
    Future<Category> updateCategory(UpdateCategoryRequest req);
    Future<Category> trashCategory(Long categoryId);
    Future<Category> restoreCategory(Long categoryId);
    Future<Boolean> deleteCategoryPermanently(Long categoryId);
    Future<Boolean> restoreAllCategories();
    Future<Boolean> deleteAllPermanentCategories();
}
