package io.example.product.service;

import io.example.product.domain.requests.CreateProductRequest;
import io.example.product.domain.requests.UpdateProductRequest;
import io.example.product.domain.response.ProductResponse;
import io.example.product.domain.response.ProductResponseDeleteAt;
import io.vertx.core.Future;

public interface ProductCommandService {
    Future<ProductResponse> create(CreateProductRequest req);

    Future<ProductResponse> update(UpdateProductRequest req);

    Future<ProductResponseDeleteAt> trash(Long id);

    Future<ProductResponseDeleteAt> restore(Long id);

    /**
     * Atomically decrements stock. Fails with a domain error when the product
     * is missing or stock is insufficient, guaranteeing stock never goes
     * negative.
     */
    Future<ProductResponse> decrementStock(Long productId, int quantity);

    /**
     * Atomically increments stock (compensation for a failed order). Fails
     * with a domain error when the product is missing or soft-deleted.
     */
    Future<ProductResponse> incrementStock(Long productId, int quantity);

    Future<Boolean> deletePermanent(Long id);

    Future<Boolean> restoreAll();

    Future<Boolean> deleteAllPermanent();
}
