package io.example.product.repository.impl;

import io.example.product.domain.requests.CreateProductRequest;
import io.example.product.domain.requests.UpdateProductRequest;
import io.example.product.model.Product;
import io.example.product.repository.ProductCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProductCommandRepositoryImpl implements ProductCommandRepository {
    private final Pool client;

    @Override
    public Future<Product> createProduct(CreateProductRequest req) {
        return client
                .preparedQuery("""
                        INSERT INTO
                            products (
                                merchant_id,
                                category_id,
                                name,
                                description,
                                price,
                                count_in_stock,
                                brand,
                                weight,
                                slug_product,
                                image_product
                            )
                        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
                        RETURNING
                            product_id,
                            merchant_id,
                            category_id,
                            name,
                            description,
                            price,
                            count_in_stock,
                            brand,
                            weight,
                            slug_product,
                            image_product,
                            barcode,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(
                        req.getMerchantId() != null ? req.getMerchantId().longValue() : null,
                        req.getCategoryId() != null ? req.getCategoryId().longValue() : null,
                        req.getName(),
                        req.getDescription(),
                        req.getPrice(),
                        req.getCountInStock(),
                        req.getBrand(),
                        req.getWeight(),
                        req.getSlugProduct(),
                        req.getImageProduct()))
                .map(rows -> Product.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<Product> updateProduct(UpdateProductRequest req) {
        return client
                .preparedQuery("""
                        UPDATE products
                        SET
                            category_id    = COALESCE(NULLIF($2::INT, 0), category_id),
                            name           = COALESCE(NULLIF($3, ''), name),
                            description    = COALESCE(NULLIF($4, ''), description),
                            price          = COALESCE(NULLIF($5::INT, 0), price),
                            count_in_stock = COALESCE(NULLIF($6::INT, 0), count_in_stock),
                            brand          = COALESCE(NULLIF($7, ''), brand),
                            weight         = COALESCE(NULLIF($8::INT, 0), weight),
                            image_product  = COALESCE(NULLIF($9, ''), image_product),
                            updated_at     = CURRENT_TIMESTAMP
                        WHERE
                            product_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            product_id,
                            merchant_id,
                            category_id,
                            name,
                            description,
                            price,
                            count_in_stock,
                            brand,
                            weight,
                            slug_product,
                            image_product,
                            barcode,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(
                        req.getProductId() != null ? req.getProductId().longValue() : 0,
                        req.getCategoryId() != null ? req.getCategoryId().longValue() : 0,
                        req.getName() != null ? req.getName() : "",
                        req.getDescription() != null ? req.getDescription() : "",
                        req.getPrice() != null ? req.getPrice() : 0,
                        req.getCountInStock() != null ? req.getCountInStock() : 0,
                        req.getBrand() != null ? req.getBrand() : "",
                        req.getWeight() != null ? req.getWeight() : 0,
                        req.getImageProduct() != null ? req.getImageProduct() : ""))
                .map(rows -> rows.iterator().hasNext() ? Product.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Product> trashProduct(Long productId) {
        return client
                .preparedQuery("""
                        UPDATE products
                        SET
                            deleted_at = current_timestamp
                        WHERE
                            product_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            product_id,
                            merchant_id,
                            category_id,
                            name,
                            description,
                            price,
                            count_in_stock,
                            brand,
                            weight,
                            slug_product,
                            image_product,
                            barcode,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(productId))
                .map(rows -> rows.iterator().hasNext() ? Product.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Product> restoreProduct(Long productId) {
        return client
                .preparedQuery("""
                        UPDATE products
                        SET
                            deleted_at = NULL
                        WHERE
                            product_id = $1
                            AND deleted_at IS NOT NULL
                        RETURNING
                            product_id,
                            merchant_id,
                            category_id,
                            name,
                            description,
                            price,
                            count_in_stock,
                            brand,
                            weight,
                            slug_product,
                            image_product,
                            barcode,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(productId))
                .map(rows -> rows.iterator().hasNext() ? Product.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Product> decrementStock(Long productId, int quantity) {
        return client
                .preparedQuery("""
                        UPDATE products
                        SET
                            count_in_stock = count_in_stock - $2,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE
                            product_id = $1
                            AND deleted_at IS NULL
                            AND count_in_stock >= $2
                        RETURNING
                            product_id,
                            merchant_id,
                            category_id,
                            name,
                            description,
                            price,
                            count_in_stock,
                            brand,
                            weight,
                            slug_product,
                            image_product,
                            barcode,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(productId, quantity))
                // Atomic guard: no row is returned when the product is missing,
                // soft-deleted, or has insufficient stock — the caller maps this
                // to a BadRequestException so the order is rejected.
                .map(rows -> rows.iterator().hasNext() ? Product.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Product> incrementStock(Long productId, int quantity) {
        return client
                .preparedQuery("""
                        UPDATE products
                        SET
                            count_in_stock = count_in_stock + $2,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE
                            product_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            product_id,
                            merchant_id,
                            category_id,
                            name,
                            description,
                            price,
                            count_in_stock,
                            brand,
                            weight,
                            slug_product,
                            image_product,
                            barcode,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(productId, quantity))
                // No guard needed: adding stock can never go negative. A null
                // result means the product is missing/soft-deleted.
                .map(rows -> rows.iterator().hasNext() ? Product.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Boolean> deleteProductPermanently(Long productId) {
        return client
                .preparedQuery("DELETE FROM products WHERE product_id = $1 AND deleted_at IS NOT NULL")
                .execute(Tuple.of(productId))
                .map(rows -> rows.rowCount() > 0);
    }

    @Override
    public Future<Integer> restoreAllProducts() {
        return client
                .preparedQuery("UPDATE products SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount);
    }

    @Override
    public Future<Integer> deleteAllPermanentProducts() {
        return client
                .preparedQuery("DELETE FROM products WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount);
    }
}
