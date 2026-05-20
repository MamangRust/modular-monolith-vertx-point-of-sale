package io.example.cashier.repository.impl;

import io.example.cashier.repository.UserQueryRepository;
import io.vertx.core.Future;
import pb.user.User.FindByIdUserRequest;
import pb.user.VertxUserQueryServiceGrpcClient;

public class UserQueryRepositoryImpl implements UserQueryRepository {
    private final VertxUserQueryServiceGrpcClient userQueryClient;

    public UserQueryRepositoryImpl(VertxUserQueryServiceGrpcClient userQueryClient) {
        this.userQueryClient = userQueryClient;
    }

    @Override
    public Future<Boolean> existsById(Integer userId) {
        if (userId == null) {
            return Future.succeededFuture(false);
        }
        FindByIdUserRequest request = FindByIdUserRequest.newBuilder()
                .setId(userId)
                .build();
        return userQueryClient.findById(request)
                .map(response -> response != null && response.hasData() && response.getData().getId() > 0)
                .recover(err -> Future.succeededFuture(false));
    }
}
