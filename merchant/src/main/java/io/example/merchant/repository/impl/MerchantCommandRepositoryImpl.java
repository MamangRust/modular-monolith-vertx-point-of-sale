package io.example.merchant.repository.impl;

import io.example.merchant.domain.requests.CreateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantStatusRequest;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MerchantCommandRepositoryImpl implements MerchantCommandRepository {
    private final Pool client;

    public Future<Merchant> createMerchant(CreateMerchantRequest req) {
        return client
                .preparedQuery("""
                        INSERT INTO
                            merchants (
                                user_id,
                                name,
                                description,
                                address,
                                contact_email,
                                contact_phone,
                                status
                            )
                        VALUES ($1, $2, $3, $4, $5, $6, COALESCE($7, 'active'))
                        RETURNING
                            merchant_id,
                            user_id,
                            name,
                            description,
                            address,
                            contact_email,
                            contact_phone,
                            status,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(
                        req.getUserId() != null ? req.getUserId().longValue() : null,
                        req.getName(),
                        req.getDescription(),
                        req.getAddress(),
                        req.getContactEmail(),
                        req.getContactPhone(),
                        req.getStatus()))
                .map(rows -> Merchant.fromRow(rows.iterator().next()));
    }

    public Future<Merchant> updateMerchant(UpdateMerchantRequest req) {
        return client
                .preparedQuery("""
                        UPDATE merchants
                        SET
                            name            = COALESCE(NULLIF($2, ''), name),
                            description     = COALESCE(NULLIF($3, ''), description),
                            address         = COALESCE(NULLIF($4, ''), address),
                            contact_email   = COALESCE(NULLIF($5, ''), contact_email),
                            contact_phone   = COALESCE(NULLIF($6, ''), contact_phone),
                            status          = COALESCE(NULLIF($7, ''), status),
                            updated_at      = CURRENT_TIMESTAMP
                        WHERE
                            merchant_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            merchant_id,
                            user_id,
                            name,
                            description,
                            address,
                            contact_email,
                            contact_phone,
                            status,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(
                        req.getMerchantId() != null ? req.getMerchantId().longValue() : 0,
                        req.getName() != null ? req.getName() : "",
                        req.getDescription() != null ? req.getDescription() : "",
                        req.getAddress() != null ? req.getAddress() : "",
                        req.getContactEmail() != null ? req.getContactEmail() : "",
                        req.getContactPhone() != null ? req.getContactPhone() : "",
                        req.getStatus() != null ? req.getStatus() : ""))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    public Future<Merchant> updateMerchantStatus(UpdateMerchantStatusRequest req) {
        return client
                .preparedQuery("""
                        UPDATE merchants
                        SET
                            status = $2,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE
                            merchant_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            merchant_id,
                            user_id,
                            name,
                            description,
                            address,
                            contact_email,
                            contact_phone,
                            status,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(req.getMerchantId(), req.getStatus()))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    public Future<Merchant> trashMerchant(Long merchantId) {
        return client
                .preparedQuery("""
                        UPDATE merchants
                        SET
                            deleted_at = current_timestamp
                        WHERE
                            merchant_id = $1
                            AND deleted_at IS NULL
                        RETURNING
                            merchant_id,
                            user_id,
                            name,
                            description,
                            address,
                            contact_email,
                            contact_phone,
                            status,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(merchantId))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    public Future<Merchant> restoreMerchant(Long merchantId) {
        return client
                .preparedQuery("""
                        UPDATE merchants
                        SET
                            deleted_at = NULL
                        WHERE
                            merchant_id = $1
                            AND deleted_at IS NOT NULL
                        RETURNING
                            merchant_id,
                            user_id,
                            name,
                            description,
                            address,
                            contact_email,
                            contact_phone,
                            status,
                            created_at,
                            updated_at,
                            deleted_at;
                        """)
                .execute(Tuple.of(merchantId))
                .map(rows -> rows.iterator().hasNext() ? Merchant.fromRow(rows.iterator().next()) : null);
    }

    public Future<Boolean> deleteMerchantPermanent(Long merchantId) {
        return client
                .preparedQuery("DELETE FROM merchants WHERE merchant_id = $1 AND deleted_at IS NOT NULL")
                .execute(Tuple.of(merchantId))
                .map(rows -> rows.rowCount() > 0);
    }

    public Future<Integer> restoreAllMerchant() {
        return client
                .preparedQuery("UPDATE merchants SET deleted_at = NULL WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount);
    }

    public Future<Integer> deleteAllMerchantPermanent() {
        return client
                .preparedQuery("DELETE FROM merchants WHERE deleted_at IS NOT NULL")
                .execute()
                .map(RowSet::rowCount);
    }
}
