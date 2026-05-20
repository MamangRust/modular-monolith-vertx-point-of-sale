package io.example.category.repository;

import io.example.category.domain.requests.FindAllCategory;
import io.example.category.model.Category;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;

public interface CategoryQueryRepository {
    Future<PagedResult<Category>> getCategories(FindAllCategory req);
    Future<PagedResult<Category>> getCategoriesActive(FindAllCategory req);
    Future<PagedResult<Category>> getCategoriesTrashed(FindAllCategory req);
    Future<Category> getCategoryById(Long categoryId);
    Future<Category> getCategoryByName(String name);
    Future<Category> getCategoryByNameAndId(String name, Long categoryId);
}
