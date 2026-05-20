package io.example.order.repository;

import io.vertx.core.Future;

public interface MerchantQueryRepository {
    Future<Boolean> existsById(Integer merchantId);
}
