package io.example.cashier.repository.impl;

import java.util.ArrayList;
import java.util.List;

import io.example.cashier.domain.requests.cashier.FindAllCashierMerchant;
import io.example.cashier.domain.requests.cashier.FindAllCashiers;
import io.example.cashier.model.Cashier;
import io.example.cashier.repository.CashierQueryRepository;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CashierQueryRepositoryImpl implements CashierQueryRepository {
    private final Pool client;

    @Override
    public Future<PagedResult<Cashier>> findAllCashiers(FindAllCashiers req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
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
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
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
                            updated_at,
                            deleted_at
                        FROM cashiers
                        WHERE
                            cashier_id = $1
                            AND deleted_at IS NULL;
                        """)
                .execute(Tuple.of(cashierId))
                .map(rows -> rows.iterator().hasNext() ? Cashier.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Cashier> findByName(String name) {
        return client
                .preparedQuery("""
                        SELECT
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at,
                            deleted_at
                        FROM cashiers
                        WHERE
                            name = $1
                            AND deleted_at IS NULL;
                        """)
                .execute(Tuple.of(name))
                .map(rows -> rows.iterator().hasNext() ? Cashier.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Cashier> findByTrashedId(Long cashierId) {
        return client
                .preparedQuery("""
                        SELECT
                            cashier_id,
                            merchant_id,
                            user_id,
                            name,
                            created_at,
                            updated_at,
                            deleted_at
                        FROM cashiers
                        WHERE
                            cashier_id = $1
                            AND deleted_at IS NOT NULL;
                        """)
                .execute(Tuple.of(cashierId))
                .map(rows -> rows.iterator().hasNext() ? Cashier.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<PagedResult<Cashier>> findByActive(FindAllCashiers req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
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
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedCashiers);
    }

    @Override
    public Future<PagedResult<Cashier>> findByTrashed(FindAllCashiers req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
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
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedCashiers);
    }

    @Override
    public Future<PagedResult<Cashier>> findByMerchant(FindAllCashierMerchant req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
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
                .execute(Tuple.of(req.getMerchantId(), normalizeSearch(req.getSearch()), req.getPageSize(),
                        offset))
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
