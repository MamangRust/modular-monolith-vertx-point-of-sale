package io.example.product.service;

import java.util.List;

import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.product.domain.requests.FindAllProducts;
import io.example.product.domain.requests.ProductByCategoryRequest;
import io.example.product.domain.requests.ProductByMerchantRequest;
import io.example.product.model.ProductResponse;
import io.example.product.model.ProductResponseDeleteAt;
import io.vertx.core.Future;

public interface ProductQueryService {
    Future<ApiResponsePagination<List<ProductResponse>>> getAll(FindAllProducts req);
    Future<ApiResponsePagination<List<ProductResponse>>> getActive(FindAllProducts req);
    Future<ApiResponsePagination<List<ProductResponseDeleteAt>>> getTrashed(FindAllProducts req);
    Future<ApiResponsePagination<List<ProductResponse>>> getByMerchant(ProductByMerchantRequest req);
    Future<ApiResponsePagination<List<ProductResponse>>> getByCategoryName(ProductByCategoryRequest req);
    Future<ApiResponse<ProductResponse>> getById(Long id);
}
