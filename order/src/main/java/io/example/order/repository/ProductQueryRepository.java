package io.example.order.repository;

import io.example.order.model.Product;
import io.vertx.core.Future;

public interface ProductQueryRepository {
    Future<Product> getProductById(Long productId);
}
