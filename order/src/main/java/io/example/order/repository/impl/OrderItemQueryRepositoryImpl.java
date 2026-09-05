package io.example.order.repository.impl;

import java.util.ArrayList;
import java.util.List;
import io.example.order.model.OrderItem;
import io.example.order.repository.OrderItemQueryRepository;
import io.vertx.core.Future;
import pb.order_item.VertxOrderItemServiceGrpcClient;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;

public class OrderItemQueryRepositoryImpl implements OrderItemQueryRepository {
    private final VertxOrderItemServiceGrpcClient client;

    public OrderItemQueryRepositoryImpl(VertxOrderItemServiceGrpcClient client) {
        this.client = client;
    }

    @Override
    public Future<List<OrderItem>> findOrderItemByOrder(Long orderId) {
        if (orderId == null) {
            return Future.succeededFuture(new ArrayList<>());
        }
        FindByIdOrderItemRequest request = FindByIdOrderItemRequest.newBuilder()
                .setOrderItemId(orderId.intValue())
                .build();

        return client.findOrderItemByOrder(request)
                .map(response -> {
                    List<OrderItem> items = new ArrayList<>();
                    if (response != null && response.getDataList() != null) {
                        response.getDataList().forEach(d -> {
                            items.add(OrderItem.builder()
                                    .orderItemId((long) d.getId())
                                    .orderId((long) d.getOrderId())
                                    .productId((long) d.getProductId())
                                    .quantity(d.getQuantity())
                                    .price(d.getPrice())
                                    .build());
                        });
                    }
                    return items;
                })
                .recover(err -> Future.succeededFuture(new ArrayList<>()));
    }

    @Override
    public Future<Integer> calculateTotalPrice(Long orderId) {
        return findOrderItemByOrder(orderId)
                .map(items -> {
                    int total = 0;
                    for (OrderItem item : items) {
                        total += item.getQuantity() * item.getPrice();
                    }
                    return total;
                });
    }
}
