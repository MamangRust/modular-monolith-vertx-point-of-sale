package io.example.cashier.repository;

import io.example.cashier.domain.requests.cashier.CreateCashierRequest;
import io.example.cashier.domain.requests.cashier.UpdateCashierRequest;
import io.example.cashier.model.Cashier;
import io.vertx.core.Future;

public interface CashierCommandRepository {
    Future<Cashier> createCashier(CreateCashierRequest req);

    Future<Cashier> updateCashier(UpdateCashierRequest req);

    Future<Cashier> trashCashier(Long cashierId);

    Future<Cashier> restoreCashier(Long cashierId);

    Future<Boolean> deleteCashierPermanent(Long cashierId);

    Future<Integer> restoreAllCashier();

    Future<Integer> deleteAllCashierPermanent();
}
