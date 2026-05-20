package io.example.order.repository.impl;

import io.example.order.model.Product;
import io.example.order.repository.ProductQueryRepository;
import io.vertx.core.Future;
import pb.product.VertxProductServiceGrpcClient;
import pb.product.Product.FindByIdProductRequest;

public class ProductQueryRepositoryImpl implements ProductQueryRepository {
    private final VertxProductServiceGrpcClient client;

    public ProductQueryRepositoryImpl(VertxProductServiceGrpcClient client) {
        this.client = client;
    }

    @Override
    public Future<Product> getProductById(Long productId) {
        if (productId == null) {
            return Future.succeededFuture(null);
        }
        FindByIdProductRequest request = FindByIdProductRequest.newBuilder()
                .setId(productId.intValue())
                .build();

        return client.findById(request)
                .map(response -> {
                    if (response != null && response.hasData()) {
                        var d = response.getData();
                        return Product.builder()
                                .productId((long) d.getId())
                                .name(d.getName())
                                .price(d.getPrice())
                                .countInStock(d.getCountInStock())
                                .build();
                    }
                    return null;
                })
                .recover(err -> Future.succeededFuture(null));
    }
}
