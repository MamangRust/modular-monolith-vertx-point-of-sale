package io.example.merchant.repository.impl;

import io.example.merchant.domain.requests.CreateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;

public class MerchantDocumentCommandRepositoryImpl implements MerchantDocumentCommandRepository {
    private final Pool client;

    public MerchantDocumentCommandRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<MerchantDocument> createMerchantDocument(CreateMerchantDocumentRequest request) {
        return client
                .preparedQuery(
                        """
                                INSERT INTO merchant_documents (merchant_id, document_type, document_url, status)
                                VALUES ($1, $2, $3, 'pending')
                                RETURNING document_id, merchant_id, document_type, document_url, status, note, uploaded_at, created_at, updated_at, deleted_at;
                                """)
                .execute(Tuple.of(request.getMerchantId(), request.getDocumentType(), request.getDocumentUrl()))
                .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<MerchantDocument> updateMerchantDocument(UpdateMerchantDocumentRequest request) {
        return client
                .preparedQuery(
                        """
                                UPDATE merchant_documents
                                SET
                                    merchant_id = $2,
                                    document_type = $3,
                                    document_url = $4,
                                    note = $5,
                                    status = $6,
                                    updated_at = CURRENT_TIMESTAMP
                                WHERE
                                    document_id = $1
                                    AND deleted_at IS NULL
                                RETURNING document_id, merchant_id, document_type, document_url, status, note, uploaded_at, created_at, updated_at, deleted_at;
                                """)
                .execute(Tuple.of(request.getDocumentId(), request.getMerchantId(), request.getDocumentType(),
                        request.getDocumentUrl(), request.getNote(), request.getStatus()))
                .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<MerchantDocument> updateMerchantDocumentStatus(UpdateMerchantDocumentStatusRequest request) {
        return client
                .preparedQuery(
                        """
                                UPDATE merchant_documents
                                SET
                                    note = $3,
                                    status = $4,
                                    updated_at = CURRENT_TIMESTAMP
                                WHERE
                                    document_id = $1
                                    AND merchant_id = $2
                                    AND deleted_at IS NULL
                                RETURNING document_id, merchant_id, document_type, document_url, status, note, uploaded_at, created_at, updated_at, deleted_at;
                                """)
                .execute(Tuple.of(request.getDocumentId(), request.getMerchantId(), request.getNote(),
                        request.getStatus()))
                .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<MerchantDocument> trashMerchantDocument(Integer documentId) {
        return client
                .preparedQuery(
                        """
                                UPDATE merchant_documents
                                SET
                                    deleted_at = CURRENT_TIMESTAMP
                                WHERE
                                    document_id = $1
                                    AND deleted_at IS NULL
                                RETURNING document_id, merchant_id, document_type, document_url, status, note, uploaded_at, created_at, updated_at, deleted_at;
                                """)
                .execute(Tuple.of(documentId))
                .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<MerchantDocument> restoreMerchantDocument(Integer documentId) {
        return client
                .preparedQuery(
                        """
                                UPDATE merchant_documents
                                SET
                                    deleted_at = NULL
                                WHERE
                                    document_id = $1
                                    AND deleted_at IS NOT NULL
                                RETURNING document_id, merchant_id, document_type, document_url, status, note, uploaded_at, created_at, updated_at, deleted_at;
                                """)
                .execute(Tuple.of(documentId))
                .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<Boolean> deleteMerchantDocumentPermanent(Integer documentId) {
        return client.preparedQuery("""
                DELETE FROM merchant_documents
                WHERE document_id = $1 AND deleted_at IS NOT NULL;
                """)
                .execute(Tuple.of(documentId))
                .map(rows -> rows.rowCount() > 0);
    }

    @Override
    public Future<Boolean> restoreAllMerchantDocument() {
        return client.preparedQuery("""
                UPDATE merchant_documents
                SET deleted_at = NULL
                WHERE deleted_at IS NOT NULL;
                """)
                .execute()
                .map(rows -> rows.rowCount() > 0);
    }

    @Override
    public Future<Boolean> deleteAllMerchantDocumentPermanent() {
        return client.preparedQuery("""
                DELETE FROM merchant_documents
                WHERE deleted_at IS NOT NULL;
                """)
                .execute()
                .map(rows -> rows.rowCount() > 0);
    }
}
