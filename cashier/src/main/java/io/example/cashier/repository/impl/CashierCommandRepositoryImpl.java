package io.example.cashier.repository.impl;

import io.example.cashier.model.Cashier;
import io.example.cashier.repository.CashierCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class CashierCommandRepositoryImpl implements CashierCommandRepository {
    private final Pool client;

    public CashierCommandRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<Cashier> createCashier(Long merchantId, Long userId, String name) {
        return client
                .preparedQuery("""
                        INSERT INTO
                            cashiers (merchant_id, user_id, name)
                        VALUES ($1, $2, $3)
                        RETURNING
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at;
                        """)
                .execute(Tuple.of(merchantId, userId, name))
                .map(rows -> io.example.cashier.model.Cashier.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<Cashier> updateCashier(Long cashierId, String name) {
        return client
                .preparedQuery("""
                        UPDATE cashiers
                        SET
                            name = $2,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE
                            cashier_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at;
                        """)
                .execute(Tuple.of(cashierId, name))
                .map(rows -> rows.iterator().hasNext() ? io.example.cashier.model.Cashier.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Cashier> trashCashier(Long cashierId) {
        return client
                .preparedQuery("""
                        UPDATE cashiers
                        SET
                            deleted_at = current_timestamp
                        WHERE
                            cashier_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(cashierId))
                .map(rows -> rows.iterator().hasNext() ? io.example.cashier.model.Cashier.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Cashier> restoreCashier(Long cashierId) {
        return client
                .preparedQuery("""
                        UPDATE cashiers
                        SET
                            deleted_at = NULL
                        WHERE
                            cashier_id = $1
                            AND deleted_at IS NOT NULL
                        RETURNING
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(cashierId))
                .map(rows -> rows.iterator().hasNext() ? io.example.cashier.model.Cashier.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Boolean> deleteCashierPermanent(Long cashierId) {
        return client
                .preparedQuery("DELETE FROM cashiers WHERE cashier_id = $1 AND deleted_at IS NOT NULL")
                .execute(Tuple.of(cashierId))
                .map(RowSet::rowCount)
                .map(count -> count > 0);
    }

    @Override
    public Future<Boolean> restoreAllCashier() {
        return client
                .preparedQuery("UPDATE cashiers SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount)
                .map(count -> count > 0);
    }

    @Override
    public Future<Boolean> deleteAllCashierPermanent() {
        return client
                .preparedQuery("DELETE FROM cashiers WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount)
                .map(count -> count > 0);
    }
}
