package io.example.merchant.service;

import io.example.common.domain.PagedResult;
import io.example.merchant.domain.requests.FindAllMerchantDocuments;
import io.example.merchant.domain.response.MerchantDocumentResponse;
import io.example.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import io.vertx.core.Future;

public interface MerchantDocumentQueryService {
    Future<PagedResult<MerchantDocumentResponse>> findAll(FindAllMerchantDocuments req);

    Future<PagedResult<MerchantDocumentResponseDeleteAt>> findByActive(FindAllMerchantDocuments req);

    Future<PagedResult<MerchantDocumentResponseDeleteAt>> findByTrashed(FindAllMerchantDocuments req);

    Future<MerchantDocumentResponse> findById(Long documentId);
}