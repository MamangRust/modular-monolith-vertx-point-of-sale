package io.example.cashier.repository;

import io.vertx.core.Future;

public interface UserQueryRepository {
    Future<Boolean> existsById(Integer userId);
}
