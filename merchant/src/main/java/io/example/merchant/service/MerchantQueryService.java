package io.example.merchant.service;

import io.example.common.domain.PagedResult;
import io.example.merchant.domain.requests.FindAllMerchants;
import io.example.merchant.domain.response.MerchantResponse;
import io.example.merchant.domain.response.MerchantResponseDeleteAt;
import io.vertx.core.Future;

public interface MerchantQueryService {
    Future<PagedResult<MerchantResponse>> findAll(FindAllMerchants req);

    Future<MerchantResponse> findById(Long merchantId);

    Future<PagedResult<MerchantResponseDeleteAt>> findByActive(FindAllMerchants req);

    Future<PagedResult<MerchantResponseDeleteAt>> findByTrashed(FindAllMerchants req);
}