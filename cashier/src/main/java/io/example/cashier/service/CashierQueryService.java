package io.example.cashier.service;

import io.example.cashier.model.Cashier;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;

public interface CashierQueryService {
    Future<PagedResult<Cashier>> getCashiers(String search, int page, int pageSize);
    Future<Cashier> getCashierById(Long cashierId);
    Future<PagedResult<Cashier>> getCashiersActive(String search, int page, int pageSize);
    Future<PagedResult<Cashier>> getCashiersTrashed(String search, int page, int pageSize);
    Future<PagedResult<Cashier>> getCashiersByMerchant(Long merchantId, String search, int page, int pageSize);
}
