package io.example.merchant.repository.impl;

import io.example.merchant.domain.requests.CreateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentCommandRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MerchantDocumentCommandRepositoryImpl implements MerchantDocumentCommandRepository {
    private final Pool client;

    @Override
    public Future<MerchantDocument> createMerchantDocument(CreateMerchantDocumentRequest request) {
        return client
                .preparedQuery(
                        """
                                INSERT INTO merchant_documents (merchant_id, document_type, document_url, status, note)
                                VALUES ($1, $2, $3, 'pending', $4)
                                RETURNING document_id, merchant_id, document_type, document_url, status, note, uploaded_at, created_at, updated_at, deleted_at;
                                """)
                .execute(Tuple.of(request.getMerchantId(), request.getDocumentType(), request.getDocumentUrl(), request.getNote() != null ? request.getNote() : ""))
                .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
    }

    @Override
    public Future<MerchantDocument> updateMerchantDocument(UpdateMerchantDocumentRequest request) {
        return client
                .preparedQuery(
                        """
                                UPDATE merchant_documents
                                SET
                                    merchant_id    = COALESCE(NULLIF($2::INT, 0), merchant_id),
                                    document_type  = COALESCE(NULLIF($3, ''), document_type),
                                    document_url   = COALESCE(NULLIF($4, ''), document_url),
                                    note           = COALESCE(NULLIF($5, ''), note),
                                    status         = COALESCE(NULLIF($6, ''), status),
                                    updated_at     = CURRENT_TIMESTAMP
                                WHERE
                                    document_id = $1
                                    AND deleted_at IS NULL
                                RETURNING document_id, merchant_id, document_type, document_url, status, note, uploaded_at, created_at, updated_at, deleted_at;
                                """)
                .execute(Tuple.of(
                        request.getDocumentId() != null ? request.getDocumentId().longValue() : 0,
                        request.getMerchantId() != null ? request.getMerchantId().longValue() : 0,
                        request.getDocumentType() != null ? request.getDocumentType() : "",
                        request.getDocumentUrl() != null ? request.getDocumentUrl() : "",
                        request.getNote() != null ? request.getNote() : "",
                        request.getStatus() != null ? request.getStatus() : ""))
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
    public Future<MerchantDocument> trashMerchantDocument(Long documentId) {
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
    public Future<MerchantDocument> restoreMerchantDocument(Long documentId) {
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
    public Future<Boolean> deleteMerchantDocumentPermanent(Long documentId) {
        return client.preparedQuery("""
                DELETE FROM merchant_documents
                WHERE document_id = $1 AND deleted_at IS NOT NULL;
                """)
                .execute(Tuple.of(documentId))
                .map(rows -> rows.rowCount() > 0);
    }

    @Override
    public Future<Integer> restoreAllMerchantDocument() {
        return client.preparedQuery("""
                UPDATE merchant_documents
                SET deleted_at = NULL
                WHERE deleted_at IS NOT NULL;
                """)
                .execute()
                .map(RowSet::rowCount);
    }

    @Override
    public Future<Integer> deleteAllMerchantDocumentPermanent() {
        return client.preparedQuery("""
                DELETE FROM merchant_documents
                WHERE deleted_at IS NOT NULL;
                """)
                .execute()
                .map(RowSet::rowCount);
    }
}
