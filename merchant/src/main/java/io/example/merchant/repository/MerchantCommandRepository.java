package io.example.merchant.repository;

import io.example.merchant.domain.requests.CreateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantStatusRequest;
import io.example.merchant.model.Merchant;
import io.vertx.core.Future;

public interface MerchantCommandRepository {
    Future<Merchant> createMerchant(CreateMerchantRequest request, String apiKey);
    Future<Merchant> updateMerchant(UpdateMerchantRequest request);
    Future<Merchant> updateMerchantStatus(UpdateMerchantStatusRequest request);
    Future<Merchant> trashMerchant(Integer merchantId);
    Future<Merchant> restoreMerchant(Integer merchantId);
    Future<Boolean> deleteMerchantPermanent(Integer merchantId);
    Future<Boolean> restoreAllMerchant();
    Future<Boolean> deleteAllMerchantPermanent();
}
