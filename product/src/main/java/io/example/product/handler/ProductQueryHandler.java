package io.example.product.handler;

import io.example.product.domain.requests.FindAllProducts;
import io.example.product.domain.requests.ProductByCategoryRequest;
import io.example.product.domain.requests.ProductByMerchantRequest;
import io.example.product.service.ProductQueryService;
import io.vertx.core.Future;
import pb.product.Product.*;
import pb.product.ProductQuery.*;

public class ProductQueryHandler implements pb.product.VertxProductServiceGrpcServer.ProductServiceApi {
    private final ProductQueryService service;

    public ProductQueryHandler(ProductQueryService service) {
        this.service = service;
    }

    private pb.common.PaginationMeta toMeta(io.example.common.model.PaginationMeta meta) {
        if (meta == null) return pb.common.PaginationMeta.getDefaultInstance();
        return pb.common.PaginationMeta.newBuilder()
                .setCurrentPage(meta.currentPage())
                .setPageSize(meta.pageSize())
                .setTotalPages(meta.totalPages())
                .setTotalRecords(meta.totalRecords())
                .build();
    }

    @Override
    public Future<ApiResponsePaginationProduct> findAll(FindAllProductRequest req) {
        FindAllProducts requests = FindAllProducts.builder()
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .search(req.getSearch())
                .build();

        return service.getAll(requests)
                .map(resp -> ApiResponsePaginationProduct.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromProductResponse).toList())
                        .setPagination(toMeta(resp.pagination()))
                        .build());
    }

    @Override
    public Future<ApiResponsePaginationProduct> findByMerchant(FindAllProductMerchantRequest req) {
        ProductByMerchantRequest requests = ProductByMerchantRequest.builder()
                .merchantId(req.getMerchantId())
                .search(req.getSearch())
                .categoryId(req.getCategoryId())
                .minPrice(req.getMinPrice())
                .maxPrice(req.getMaxPrice())
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .build();

        return service.getByMerchant(requests)
                .map(resp -> ApiResponsePaginationProduct.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromProductResponse).toList())
                        .setPagination(toMeta(resp.pagination()))
                        .build());
    }

    @Override
    public Future<ApiResponsePaginationProduct> findByCategory(FindAllProductCategoryRequest req) {
        ProductByCategoryRequest requests = ProductByCategoryRequest.builder()
                .categoryName(req.getCategoryName())
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .search(req.getSearch())
                .minPrice(req.getMinprice())
                .maxPrice(req.getMaxprice())
                .build();

        return service.getByCategoryName(requests)
                .map(resp -> ApiResponsePaginationProduct.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromProductResponse).toList())
                        .setPagination(toMeta(resp.pagination()))
                        .build());
    }

    @Override
    public Future<ApiResponseProduct> findById(FindByIdProductRequest req) {
        return service.getById((long) req.getId())
                .map(resp -> {
                    var builder = ApiResponseProduct.newBuilder()
                            .setStatus(resp.status())
                            .setMessage(resp.message());
                    if (resp.data() != null) {
                        builder.setData(ProtoConverter.fromProductResponse(resp.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<ApiResponsePaginationProductDeleteAt> findByActive(FindAllProductRequest req) {
        FindAllProducts requests = FindAllProducts.builder()
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .search(req.getSearch())
                .build();

        return service.getActive(requests)
                .map(resp -> ApiResponsePaginationProductDeleteAt.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromProductResponseToDeleteAt).toList())
                        .setPagination(toMeta(resp.pagination()))
                        .build());
    }

    @Override
    public Future<ApiResponsePaginationProductDeleteAt> findByTrashed(FindAllProductRequest req) {
        FindAllProducts requests = FindAllProducts.builder()
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .search(req.getSearch())
                .build();

        return service.getTrashed(requests)
                .map(resp -> ApiResponsePaginationProductDeleteAt.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromProductResponseDeleteAt).toList())
                        .setPagination(toMeta(resp.pagination()))
                        .build());
    }
}
