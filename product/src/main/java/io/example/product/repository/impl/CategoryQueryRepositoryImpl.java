package io.example.product.repository.impl;

import io.example.product.repository.CategoryQueryRepository;
import io.vertx.core.Future;
import pb.category.VertxCategoryServiceGrpcClient;
import pb.category.Category.FindByIdCategoryRequest;

public class CategoryQueryRepositoryImpl implements CategoryQueryRepository {
    private final VertxCategoryServiceGrpcClient client;

    public CategoryQueryRepositoryImpl(VertxCategoryServiceGrpcClient client) {
        this.client = client;
    }

    @Override
    public Future<Boolean> existsById(Integer categoryId) {
        if (categoryId == null || categoryId <= 0) {
            return Future.succeededFuture(false);
        }
        FindByIdCategoryRequest request = FindByIdCategoryRequest.newBuilder()
                .setId(categoryId)
                .build();
        return client.findById(request)
                .map(response -> response != null && response.hasData() && response.getData().getId() > 0)
                .recover(err -> Future.succeededFuture(false));
    }
}
