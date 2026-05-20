package io.example.category.service;

import io.example.category.domain.requests.FindAllCategory;
import io.example.category.model.Category;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;

public interface CategoryQueryService {
    Future<PagedResult<Category>> getCategories(FindAllCategory req);
    Future<PagedResult<Category>> getCategoriesActive(FindAllCategory req);
    Future<PagedResult<Category>> getTrashedCategories(FindAllCategory req);
    Future<Category> getCategoryById(Long categoryId);
}
