package io.example.cashier.repository.impl;

import io.example.cashier.domain.requests.cashier.CreateCashierRequest;
import io.example.cashier.domain.requests.cashier.UpdateCashierRequest;
import io.example.cashier.model.Cashier;
import io.example.cashier.repository.CashierCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CashierCommandRepositoryImpl implements CashierCommandRepository {
    private final Pool client;

    @Override
    public Future<Cashier> createCashier(CreateCashierRequest req) {
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
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(req.getMerchantId(), req.getUserId(), req.getName()))
                .map(rows -> io.example.cashier.model.Cashier.fromRow(rows.iterator().next()));
    }

    @Override
    public Future<Cashier> updateCashier(UpdateCashierRequest req) {
        return client
                .preparedQuery("""
                        UPDATE cashiers
                        SET
                            name        = COALESCE(NULLIF($2, ''), name),
                            updated_at  = CURRENT_TIMESTAMP
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
                .execute(Tuple.of(
                        req.getCashierId() != null ? req.getCashierId().longValue() : 0,
                        req.getName() != null ? req.getName() : ""))
                .map(rows -> rows.iterator().hasNext()
                        ? io.example.cashier.model.Cashier.fromRow(rows.iterator().next())
                        : null);
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
                .map(rows -> rows.iterator().hasNext()
                        ? io.example.cashier.model.Cashier.fromRow(rows.iterator().next())
                        : null);
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
                .map(rows -> rows.iterator().hasNext()
                        ? io.example.cashier.model.Cashier.fromRow(rows.iterator().next())
                        : null);
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
    public Future<Integer> restoreAllCashier() {
        return client
                .preparedQuery("UPDATE cashiers SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount);
    }

    @Override
    public Future<Integer> deleteAllCashierPermanent() {
        return client
                .preparedQuery("DELETE FROM cashiers WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount);
    }
}
