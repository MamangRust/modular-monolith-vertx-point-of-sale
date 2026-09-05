package io.example.order.repository;

import io.example.order.model.Product;
import io.vertx.core.Future;

public interface ProductCommandRepository {
    /**
     * Atomically decrements stock on the product service (guaranteed never
     * negative). The future fails with a domain error when stock is
     * insufficient — the caller must NOT swallow it.
     */
    Future<Product> decrementStock(Long productId, int quantity);

    /**
     * Atomically increments stock on the product service — used to
     * compensate/roll back stock when an order fails part-way through.
     */
    Future<Product> incrementStock(Long productId, int quantity);
}
