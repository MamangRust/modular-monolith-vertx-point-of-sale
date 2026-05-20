package io.example.category.repository.impl;

import io.example.category.domain.requests.CreateCategoryRequest;
import io.example.category.domain.requests.UpdateCategoryRequest;
import io.example.category.model.Category;
import io.example.category.repository.CategoryCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class CategoryCommandRepositoryImpl implements CategoryCommandRepository {
    private final Pool client;

    public CategoryCommandRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<Category> createCategory(CreateCategoryRequest req) {
        return client
                .preparedQuery("""
                        INSERT INTO
                            categories (
                                name,
                                description,
                                slug_category
                            )
                        VALUES ($1, $2, $3)
                        RETURNING
                            category_id,
                            name,
                            description,
                            slug_category,
                            created_at,
                            updated_at;
                        """)
                .execute(Tuple.of(req.getName(), req.getDescription(), req.getSlugCategory()))
                .map(rows -> Category.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<Category> updateCategory(UpdateCategoryRequest req) {
        return client
                .preparedQuery("""
                        UPDATE categories
                        SET
                            name = $2,
                            description = $3,
                            slug_category = $4,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE
                            category_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            category_id,
                            name,
                            description,
                            slug_category,
                            created_at,
                            updated_at;
                        """)
                .execute(Tuple.of(req.getCategoryId(), req.getName(), req.getDescription(), req.getSlugCategory()))
                .map(rows -> rows.iterator().hasNext() ? Category.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Category> trashCategory(Long categoryId) {
        return client
                .preparedQuery("""
                        UPDATE categories
                        SET
                            deleted_at = current_timestamp
                        WHERE
                            category_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            category_id,
                            name,
                            description,
                            slug_category,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(categoryId))
                .map(rows -> rows.iterator().hasNext() ? Category.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Category> restoreCategory(Long categoryId) {
        return client
                .preparedQuery("""
                        UPDATE categories
                        SET
                            deleted_at = NULL
                        WHERE
                            category_id = $1
                            AND deleted_at IS NOT NULL
                        RETURNING
                            category_id,
                            name,
                            description,
                            slug_category,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(categoryId))
                .map(rows -> rows.iterator().hasNext() ? Category.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Boolean> deleteCategoryPermanently(Long categoryId) {
        return client
                .preparedQuery("DELETE FROM categories WHERE category_id = $1 AND deleted_at IS NOT NULL")
                .execute(Tuple.of(categoryId))
                .map(RowSet::rowCount)
                .map(count -> count > 0);
    }

    @Override
    public Future<Boolean> restoreAllCategories() {
        return client
                .preparedQuery("UPDATE categories SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount)
                .map(count -> count > 0);
    }

    @Override
    public Future<Boolean> deleteAllPermanentCategories() {
        return client
                .preparedQuery("DELETE FROM categories WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount)
                .map(count -> count > 0);
    }
}
