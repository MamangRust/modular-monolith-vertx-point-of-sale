package io.example.category.repository.impl;

import io.example.category.domain.requests.FindAllCategory;
import io.example.category.model.Category;
import io.example.category.repository.CategoryQueryRepository;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class CategoryQueryRepositoryImpl implements CategoryQueryRepository {
    private final Pool client;

    public CategoryQueryRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<PagedResult<Category>> getCategories(FindAllCategory req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client
                .preparedQuery("""
                        SELECT
                            category_id,
                            name,
                            description,
                            slug_category,
                            created_at,
                            updated_at,
                            COUNT(*) OVER () AS total_count
                        FROM categories
                        WHERE
                            deleted_at IS NULL
                            AND (
                                $1::TEXT IS NULL
                                OR name ILIKE '%' || $1 || '%'
                                OR slug_category ILIKE '%' || $1 || '%'
                            )
                        ORDER BY created_at DESC
                        LIMIT $2
                        OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedCategories);
    }

    @Override
    public Future<PagedResult<Category>> getCategoriesActive(FindAllCategory req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client
                .preparedQuery("""
                        SELECT
                            category_id,
                            name,
                            description,
                            slug_category,
                            created_at,
                            updated_at,
                            deleted_at,
                            COUNT(*) OVER () AS total_count
                        FROM categories
                        WHERE
                            deleted_at IS NULL
                            AND (
                                $1::TEXT IS NULL
                                OR name ILIKE '%' || $1 || '%'
                                OR slug_category ILIKE '%' || $1 || '%'
                            )
                        ORDER BY created_at DESC
                        LIMIT $2
                        OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedCategories);
    }

    @Override
    public Future<PagedResult<Category>> getCategoriesTrashed(FindAllCategory req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client
                .preparedQuery("""
                        SELECT
                            category_id,
                            name,
                            description,
                            slug_category,
                            created_at,
                            updated_at,
                            deleted_at,
                            COUNT(*) OVER () AS total_count
                        FROM categories
                        WHERE
                            deleted_at IS NOT NULL
                            AND (
                                $1::TEXT IS NULL
                                OR name ILIKE '%' || $1 || '%'
                                OR slug_category ILIKE '%' || $1 || '%'
                            )
                        ORDER BY created_at DESC
                        LIMIT $2
                        OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedCategories);
    }

    @Override
    public Future<Category> getCategoryById(Long categoryId) {
        return client
                .preparedQuery("""
                        SELECT
                            category_id,
                            name,
                            description,
                            slug_category,
                            created_at,
                            updated_at
                        FROM categories
                        WHERE
                            category_id = $1
                            AND deleted_at IS NULL;
                        """)
                .execute(Tuple.of(categoryId))
                .map(rows -> rows.iterator().hasNext() ? Category.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Category> getCategoryByName(String name) {
        return client
                .preparedQuery("""
                        SELECT
                            category_id,
                            name,
                            description,
                            slug_category,
                            created_at,
                            updated_at
                        FROM categories
                        WHERE
                            name = $1
                            AND deleted_at IS NULL;
                        """)
                .execute(Tuple.of(name))
                .map(rows -> rows.iterator().hasNext() ? Category.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Category> getCategoryByNameAndId(String name, Long categoryId) {
        return client
                .preparedQuery("""
                        SELECT
                            category_id,
                            name,
                            description,
                            slug_category,
                            created_at,
                            updated_at
                        FROM categories
                        WHERE
                            name = $1
                            AND category_id = $2
                            AND deleted_at IS NULL;
                        """)
                .execute(Tuple.of(name, categoryId))
                .map(rows -> rows.iterator().hasNext() ? Category.fromRow(rows.iterator().next()) : null);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank())
            return null;
        return search;
    }

    private PagedResult<Category> mapPagedCategories(RowSet<Row> rows) {
        List<Category> list = new ArrayList<>();
        int total = 0;
        for (Row row : rows) {
            list.add(Category.fromRow(row));
            if (total == 0)
                total = row.getInteger("total_count");
        }
        return new PagedResult<>(list, total);
    }
}
