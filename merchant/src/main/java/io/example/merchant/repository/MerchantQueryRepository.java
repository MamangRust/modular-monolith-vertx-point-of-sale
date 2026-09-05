package io.example.merchant.repository;

import io.example.common.domain.PagedResult;
import io.example.merchant.domain.requests.FindAllMerchants;
import io.example.merchant.model.Merchant;
import io.vertx.core.Future;

public interface MerchantQueryRepository {
    Future<PagedResult<Merchant>> getMerchants(FindAllMerchants req);

    Future<PagedResult<Merchant>> getMerchantsActive(FindAllMerchants req);

    Future<PagedResult<Merchant>> getMerchantsTrashed(FindAllMerchants req);

    Future<Merchant> getMerchantById(Long merchantId);

    Future<Merchant> findByTrashedId(Long merchantId);
}
