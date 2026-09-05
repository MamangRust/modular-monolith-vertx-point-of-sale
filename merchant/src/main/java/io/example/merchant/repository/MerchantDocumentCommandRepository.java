package io.example.merchant.repository;

import io.example.merchant.domain.requests.CreateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest;
import io.example.merchant.model.MerchantDocument;
import io.vertx.core.Future;

public interface MerchantDocumentCommandRepository {
    Future<MerchantDocument> createMerchantDocument(CreateMerchantDocumentRequest request);

    Future<MerchantDocument> updateMerchantDocument(UpdateMerchantDocumentRequest request);

    Future<MerchantDocument> updateMerchantDocumentStatus(UpdateMerchantDocumentStatusRequest request);

    Future<MerchantDocument> trashMerchantDocument(Long documentId);

    Future<MerchantDocument> restoreMerchantDocument(Long documentId);

    Future<Boolean> deleteMerchantDocumentPermanent(Long documentId);

    Future<Integer> restoreAllMerchantDocument();

    Future<Integer> deleteAllMerchantDocumentPermanent();
}
