package io.example.order.repository.impl;

import io.example.order.repository.MerchantQueryRepository;
import io.vertx.core.Future;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;
import pb.merchant.Merchant.FindByIdMerchantRequest;

public class MerchantQueryRepositoryImpl implements MerchantQueryRepository {
    private final VertxMerchantQueryServiceGrpcClient client;

    public MerchantQueryRepositoryImpl(VertxMerchantQueryServiceGrpcClient client) {
        this.client = client;
    }

    @Override
    public Future<Boolean> existsById(Integer merchantId) {
        if (merchantId == null) {
            return Future.succeededFuture(false);
        }
        FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                .setMerchantId(merchantId)
                .build();
        return client.findByIdMerchant(request)
                .map(response -> response != null && response.hasData() && response.getData().getId() > 0)
                .recover(err -> Future.succeededFuture(false));
    }
}
