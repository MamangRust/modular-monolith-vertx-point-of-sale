package io.example.merchant.service;

import io.example.merchant.domain.requests.CreateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantStatusRequest;
import io.example.merchant.model.Merchant;
import io.vertx.core.Future;

public interface MerchantCommandService {
    Future<Merchant> createMerchant(CreateMerchantRequest request);
    Future<Merchant> updateMerchant(UpdateMerchantRequest request);
    Future<Merchant> updateMerchantStatus(UpdateMerchantStatusRequest request);
    Future<Merchant> trashMerchant(Integer merchantId);
    Future<Merchant> restoreMerchant(Integer merchantId);
    Future<Boolean> deleteMerchantPermanent(Integer merchantId);
    Future<Boolean> restoreAllMerchant();
    Future<Boolean> deleteAllMerchantPermanent();
}
