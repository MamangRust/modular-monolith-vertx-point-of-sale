package io.example.merchant.repository.impl;

import io.example.common.domain.PagedResult;
import io.example.merchant.domain.requests.FindAllMerchants;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class MerchantQueryRepositoryImpl implements MerchantQueryRepository {
    private final Pool client;

    public MerchantQueryRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<PagedResult<Merchant>> getMerchants(FindAllMerchants req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client.preparedQuery("""
                SELECT
                    merchant_id,
                    merchant_no,
                    name,
                    api_key,
                    user_id,
                    status,
                    created_at,
                    updated_at,
                    deleted_at,
                    COUNT(*) OVER () AS total_count
                FROM merchants
                WHERE
                    deleted_at IS NULL
                    AND (
                        $1::TEXT IS NULL
                        OR name ILIKE '%' || $1 || '%'
                        OR api_key ILIKE '%' || $1 || '%'
                    )
                ORDER BY created_at DESC
                LIMIT $2
                OFFSET $3;
                """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedMerchants);
    }

    @Override
    public Future<PagedResult<Merchant>> getMerchantsActive(FindAllMerchants req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client.preparedQuery("""
                SELECT
                    merchant_id,
                    merchant_no,
                    name,
                    api_key,
                    user_id,
                    status,
                    created_at,
                    updated_at,
                    deleted_at,
                    COUNT(*) OVER () AS total_count
                FROM merchants
                WHERE
                    deleted_at IS NULL
                    AND status = 'active'
                    AND (
                        $1::TEXT IS NULL
                        OR name ILIKE '%' || $1 || '%'
                        OR api_key ILIKE '%' || $1 || '%'
                    )
                ORDER BY created_at DESC
                LIMIT $2
                OFFSET $3;
                """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedMerchants);
    }

    @Override
    public Future<PagedResult<Merchant>> getMerchantsTrashed(FindAllMerchants req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client.preparedQuery("""
                SELECT
                    merchant_id,
                    merchant_no,
                    name,
                    api_key,
                    user_id,
                    status,
                    created_at,
                    updated_at,
                    deleted_at,
                    COUNT(*) OVER () AS total_count
                FROM merchants
                WHERE
                    deleted_at IS NOT NULL
                    AND (
                        $1::TEXT IS NULL
                        OR name ILIKE '%' || $1 || '%'
                        OR api_key ILIKE '%' || $1 || '%'
                    )
                ORDER BY created_at DESC
                LIMIT $2
                OFFSET $3;
                """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedMerchants);
    }

    @Override
    public Future<Merchant> getMerchantById(Integer merchantId) {
        return client.preparedQuery("""
                SELECT
                    merchant_id,
                    merchant_no,
                    name,
                    api_key,
                    user_id,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                FROM merchants
                WHERE
                    merchant_id = $1
                    AND deleted_at IS NULL;
                """)
                .execute(Tuple.of(merchantId))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Merchant> getMerchantByApiKey(String apiKey) {
        return client.preparedQuery("""
                SELECT
                    merchant_id,
                    merchant_no,
                    name,
                    api_key,
                    user_id,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                FROM merchants
                WHERE
                    api_key = $1
                    AND deleted_at IS NULL;
                """)
                .execute(Tuple.of(apiKey))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<List<Merchant>> getMerchantsByUserId(Integer userId) {
        return client.preparedQuery("""
                SELECT
                    merchant_id,
                    merchant_no,
                    name,
                    api_key,
                    user_id,
                    status,
                    created_at,
                    updated_at,
                    deleted_at
                FROM merchants
                WHERE
                    user_id = $1
                    AND deleted_at IS NULL;
                """)
                .execute(Tuple.of(userId))
                .map(rows -> {
                    List<Merchant> list = new ArrayList<>();
                    for (Row row : rows) {
                        list.add(Merchant.fromRow(row));
                    }
                    return list;
                });
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search;
    }

    private PagedResult<Merchant> mapPagedMerchants(RowSet<Row> rows) {
        List<Merchant> list = new ArrayList<>();
        int total = 0;
        for (Row row : rows) {
            list.add(Merchant.fromRow(row));
            if (total == 0) {
                total = row.getInteger("total_count");
            }
        }
        return new PagedResult<>(list, total);
    }
}
