package io.example.merchant.repository.impl;

import io.example.merchant.domain.requests.CreateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantStatusRequest;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class MerchantCommandRepositoryImpl implements MerchantCommandRepository {
    private final Pool client;

    public MerchantCommandRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<Merchant> createMerchant(CreateMerchantRequest request, String apiKey) {
        return client.preparedQuery("""
                INSERT INTO merchants (name, api_key, user_id, status)
                VALUES ($1, $2, $3, 'pending')
                RETURNING merchant_id, merchant_no, name, api_key, user_id, status, created_at, updated_at, deleted_at;
                """)
                .execute(Tuple.of(request.getName(), apiKey, request.getUserId()))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Merchant> updateMerchant(UpdateMerchantRequest request) {
        return client.preparedQuery("""
                UPDATE merchants
                SET
                    name = $2,
                    user_id = $3,
                    status = $4,
                    updated_at = CURRENT_TIMESTAMP
                WHERE
                    merchant_id = $1
                    AND deleted_at IS NULL
                RETURNING merchant_id, merchant_no, name, api_key, user_id, status, created_at, updated_at, deleted_at;
                """)
                .execute(Tuple.of(request.getMerchantId(), request.getName(), request.getUserId(), request.getStatus()))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Merchant> updateMerchantStatus(UpdateMerchantStatusRequest request) {
        return client.preparedQuery("""
                UPDATE merchants
                SET
                    status = $2,
                    updated_at = CURRENT_TIMESTAMP
                WHERE
                    merchant_id = $1
                    AND deleted_at IS NULL
                RETURNING merchant_id, merchant_no, name, api_key, user_id, status, created_at, updated_at, deleted_at;
                """)
                .execute(Tuple.of(request.getMerchantId(), request.getStatus()))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Merchant> trashMerchant(Integer merchantId) {
        return client.preparedQuery("""
                UPDATE merchants
                SET
                    deleted_at = CURRENT_TIMESTAMP
                WHERE
                    merchant_id = $1
                    AND deleted_at IS NULL
                RETURNING merchant_id, merchant_no, name, api_key, user_id, status, created_at, updated_at, deleted_at;
                """)
                .execute(Tuple.of(merchantId))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Merchant> restoreMerchant(Integer merchantId) {
        return client.preparedQuery("""
                UPDATE merchants
                SET
                    deleted_at = NULL
                WHERE
                    merchant_id = $1
                    AND deleted_at IS NOT NULL
                RETURNING merchant_id, merchant_no, name, api_key, user_id, status, created_at, updated_at, deleted_at;
                """)
                .execute(Tuple.of(merchantId))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Boolean> deleteMerchantPermanent(Integer merchantId) {
        return client.preparedQuery("""
                DELETE FROM merchants
                WHERE merchant_id = $1 AND deleted_at IS NOT NULL;
                """)
                .execute(Tuple.of(merchantId))
                .map(rows -> rows.rowCount() > 0);
    }

    @Override
    public Future<Boolean> restoreAllMerchant() {
        return client.preparedQuery("""
                UPDATE merchants
                SET deleted_at = NULL
                WHERE deleted_at IS NOT NULL;
                """)
                .execute()
                .map(rows -> rows.rowCount() > 0);
    }

    @Override
    public Future<Boolean> deleteAllMerchantPermanent() {
        return client.preparedQuery("""
                DELETE FROM merchants
                WHERE deleted_at IS NOT NULL;
                """)
                .execute()
                .map(rows -> rows.rowCount() > 0);
    }
}
