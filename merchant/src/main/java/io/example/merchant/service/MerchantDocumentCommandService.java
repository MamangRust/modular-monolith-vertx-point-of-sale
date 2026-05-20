package io.example.merchant.service;

import io.example.merchant.domain.requests.CreateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest;
import io.example.merchant.model.MerchantDocument;
import io.vertx.core.Future;

public interface MerchantDocumentCommandService {
    Future<MerchantDocument> createMerchantDocument(CreateMerchantDocumentRequest request);
    Future<MerchantDocument> updateMerchantDocument(UpdateMerchantDocumentRequest request);
    Future<MerchantDocument> updateMerchantDocumentStatus(UpdateMerchantDocumentStatusRequest request);
    Future<MerchantDocument> trashMerchantDocument(Integer documentId);
    Future<MerchantDocument> restoreMerchantDocument(Integer documentId);
    Future<Boolean> deleteMerchantDocumentPermanent(Integer documentId);
    Future<Boolean> restoreAllMerchantDocument();
    Future<Boolean> deleteAllMerchantDocumentPermanent();
}
