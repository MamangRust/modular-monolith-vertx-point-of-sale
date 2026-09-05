package io.example.merchant.service;

import io.example.merchant.domain.requests.CreateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantStatusRequest;
import io.example.merchant.domain.response.MerchantResponse;
import io.example.merchant.domain.response.MerchantResponseDeleteAt;
import io.vertx.core.Future;

public interface MerchantCommandService {
    Future<MerchantResponse> createMerchant(CreateMerchantRequest request);

    Future<MerchantResponse> updateMerchant(UpdateMerchantRequest request);

    Future<MerchantResponse> updateMerchantStatus(UpdateMerchantStatusRequest request);

    Future<MerchantResponseDeleteAt> trashedMerchant(Long merchantId);

    Future<MerchantResponseDeleteAt> restoreMerchant(Long merchantId);

    Future<Void> deleteMerchantPermanent(Long merchantId);

    Future<Void> restoreAllMerchant();

    Future<Void> deleteAllMerchantPermanent();
}