package io.example.order.repository.impl;

import io.example.order.model.Product;
import io.example.order.repository.ProductCommandRepository;
import io.vertx.core.Future;
import pb.product.VertxProductCommandServiceGrpcClient;
import pb.product.ProductCommand.DecrementStockRequest;
import pb.product.ProductCommand.IncrementStockRequest;

public class ProductCommandRepositoryImpl implements ProductCommandRepository {
    private final VertxProductCommandServiceGrpcClient commandClient;

    public ProductCommandRepositoryImpl(VertxProductCommandServiceGrpcClient commandClient) {
        this.commandClient = commandClient;
    }

    @Override
    public Future<Product> decrementStock(Long productId, int quantity) {
        if (productId == null) {
            return Future.succeededFuture(null);
        }

        DecrementStockRequest req = DecrementStockRequest.newBuilder()
                .setProductId(productId.intValue())
                .setQuantity(quantity)
                .build();

        // Do NOT recover to success here: an insufficient-stock failure must
        // propagate so the order is rejected.
        return commandClient.decrementStock(req)
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
    }

    @Override
    public Future<Product> incrementStock(Long productId, int quantity) {
        if (productId == null) {
            return Future.succeededFuture(null);
        }

        IncrementStockRequest req = IncrementStockRequest.newBuilder()
                .setProductId(productId.intValue())
                .setQuantity(quantity)
                .build();

        return commandClient.incrementStock(req)
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
    }
}
