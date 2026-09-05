package io.example.cashier.repository;

import io.example.cashier.domain.requests.cashier.FindAllCashierMerchant;
import io.example.cashier.domain.requests.cashier.FindAllCashiers;
import io.example.cashier.model.Cashier;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;

public interface CashierQueryRepository {
    Future<PagedResult<Cashier>> findAllCashiers(FindAllCashiers req);

    Future<Cashier> findById(Long cashierId);

    Future<Cashier> findByName(String name);

    Future<Cashier> findByTrashedId(Long cashierId);

    Future<PagedResult<Cashier>> findByActive(FindAllCashiers req);

    Future<PagedResult<Cashier>> findByTrashed(FindAllCashiers req);

    Future<PagedResult<Cashier>> findByMerchant(FindAllCashierMerchant req);
}
