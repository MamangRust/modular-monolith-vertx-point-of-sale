package io.example.product.repository;

import io.example.common.model.PagedResult;
import io.example.product.domain.requests.FindAllProducts;
import io.example.product.domain.requests.ProductByCategoryRequest;
import io.example.product.domain.requests.ProductByMerchantRequest;
import io.example.product.model.Product;
import io.vertx.core.Future;

public interface ProductQueryRepository {
    Future<PagedResult<Product>> getProducts(FindAllProducts req);
    Future<PagedResult<Product>> getProductsActive(FindAllProducts req);
    Future<PagedResult<Product>> getProductsTrashed(FindAllProducts req);
    Future<PagedResult<Product>> getProductsByMerchant(ProductByMerchantRequest req);
    Future<PagedResult<Product>> getProductsByCategoryName(ProductByCategoryRequest req);
    Future<Product> getProductById(Long productId);
}
