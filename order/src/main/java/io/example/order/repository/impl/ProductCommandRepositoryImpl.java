package io.example.order.repository.impl;

import io.example.order.model.Product;
import io.example.order.repository.ProductCommandRepository;
import io.vertx.core.Future;
import pb.product.VertxProductServiceGrpcClient;
import pb.product.VertxProductCommandServiceGrpcClient;
import pb.product.Product.FindByIdProductRequest;
import pb.product.ProductCommand.UpdateProductRequest;

public class ProductCommandRepositoryImpl implements ProductCommandRepository {
    private final VertxProductServiceGrpcClient queryClient;
    private final VertxProductCommandServiceGrpcClient commandClient;

    public ProductCommandRepositoryImpl(VertxProductServiceGrpcClient queryClient, VertxProductCommandServiceGrpcClient commandClient) {
        this.queryClient = queryClient;
        this.commandClient = commandClient;
    }

    @Override
    public Future<Product> updateProductCountStock(Long productId, int stock) {
        if (productId == null) {
            return Future.succeededFuture(null);
        }

        FindByIdProductRequest findReq = FindByIdProductRequest.newBuilder()
                .setId(productId.intValue())
                .build();

        return queryClient.findById(findReq)
                .compose(response -> {
                    if (response == null || !response.hasData()) {
                        return Future.succeededFuture(null);
                    }
                    var p = response.getData();
                    UpdateProductRequest updateReq = UpdateProductRequest.newBuilder()
                            .setProductId(p.getId())
                            .setMerchantId(p.getMerchantId())
                            .setCategoryId(p.getCategoryId())
                            .setName(p.getName())
                            .setDescription(p.getDescription())
                            .setPrice(p.getPrice())
                            .setCountInStock(stock)
                            .setBrand(p.getBrand())
                            .setWeight(p.getWeight())
                            .setImageProduct(p.getImageProduct())
                            .build();

                    return commandClient.update(updateReq)
                            .map(upRes -> {
                                if (upRes != null && upRes.hasData()) {
                                    var d = upRes.getData();
                                    return Product.builder()
                                            .productId((long) d.getId())
                                            .name(d.getName())
                                            .price(d.getPrice())
                                            .countInStock(d.getCountInStock())
                                            .build();
                                }
                                return null;
                            });
                })
                .recover(err -> Future.succeededFuture(null));
    }
}
