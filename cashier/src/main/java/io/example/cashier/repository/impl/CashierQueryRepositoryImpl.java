package io.example.cashier.repository.impl;

import io.example.cashier.model.Cashier;
import io.example.cashier.repository.CashierQueryRepository;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class CashierQueryRepositoryImpl implements CashierQueryRepository {
    private final Pool client;

    public CashierQueryRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<PagedResult<Cashier>> findAllCashiers(String search, int page, int pageSize) {
        int offset = (page > 0 ? page - 1 : 0) * pageSize;
        return client
                .preparedQuery("""
                        SELECT
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at,
                            COUNT(*) OVER () AS total_count
                        FROM cashiers
                        WHERE
                            deleted_at IS NULL
                            AND (
                                $1::TEXT IS NULL
                                OR name ILIKE '%' || $1 || '%'
                            )
                        ORDER BY created_at DESC
                        LIMIT $2
                        OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
                .map(this::mapPagedCashiers);
    }

    @Override
    public Future<Cashier> findById(Long cashierId) {
        return client
                .preparedQuery("""
                        SELECT
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at
                        FROM cashiers
                        WHERE
                            cashier_id = $1
                            AND deleted_at IS NULL;
                        """)
                .execute(Tuple.of(cashierId))
                .map(rows -> rows.iterator().hasNext() ? Cashier.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<PagedResult<Cashier>> findByActive(String search, int page, int pageSize) {
        int offset = (page > 0 ? page - 1 : 0) * pageSize;
        return client
                .preparedQuery("""
                        SELECT
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at,
                            deleted_at,
                            COUNT(*) OVER () AS total_count
                        FROM cashiers
                        WHERE
                            deleted_at IS NULL
                            AND (
                                $1::TEXT IS NULL
                                OR name ILIKE '%' || $1 || '%'
                            )
                        ORDER BY created_at DESC
                        LIMIT $2
                        OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
                .map(this::mapPagedCashiers);
    }

    @Override
    public Future<PagedResult<Cashier>> findByTrashed(String search, int page, int pageSize) {
        int offset = (page > 0 ? page - 1 : 0) * pageSize;
        return client
                .preparedQuery("""
                        SELECT
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at,
                            deleted_at,
                            COUNT(*) OVER () AS total_count
                        FROM cashiers
                        WHERE
                            deleted_at IS NOT NULL
                            AND (
                                $1::TEXT IS NULL
                                OR name ILIKE '%' || $1 || '%'
                            )
                        ORDER BY created_at DESC
                        LIMIT $2
                        OFFSET $3;
                        """)
                .execute(Tuple.of(normalizeSearch(search), pageSize, offset))
                .map(this::mapPagedCashiers);
    }

    @Override
    public Future<PagedResult<Cashier>> findByMerchant(Long merchantId, String search, int page, int pageSize) {
        int offset = (page > 0 ? page - 1 : 0) * pageSize;
        return client
                .preparedQuery("""
                        SELECT
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at,
                            deleted_at,
                            COUNT(*) OVER () AS total_count
                        FROM cashiers
                        WHERE
                            merchant_id = $1
                            AND deleted_at IS NULL
                            AND (
                                $2::TEXT IS NULL
                                OR name ILIKE '%' || $2 || '%'
                            )
                        ORDER BY created_at DESC
                        LIMIT $3
                        OFFSET $4;
                        """)
                .execute(Tuple.of(merchantId, normalizeSearch(search), pageSize, offset))
                .map(this::mapPagedCashiers);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank())
            return null;
        return search;
    }

    private PagedResult<Cashier> mapPagedCashiers(RowSet<Row> rows) {
        List<Cashier> list = new ArrayList<>();
        int total = 0;
        for (Row row : rows) {
            list.add(Cashier.fromRow(row));
            if (total == 0)
                total = row.getInteger("total_count");
        }
        return new PagedResult<>(list, total);
    }
}
