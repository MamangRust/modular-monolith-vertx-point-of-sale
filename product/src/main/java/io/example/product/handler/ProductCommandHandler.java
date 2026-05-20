package io.example.product.handler;

import com.google.protobuf.Empty;
import io.example.product.service.ProductCommandService;
import io.vertx.core.Future;
import pb.product.Product.*;
import pb.product.ProductCommand.*;

public class ProductCommandHandler implements pb.product.VertxProductCommandServiceGrpcServer.ProductCommandServiceApi {
    private final ProductCommandService service;

    public ProductCommandHandler(ProductCommandService service) {
        this.service = service;
    }

    @Override
    public Future<ApiResponseProduct> create(CreateProductRequest req) {
        io.example.product.domain.requests.CreateProductRequest requests = io.example.product.domain.requests.CreateProductRequest.builder()
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
    public Future<ApiResponseProduct> update(UpdateProductRequest req) {
        io.example.product.domain.requests.UpdateProductRequest requests = io.example.product.domain.requests.UpdateProductRequest.builder()
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
    public Future<ApiResponseProductDeleteAt> trashedProduct(FindByIdProductRequest req) {
        return service.trash((long) req.getId())
                .map(resp -> {
                    var builder = ApiResponseProductDeleteAt.newBuilder()
                            .setStatus(resp.status())
                            .setMessage(resp.message());
                    if (resp.data() != null) {
                        builder.setData(ProtoConverter.fromProductResponseDeleteAt(resp.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<ApiResponseProductDeleteAt> restoreProduct(FindByIdProductRequest req) {
        return service.restore((long) req.getId())
                .map(resp -> {
                    var builder = ApiResponseProductDeleteAt.newBuilder()
                            .setStatus(resp.status())
                            .setMessage(resp.message());
                    if (resp.data() != null) {
                        builder.setData(ProtoConverter.fromProductResponseDeleteAt(resp.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<ApiResponseProductDelete> deleteProductPermanent(FindByIdProductRequest req) {
        return service.deletePermanent((long) req.getId())
                .map(resp -> ApiResponseProductDelete.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .build());
    }

    @Override
    public Future<ApiResponseProductAll> restoreAllProduct(Empty req) {
        return service.restoreAll()
                .map(resp -> ApiResponseProductAll.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .build());
    }

    @Override
    public Future<ApiResponseProductAll> deleteAllProductPermanent(Empty req) {
        return service.deleteAllPermanent()
                .map(resp -> ApiResponseProductAll.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .build());
    }
}
