package io.example.cashier.repository;

import io.example.cashier.model.Cashier;
import io.vertx.core.Future;

public interface CashierCommandRepository {
    Future<Cashier> createCashier(Long merchantId, Long userId, String name);
    Future<Cashier> updateCashier(Long cashierId, String name);
    Future<Cashier> trashCashier(Long cashierId);
    Future<Cashier> restoreCashier(Long cashierId);
    Future<Boolean> deleteCashierPermanent(Long cashierId);
    Future<Boolean> restoreAllCashier();
    Future<Boolean> deleteAllCashierPermanent();
}
