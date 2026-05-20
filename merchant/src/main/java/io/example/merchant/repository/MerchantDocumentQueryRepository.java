package io.example.merchant.repository;

import io.example.common.domain.PagedResult;
import io.example.merchant.domain.requests.FindAllMerchantDocuments;
import io.example.merchant.model.MerchantDocument;
import io.vertx.core.Future;

public interface MerchantDocumentQueryRepository {
    Future<PagedResult<MerchantDocument>> getDocuments(FindAllMerchantDocuments req);
    Future<PagedResult<MerchantDocument>> getDocumentsActive(FindAllMerchantDocuments req);
    Future<PagedResult<MerchantDocument>> getDocumentsTrashed(FindAllMerchantDocuments req);
    Future<MerchantDocument> getDocumentById(Integer documentId);
}
