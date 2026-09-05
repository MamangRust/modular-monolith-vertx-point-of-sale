package io.example.order.repository.impl;

import io.example.order.repository.MerchantQueryRepository;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;
import pb.merchant.Merchant.FindByIdMerchantRequest;

@RequiredArgsConstructor
public class MerchantQueryRepositoryImpl implements MerchantQueryRepository {
    private final VertxMerchantQueryServiceGrpcClient client;

    @Override
    public Future<Boolean> existsById(Long merchantId) {
        if (merchantId == null) {
            return Future.succeededFuture(false);
        }
        FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                .setMerchantId(merchantId.intValue())
                .build();
        return client.findByIdMerchant(request)
                .map(response -> response != null && response.hasData() && response.getData().getId() > 0)
                .recover(err -> Future.succeededFuture(false));
    }
}
