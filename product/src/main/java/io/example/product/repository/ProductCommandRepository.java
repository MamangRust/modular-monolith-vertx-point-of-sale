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
    Future<Void> deleteProductPermanently(Long productId);
    Future<Integer> restoreAllProducts();
    Future<Integer> deleteAllPermanentProducts();
}
