package io.example.cashier.service;

import io.example.cashier.domain.requests.cashier.CreateCashierRequest;
import io.example.cashier.domain.requests.cashier.UpdateCashierRequest;
import io.example.cashier.domain.response.cashier.CashierResponse;
import io.example.cashier.domain.response.cashier.CashierResponseDeleteAt;
import io.vertx.core.Future;

public interface CashierCommandService {
    Future<CashierResponse> createCashier(CreateCashierRequest req);

    Future<CashierResponse> updateCashier(UpdateCashierRequest req);

    Future<CashierResponseDeleteAt> trashCashier(Long cashierId);

    Future<CashierResponseDeleteAt> restoreCashier(Long cashierId);

    Future<Void> deleteCashierPermanent(Long cashierId);

    Future<Void> restoreAllCashier();

    Future<Void> deleteAllCashierPermanent();
}
