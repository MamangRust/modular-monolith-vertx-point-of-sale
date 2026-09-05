package io.example.product.service;

import io.example.common.domain.PagedResult;
import io.example.product.domain.requests.FindAllProducts;
import io.example.product.domain.requests.ProductByCategoryRequest;
import io.example.product.domain.requests.ProductByMerchantRequest;
import io.example.product.domain.response.ProductResponse;
import io.example.product.domain.response.ProductResponseDeleteAt;
import io.vertx.core.Future;

public interface ProductQueryService {
    Future<PagedResult<ProductResponse>> getAll(FindAllProducts req);

    Future<PagedResult<ProductResponseDeleteAt>> getActive(FindAllProducts req);

    Future<PagedResult<ProductResponseDeleteAt>> getTrashed(FindAllProducts req);

    Future<PagedResult<ProductResponse>> getByMerchant(ProductByMerchantRequest req);

    Future<PagedResult<ProductResponse>> getByCategoryName(ProductByCategoryRequest req);

    Future<ProductResponse> getById(Long id);
}
