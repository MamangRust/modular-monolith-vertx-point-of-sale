package io.example.merchant.repository.impl;

import io.example.common.domain.PagedResult;
import io.example.merchant.domain.requests.FindAllMerchantDocuments;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;
import java.util.List;

public class MerchantDocumentQueryRepositoryImpl implements MerchantDocumentQueryRepository {
    private final Pool client;

    public MerchantDocumentQueryRepositoryImpl(Pool client) {
        this.client = client;
    }

    @Override
    public Future<PagedResult<MerchantDocument>> getDocuments(FindAllMerchantDocuments req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client.preparedQuery("""
                SELECT
                    document_id,
                    merchant_id,
                    document_type,
                    document_url,
                    status,
                    note,
                    uploaded_at,
                    created_at,
                    updated_at,
                    deleted_at,
                    COUNT(*) OVER () AS total_count
                FROM merchant_documents
                WHERE
                    deleted_at IS NULL
                    AND (
                        $1::TEXT IS NULL
                        OR document_type ILIKE '%' || $1 || '%'
                        OR status ILIKE '%' || $1 || '%'
                    )
                ORDER BY created_at DESC
                LIMIT $2
                OFFSET $3;
                """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedDocuments);
    }

    @Override
    public Future<PagedResult<MerchantDocument>> getDocumentsActive(FindAllMerchantDocuments req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client.preparedQuery("""
                SELECT
                    document_id,
                    merchant_id,
                    document_type,
                    document_url,
                    status,
                    note,
                    uploaded_at,
                    created_at,
                    updated_at,
                    deleted_at,
                    COUNT(*) OVER () AS total_count
                FROM merchant_documents
                WHERE
                    deleted_at IS NULL
                    AND status = 'active'
                    AND (
                        $1::TEXT IS NULL
                        OR document_type ILIKE '%' || $1 || '%'
                    )
                ORDER BY created_at DESC
                LIMIT $2
                OFFSET $3;
                """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedDocuments);
    }

    @Override
    public Future<PagedResult<MerchantDocument>> getDocumentsTrashed(FindAllMerchantDocuments req) {
        int offset = (req.getPage() > 0 ? req.getPage() - 1 : 0) * req.getPageSize();
        return client.preparedQuery("""
                SELECT
                    document_id,
                    merchant_id,
                    document_type,
                    document_url,
                    status,
                    note,
                    uploaded_at,
                    created_at,
                    updated_at,
                    deleted_at,
                    COUNT(*) OVER () AS total_count
                FROM merchant_documents
                WHERE
                    deleted_at IS NOT NULL
                    AND (
                        $1::TEXT IS NULL
                        OR document_type ILIKE '%' || $1 || '%'
                        OR status ILIKE '%' || $1 || '%'
                    )
                ORDER BY created_at DESC
                LIMIT $2
                OFFSET $3;
                """)
                .execute(Tuple.of(normalizeSearch(req.getSearch()), req.getPageSize(), offset))
                .map(this::mapPagedDocuments);
    }

    @Override
    public Future<MerchantDocument> getDocumentById(Integer documentId) {
        return client.preparedQuery("""
                SELECT
                    document_id,
                    merchant_id,
                    document_type,
                    document_url,
                    status,
                    note,
                    uploaded_at,
                    created_at,
                    updated_at,
                    deleted_at
                FROM merchant_documents
                WHERE
                    document_id = $1
                    AND deleted_at IS NULL;
                """)
                .execute(Tuple.of(documentId))
                .map(rows -> rows.iterator().hasNext() ? MerchantDocument.fromRow(rows.iterator().next()) : null);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return search;
    }

    private PagedResult<MerchantDocument> mapPagedDocuments(RowSet<Row> rows) {
        List<MerchantDocument> list = new ArrayList<>();
        int total = 0;
        for (Row row : rows) {
            list.add(MerchantDocument.fromRow(row));
            if (total == 0) {
                total = row.getInteger("total_count");
            }
        }
        return new PagedResult<>(list, total);
    }
}
