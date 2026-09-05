package io.example.cashier.service;

import io.example.cashier.domain.requests.cashier.FindAllCashierMerchant;
import io.example.cashier.domain.requests.cashier.FindAllCashiers;
import io.example.cashier.domain.response.cashier.CashierResponse;
import io.example.cashier.domain.response.cashier.CashierResponseDeleteAt;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;

public interface CashierQueryService {
    Future<PagedResult<CashierResponse>> getCashiers(FindAllCashiers req);

    Future<CashierResponse> getCashierById(Long cashierId);

    Future<PagedResult<CashierResponseDeleteAt>> getCashiersActive(FindAllCashiers req);

    Future<PagedResult<CashierResponseDeleteAt>> getCashiersTrashed(FindAllCashiers req);

    Future<PagedResult<CashierResponse>> getCashiersByMerchant(FindAllCashierMerchant req);
}
