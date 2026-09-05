package io.example.product.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.product.domain.requests.FindAllProducts;
import io.example.product.domain.requests.ProductByCategoryRequest;
import io.example.product.domain.requests.ProductByMerchantRequest;
import io.example.product.service.ProductQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.product.Product.ApiResponseProduct;
import pb.product.Product.FindAllProductCategoryRequest;
import pb.product.Product.FindAllProductMerchantRequest;
import pb.product.Product.FindAllProductRequest;
import pb.product.Product.FindByIdProductRequest;
import pb.product.ProductQuery.ApiResponsePaginationProduct;
import pb.product.ProductQuery.ApiResponsePaginationProductDeleteAt;

@RequiredArgsConstructor
public class ProductQueryHandler implements pb.product.VertxProductServiceGrpcServer.ProductServiceApi {
        private final ProductQueryService service;

        private pb.common.PaginationMeta toMeta(int page, int pageSize, int totalRecords) {
                int currentPage = page > 0 ? page : 1;
                int size = pageSize > 0 ? pageSize : 10;
                int totalPages = size > 0 ? (int) Math.ceil((double) totalRecords / size) : 0;
                return pb.common.PaginationMeta.newBuilder()
                                .setCurrentPage(currentPage)
                                .setPageSize(size)
                                .setTotalPages(totalPages)
                                .setTotalRecords(totalRecords)
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
                                                .setStatus("success")
                                                .setMessage("Products retrieved successfully")
                                                .addAllData(resp.getData().stream()
                                                                .map(ProtoConverter::fromProductResponse)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                resp.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
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
                                                .setStatus("success")
                                                .setMessage("Products by merchant retrieved successfully")
                                                .addAllData(resp.getData().stream()
                                                                .map(ProtoConverter::fromProductResponse)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                resp.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
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
                                                .setStatus("success")
                                                .setMessage("Products by category retrieved successfully")
                                                .addAllData(resp.getData().stream()
                                                                .map(ProtoConverter::fromProductResponse)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                resp.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseProduct> findById(FindByIdProductRequest req) {
                return service.getById((long) req.getId())
                                .map(resp -> ApiResponseProduct.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Product found successfully")
                                                .setData(ProtoConverter.fromProductResponse(resp))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
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
                                                .setStatus("success")
                                                .setMessage("Active products retrieved successfully")
                                                .addAllData(resp.getData().stream()
                                                                .map(ProtoConverter::fromProductResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                resp.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
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
                                                .setStatus("success")
                                                .setMessage("Trashed products retrieved successfully")
                                                .addAllData(resp.getData().stream()
                                                                .map(ProtoConverter::fromProductResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                resp.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}