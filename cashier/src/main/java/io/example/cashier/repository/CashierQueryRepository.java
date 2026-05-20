package io.example.cashier.repository;

import io.example.cashier.model.Cashier;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;

public interface CashierQueryRepository {
    Future<PagedResult<Cashier>> findAllCashiers(String search, int page, int pageSize);
    Future<Cashier> findById(Long cashierId);
    Future<PagedResult<Cashier>> findByActive(String search, int page, int pageSize);
    Future<PagedResult<Cashier>> findByTrashed(String search, int page, int pageSize);
    Future<PagedResult<Cashier>> findByMerchant(Long merchantId, String search, int page, int pageSize);
}
