package io.example.product.handler;

import com.google.protobuf.Empty;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.product.service.ProductCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.product.Product.ApiResponseProduct;
import pb.product.Product.ApiResponseProductDeleteAt;
import pb.product.Product.FindByIdProductRequest;
import pb.product.ProductCommand.ApiResponseProductAll;
import pb.product.ProductCommand.ApiResponseProductDelete;
import pb.product.ProductCommand.CreateProductRequest;
import pb.product.ProductCommand.DecrementStockRequest;
import pb.product.ProductCommand.IncrementStockRequest;
import pb.product.ProductCommand.UpdateProductRequest;

@RequiredArgsConstructor
public class ProductCommandHandler implements pb.product.VertxProductCommandServiceGrpcServer.ProductCommandServiceApi {
    private final ProductCommandService service;

    @Override
    public Future<ApiResponseProduct> create(CreateProductRequest req) {
        io.example.product.domain.requests.CreateProductRequest requests = io.example.product.domain.requests.CreateProductRequest
                .builder()
                .merchantId(req.getMerchantId())
                .categoryId(req.getCategoryId())
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .countInStock(req.getCountInStock())
                .brand(req.getBrand())
                .weight(req.getWeight())
                .imageProduct(req.getImageProduct())
                .build();

        return service.create(requests)
                .map(resp -> ApiResponseProduct.newBuilder()
                        .setStatus("success")
                        .setMessage("Product created successfully")
                        .setData(ProtoConverter.fromProductResponse(resp))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseProduct> update(UpdateProductRequest req) {
        io.example.product.domain.requests.UpdateProductRequest requests = io.example.product.domain.requests.UpdateProductRequest
                .builder()
                .productId(req.getProductId())
                .merchantId(req.getMerchantId())
                .categoryId(req.getCategoryId())
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .countInStock(req.getCountInStock())
                .brand(req.getBrand())
                .weight(req.getWeight())
                .imageProduct(req.getImageProduct())
                .build();

        return service.update(requests)
                .map(resp -> ApiResponseProduct.newBuilder()
                        .setStatus("success")
                        .setMessage("Product updated successfully")
                        .setData(ProtoConverter.fromProductResponse(resp))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseProduct> decrementStock(DecrementStockRequest req) {
        return service.decrementStock((long) req.getProductId(), req.getQuantity())
                .map(resp -> ApiResponseProduct.newBuilder()
                        .setStatus("success")
                        .setMessage("Stock decremented successfully")
                        .setData(ProtoConverter.fromProductResponse(resp))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseProduct> incrementStock(IncrementStockRequest req) {
        return service.incrementStock((long) req.getProductId(), req.getQuantity())
                .map(resp -> ApiResponseProduct.newBuilder()
                        .setStatus("success")
                        .setMessage("Stock incremented successfully")
                        .setData(ProtoConverter.fromProductResponse(resp))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseProductDeleteAt> trashedProduct(FindByIdProductRequest req) {
        return service.trash((long) req.getId())
                .map(resp -> ApiResponseProductDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Product trashed successfully")
                        .setData(ProtoConverter.fromProductResponseDeleteAt(resp))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseProductDeleteAt> restoreProduct(FindByIdProductRequest req) {
        return service.restore((long) req.getId())
                .map(resp -> ApiResponseProductDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Product restored successfully")
                        .setData(ProtoConverter.fromProductResponseDeleteAt(resp))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseProductDelete> deleteProductPermanent(FindByIdProductRequest req) {
        return service.deletePermanent((long) req.getId())
                .map(resp -> ApiResponseProductDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Product permanently deleted successfully")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseProductAll> restoreAllProduct(Empty req) {
        return service.restoreAll()
                .map(resp -> ApiResponseProductAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All products restored successfully")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseProductAll> deleteAllProductPermanent(Empty req) {
        return service.deleteAllPermanent()
                .map(resp -> ApiResponseProductAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All products permanently deleted successfully")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }
}