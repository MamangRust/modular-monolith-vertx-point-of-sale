package io.example.product.repository;

import io.example.product.domain.requests.CreateProductRequest;
import io.example.product.domain.requests.UpdateProductRequest;
import io.example.product.model.Product;
import io.vertx.core.Future;

public interface ProductCommandRepository {
    Future<Product> createProduct(CreateProductRequest req);

    Future<Product> updateProduct(UpdateProductRequest req);

    Future<Product> trashProduct(Long productId);

    Future<Product> restoreProduct(Long productId);

    /**
     * Atomically decrements count_in_stock. Succeeds only when the product
     * exists (not deleted) and has enough stock; otherwise returns null so the
     * caller can fail with a domain error. Guarantees stock never goes negative
     * even under concurrent orders.
     */
    Future<Product> decrementStock(Long productId, int quantity);

    /**
     * Atomically increments count_in_stock (compensation for a failed order).
     * Returns null when the product does not exist or is soft-deleted.
     */
    Future<Product> incrementStock(Long productId, int quantity);

    Future<Boolean> deleteProductPermanently(Long productId);

    Future<Integer> restoreAllProducts();

    Future<Integer> deleteAllPermanentProducts();
}
