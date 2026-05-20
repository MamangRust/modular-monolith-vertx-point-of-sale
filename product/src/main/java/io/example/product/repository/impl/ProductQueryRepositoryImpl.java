package io.example.product.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.common.model.PagedResult;
import io.example.product.domain.requests.FindAllProducts;
import io.example.product.domain.requests.ProductByCategoryRequest;
import io.example.product.domain.requests.ProductByMerchantRequest;
import io.example.product.model.Product;
import io.example.product.repository.ProductQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class ProductQueryRepositoryImpl implements ProductQueryRepository {
    private final Pool client;

    public ProductQueryRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<PagedResult<Product>> getProducts(FindAllProducts req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client
                .preparedQuery("""
                        SELECT
                            p.product_id,
                            p.merchant_id,
                            p.category_id,
                            p.name,
                            p.description,
                            p.price,
                            p.count_in_stock,
                            p.brand,
                            p.weight,
                            p.slug_product,
                            p.image_product,
                            p.barcode,
                            p.created_at,
                            p.updated_at,
                            COUNT(*) OVER () AS total_count
                        FROM products as p
                        WHERE
                            deleted_at IS NULL
                            AND (
                                $1::TEXT IS NULL
                                OR p.name ILIKE '%' || $1 || '%'
                                OR p.description ILIKE '%' || $1 || '%'
                                OR p.brand ILIKE '%' || $1 || '%'
                                OR p.slug_product ILIKE '%' || $1 || '%'
                                OR p.barcode ILIKE '%' || $1 || '%'
                            )
                        ORDER BY created_at DESC
                        LIMIT $2
                        OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedProducts);
    }

    @Override
    public Future<PagedResult<Product>> getProductsActive(FindAllProducts req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client
                .preparedQuery("""
                        SELECT
                            p.product_id,
                            p.merchant_id,
                            p.category_id,
                            p.name,
                            p.description,
                            p.price,
                            p.count_in_stock,
                            p.brand,
                            p.weight,
                            p.slug_product,
                            p.image_product,
                            p.barcode,
                            p.created_at,
                            p.updated_at,
                            p.deleted_at,
                            COUNT(*) OVER () AS total_count
                        FROM products as p
                        WHERE
                            deleted_at IS NULL
                            AND (
                                $1::TEXT IS NULL
                                OR p.name ILIKE '%' || $1 || '%'
                                OR p.description ILIKE '%' || $1 || '%'
                                OR p.brand ILIKE '%' || $1 || '%'
                                OR p.slug_product ILIKE '%' || $1 || '%'
                                OR p.barcode ILIKE '%' || $1 || '%'
                            )
                        ORDER BY created_at DESC
                        LIMIT $2
                        OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedProducts);
    }

    @Override
    public Future<PagedResult<Product>> getProductsTrashed(FindAllProducts req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client
                .preparedQuery("""
                        SELECT
                            p.product_id,
                            p.merchant_id,
                            p.category_id,
                            p.name,
                            p.description,
                            p.price,
                            p.count_in_stock,
                            p.brand,
                            p.weight,
                            p.slug_product,
                            p.image_product,
                            p.barcode,
                            p.created_at,
                            p.updated_at,
                            p.deleted_at,
                            COUNT(*) OVER () AS total_count
                        FROM products as p
                        WHERE
                            deleted_at IS NOT NULL
                            AND (
                                $1::TEXT IS NULL
                                OR p.name ILIKE '%' || $1 || '%'
                                OR p.description ILIKE '%' || $1 || '%'
                                OR p.brand ILIKE '%' || $1 || '%'
                                OR p.slug_product ILIKE '%' || $1 || '%'
                                OR p.barcode ILIKE '%' || $1 || '%'
                            )
                        ORDER BY created_at DESC
                        LIMIT $2
                        OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedProducts);
    }

    @Override
    public Future<PagedResult<Product>> getProductsByMerchant(ProductByMerchantRequest req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();

        Integer catIdVal = req.getCategoryId() != null && req.getCategoryId() > 0 ? req.getCategoryId() : null;
        Integer minPriceVal = req.getMinPrice() != null && req.getMinPrice() > 0 ? req.getMinPrice() : null;
        Integer maxPriceVal = req.getMaxPrice() != null && req.getMaxPrice() > 0 ? req.getMaxPrice() : null;

        return client
                .preparedQuery("""
                        WITH filtered_products AS (
                            SELECT
                                p.product_id,
                                p.merchant_id,
                                p.category_id,
                                p.name,
                                p.description,
                                p.price,
                                p.count_in_stock,
                                p.brand,
                                p.weight,
                                p.slug_product,
                                p.image_product,
                                p.barcode,
                                p.created_at,
                                p.updated_at,
                                c.name AS category_name
                            FROM products p
                                JOIN categories c ON p.category_id = c.category_id
                            WHERE
                                p.deleted_at IS NULL
                                AND p.merchant_id = $1
                                AND (
                                    p.name ILIKE '%' || COALESCE($2, '') || '%'
                                    OR p.description ILIKE '%' || COALESCE($2, '') || '%'
                                    OR $2 IS NULL
                                )
                                AND (
                                    c.category_id = NULLIF($3, 0)
                                    OR NULLIF($3, 0) IS NULL
                                )
                                AND (
                                    p.price >= COALESCE(NULLIF($4, 0), 0)
                                    AND p.price <= COALESCE(NULLIF($5, 0), 999999999)
                                )
                        )
                        SELECT (SELECT COUNT(*) FROM filtered_products) AS total_count, fp.*
                        FROM filtered_products fp
                        ORDER BY fp.created_at DESC
                        LIMIT $6
                        OFFSET $7;
                        """)
                .execute(Tuple.of(req.getMerchantId().longValue(), req.getSearch(), catIdVal, minPriceVal, maxPriceVal, req.getPageSize(), offset))
                .map(this::mapPagedProducts);
    }

    @Override
    public Future<PagedResult<Product>> getProductsByCategoryName(ProductByCategoryRequest req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client
                .preparedQuery("""
                        WITH filtered_products AS (
                            SELECT
                                p.product_id,
                                p.merchant_id,
                                p.category_id,
                                p.slug_product,
                                p.weight,
                                p.name,
                                p.description,
                                p.price,
                                p.count_in_stock,
                                p.brand,
                                p.image_product,
                                p.barcode,
                                p.created_at,
                                p.updated_at,
                                p.deleted_at,
                                c.name AS category_name
                            FROM products p
                                JOIN categories c ON p.category_id = c.category_id
                            WHERE
                                p.deleted_at IS NULL
                                AND c.name = $1
                                AND (
                                    $2 IS NULL
                                    OR p.name ILIKE '%' || $2 || '%'
                                    OR p.description ILIKE '%' || $2 || '%'
                                )
                                AND (
                                    ($3 IS NULL OR p.price >= $3)
                                    AND ($4 IS NULL OR p.price <= $4)
                                )
                        )
                        SELECT (SELECT COUNT(*) FROM filtered_products) AS total_count, fp.*
                        FROM filtered_products fp
                        ORDER BY fp.created_at DESC
                        LIMIT $5
                        OFFSET $6;
                        """)
                .execute(Tuple.of(req.getCategoryName(), req.getSearch(), req.getMinPrice(), req.getMaxPrice(), req.getPageSize(), offset))
                .map(this::mapPagedProducts);
    }

    @Override
    public Future<Product> getProductById(Long productId) {
        return client
                .preparedQuery("""
                        SELECT
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
                            updated_at
                        FROM products
                        WHERE
                            product_id = $1
                            AND deleted_at IS NULL;
                        """)
                .execute(Tuple.of(productId))
                .map(rows -> rows.iterator().hasNext() ? Product.fromRow(rows.iterator().next()) : null);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank())
            return null;
        return search;
    }

    private PagedResult<Product> mapPagedProducts(RowSet<Row> rows) {
        List<Product> list = new ArrayList<>();
        int total = 0;
        for (Row row : rows) {
            list.add(Product.fromRow(row));
            if (total == 0) {
                Integer tc = row.getInteger("total_count");
                total = tc != null ? tc : 0;
            }
        }
        return new PagedResult<>(list, total);
    }
}
