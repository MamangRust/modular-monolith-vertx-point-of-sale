package io.example.category.service;

import io.example.category.domain.requests.FindAllCategory;
import io.example.category.domain.response.category.CategoryResponse;
import io.example.category.domain.response.category.CategoryResponseDeleteAt;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;

public interface CategoryQueryService {
    Future<PagedResult<CategoryResponse>> getCategories(FindAllCategory req);

    Future<PagedResult<CategoryResponseDeleteAt>> getCategoriesActive(FindAllCategory req);

    Future<PagedResult<CategoryResponseDeleteAt>> getTrashedCategories(FindAllCategory req);

    Future<CategoryResponse> getCategoryById(Long categoryId);
}
