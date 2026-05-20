package io.example.merchant.repository;

import io.example.common.domain.PagedResult;
import io.example.merchant.domain.requests.FindAllMerchants;
import io.example.merchant.model.Merchant;
import io.vertx.core.Future;

import java.util.List;

public interface MerchantQueryRepository {
    Future<PagedResult<Merchant>> getMerchants(FindAllMerchants req);
    Future<PagedResult<Merchant>> getMerchantsActive(FindAllMerchants req);
    Future<PagedResult<Merchant>> getMerchantsTrashed(FindAllMerchants req);
    Future<Merchant> getMerchantById(Integer merchantId);
    Future<Merchant> getMerchantByApiKey(String apiKey);
    Future<List<Merchant>> getMerchantsByUserId(Integer userId);
}
