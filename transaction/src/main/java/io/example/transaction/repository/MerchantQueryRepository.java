package io.example.transaction.repository;

import io.vertx.core.Future;

public interface MerchantQueryRepository {
    Future<String> findContactEmailByMerchantId(Integer merchantId);
}
