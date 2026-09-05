package io.example.order.repository;

import io.vertx.core.Future;

public interface CashierQueryRepository {
    Future<Boolean> existsById(Long cashierId);
}
