package io.example.order.repository.impl;

import io.example.order.repository.CashierQueryRepository;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.cashier.VertxCashierServiceGrpcClient;
import pb.cashier.Cashier.FindByIdCashierRequest;

@RequiredArgsConstructor
public class CashierQueryRepositoryImpl implements CashierQueryRepository {
    private final VertxCashierServiceGrpcClient client;

    @Override
    public Future<Boolean> existsById(Long cashierId) {
        if (cashierId == null) {
            return Future.succeededFuture(false);
        }
        FindByIdCashierRequest request = FindByIdCashierRequest.newBuilder()
                .setId(cashierId.intValue())
                .build();
        return client.findById(request)
                .map(response -> response != null && response.hasData() && response.getData().getId() > 0)
                .recover(err -> Future.succeededFuture(false));
    }
}
