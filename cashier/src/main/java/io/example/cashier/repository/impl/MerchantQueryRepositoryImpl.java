package io.example.cashier.repository.impl;

import io.example.cashier.repository.MerchantQueryRepository;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;

public class MerchantQueryRepositoryImpl implements MerchantQueryRepository {
    private final VertxMerchantQueryServiceGrpcClient merchantQueryClient;

    public MerchantQueryRepositoryImpl(VertxMerchantQueryServiceGrpcClient merchantQueryClient) {
        this.merchantQueryClient = merchantQueryClient;
    }

    @Override
    public Future<Boolean> existsById(Integer merchantId) {
        if (merchantId == null) {
            return Future.succeededFuture(false);
        }
        FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                .setMerchantId(merchantId)
                .build();
        return merchantQueryClient.findByIdMerchant(request)
                .map(response -> response != null && response.hasData() && response.getData().getId() > 0)
                .recover(err -> Future.succeededFuture(false));
    }
}
