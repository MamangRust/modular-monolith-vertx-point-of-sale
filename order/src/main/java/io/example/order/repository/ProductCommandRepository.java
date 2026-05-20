package io.example.order.repository;

import io.example.order.model.Product;
import io.vertx.core.Future;

public interface ProductCommandRepository {
    Future<Product> updateProductCountStock(Long productId, int stock);
}
