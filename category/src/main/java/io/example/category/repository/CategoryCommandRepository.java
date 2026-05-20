package io.example.category.repository;

import io.example.category.domain.requests.CreateCategoryRequest;
import io.example.category.domain.requests.UpdateCategoryRequest;
import io.example.category.model.Category;
import io.vertx.core.Future;

public interface CategoryCommandRepository {
    Future<Category> createCategory(CreateCategoryRequest req);
    Future<Category> updateCategory(UpdateCategoryRequest req);
    Future<Category> trashCategory(Long categoryId);
    Future<Category> restoreCategory(Long categoryId);
    Future<Boolean> deleteCategoryPermanently(Long categoryId);
    Future<Boolean> restoreAllCategories();
    Future<Boolean> deleteAllPermanentCategories();
}
