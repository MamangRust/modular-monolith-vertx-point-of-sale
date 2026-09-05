package io.example.merchant.service;

import io.example.merchant.domain.requests.CreateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest;
import io.example.merchant.domain.response.MerchantDocumentResponse;
import io.example.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import io.vertx.core.Future;

public interface MerchantDocumentCommandService {
    Future<MerchantDocumentResponse> createMerchantDocument(CreateMerchantDocumentRequest request);

    Future<MerchantDocumentResponse> updateMerchantDocument(UpdateMerchantDocumentRequest request);

    Future<MerchantDocumentResponse> updateMerchantDocumentStatus(UpdateMerchantDocumentStatusRequest request);

    Future<MerchantDocumentResponseDeleteAt> trashedMerchantDocument(Long documentId);

    Future<MerchantDocumentResponseDeleteAt> restoreMerchantDocument(Long documentId);

    Future<Boolean> deleteMerchantDocumentPermanent(Long documentId);

    Future<Void> restoreAllMerchantDocument();

    Future<Void> deleteAllMerchantDocumentPermanent();
}