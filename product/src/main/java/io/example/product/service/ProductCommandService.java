package io.example.product.service;

import io.example.common.model.ApiResponse;
import io.example.product.domain.requests.CreateProductRequest;
import io.example.product.domain.requests.UpdateProductRequest;
import io.example.product.model.ProductResponse;
import io.example.product.model.ProductResponseDeleteAt;
import io.vertx.core.Future;

public interface ProductCommandService {
    Future<ApiResponse<ProductResponse>> create(CreateProductRequest req);
    Future<ApiResponse<ProductResponse>> update(UpdateProductRequest req);
    Future<ApiResponse<ProductResponseDeleteAt>> trash(Long id);
    Future<ApiResponse<ProductResponseDeleteAt>> restore(Long id);
    Future<ApiResponse<Boolean>> deletePermanent(Long id);
    Future<ApiResponse<Boolean>> restoreAll();
    Future<ApiResponse<Boolean>> deleteAllPermanent();
}
