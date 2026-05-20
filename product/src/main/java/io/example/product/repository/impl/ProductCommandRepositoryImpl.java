package io.example.product.repository.impl;

import io.example.product.domain.requests.CreateProductRequest;
import io.example.product.domain.requests.UpdateProductRequest;
import io.example.product.model.Product;
import io.example.product.repository.ProductCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class ProductCommandRepositoryImpl implements ProductCommandRepository {
    private final Pool client;

    public ProductCommandRepositoryImpl(Pool client) {
        this.client = client;
    }

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
                            created_at,
                            updated_at;
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
                        req.getImageProduct()
                ))
                .map(rows -> Product.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<Product> updateProduct(UpdateProductRequest req) {
        return client
                .preparedQuery("""
                        UPDATE products
                        SET
                            category_id = $2,
                            name = $3,
                            description = $4,
                            price = $5,
                            count_in_stock = $6,
                            brand = $7,
                            weight = $8,
                            image_product = $9,
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
                            created_at,
                            updated_at;
                        """)
                .execute(Tuple.of(
                        req.getProductId() != null ? req.getProductId().longValue() : null,
                        req.getCategoryId() != null ? req.getCategoryId().longValue() : null,
                        req.getName(),
                        req.getDescription(),
                        req.getPrice(),
                        req.getCountInStock(),
                        req.getBrand(),
                        req.getWeight(),
                        req.getImageProduct()
                ))
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
    public Future<Void> deleteProductPermanently(Long productId) {
        return client
                .preparedQuery("DELETE FROM products WHERE product_id = $1 AND deleted_at IS NOT NULL")
                .execute(Tuple.of(productId))
                .mapEmpty();
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
