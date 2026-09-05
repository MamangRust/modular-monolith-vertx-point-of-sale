package io.example.transaction.repository.impl;

import io.example.transaction.repository.MerchantQueryRepository;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;

public class MerchantQueryRepositoryImpl implements MerchantQueryRepository {
    private final VertxMerchantQueryServiceGrpcClient merchantQueryClient;

    public MerchantQueryRepositoryImpl(VertxMerchantQueryServiceGrpcClient merchantQueryClient) {
        this.merchantQueryClient = merchantQueryClient;
    }

    @Override
    public Future<String> findContactEmailByMerchantId(Integer merchantId) {
        if (merchantId == null) {
            return Future.succeededFuture(null);
        }
        FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                .setMerchantId(merchantId)
                .build();
        return merchantQueryClient.findByIdMerchant(request)
                .map(response -> {
                    if (response != null && response.hasData()) {
                        return response.getData().getContactEmail();
                    }
                    return null;
                })
                .recover(err -> {
                    return Future.succeededFuture(null);
                });
    }
}
