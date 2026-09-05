package io.example.order.repository.impl;

import io.example.order.domain.requests.CreateOrderItemRecordRequest;
import io.example.order.domain.requests.UpdateOrderItemRecordRequest;
import io.example.order.model.OrderItem;
import io.example.order.repository.OrderItemCommandRepository;
import io.vertx.core.Future;
import pb.order_item.VertxOrderItemCommandServiceGrpcClient;
import pb.order_item.OrderItemCommand.CreateOrderItemRequest;
import pb.order_item.OrderItemCommand.UpdateOrderItemRequest;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;
import com.google.protobuf.Empty;

public class OrderItemCommandRepositoryImpl implements OrderItemCommandRepository {
    private final VertxOrderItemCommandServiceGrpcClient client;

    public OrderItemCommandRepositoryImpl(VertxOrderItemCommandServiceGrpcClient client) {
        this.client = client;
    }

    @Override
    public Future<OrderItem> createOrderItem(CreateOrderItemRecordRequest req) {
        CreateOrderItemRequest grpcReq = CreateOrderItemRequest.newBuilder()
                .setOrderId(req.getOrderId().intValue())
                .setProductId(req.getProductId().intValue())
                .setQuantity(req.getQuantity())
                .setPrice(req.getPrice())
                .build();

        return client.createOrderItem(grpcReq)
                .map(response -> {
                    if (response != null && response.hasData()) {
                        var d = response.getData();
                        return OrderItem.builder()
                                .orderItemId((long) d.getId())
                                .orderId((long) d.getOrderId())
                                .productId((long) d.getProductId())
                                .quantity(d.getQuantity())
                                .price(d.getPrice())
                                .build();
                    }
                    return null;
                });
    }

    @Override
    public Future<OrderItem> updateOrderItem(UpdateOrderItemRecordRequest req) {
        UpdateOrderItemRequest grpcReq = UpdateOrderItemRequest.newBuilder()
                .setOrderItemId(req.getOrderItemId().intValue())
                .setQuantity(req.getQuantity())
                .setPrice(req.getPrice())
                .build();

        return client.updateOrderItem(grpcReq)
                .map(response -> {
                    if (response != null && response.hasData()) {
                        var d = response.getData();
                        return OrderItem.builder()
                                .orderItemId((long) d.getId())
                                .orderId((long) d.getOrderId())
                                .productId((long) d.getProductId())
                                .quantity(d.getQuantity())
                                .price(d.getPrice())
                                .build();
                    }
                    return null;
                });
    }

    @Override
    public Future<OrderItem> trashedOrderItem(Long orderItemId) {
        FindByIdOrderItemRequest grpcReq = FindByIdOrderItemRequest.newBuilder()
                .setOrderItemId(orderItemId.intValue())
                .build();

        return client.trashedOrderItem(grpcReq)
                .map(response -> {
                    if (response != null && response.hasData()) {
                        var d = response.getData();
                        return OrderItem.builder()
                                .orderItemId((long) d.getId())
                                .orderId((long) d.getOrderId())
                                .productId((long) d.getProductId())
                                .quantity(d.getQuantity())
                                .price(d.getPrice())
                                .build();
                    }
                    return null;
                });
    }

    @Override
    public Future<OrderItem> restoreOrderItem(Long orderItemId) {
        FindByIdOrderItemRequest grpcReq = FindByIdOrderItemRequest.newBuilder()
                .setOrderItemId(orderItemId.intValue())
                .build();

        return client.restoreOrderItem(grpcReq)
                .map(response -> {
                    if (response != null && response.hasData()) {
                        var d = response.getData();
                        return OrderItem.builder()
                                .orderItemId((long) d.getId())
                                .orderId((long) d.getOrderId())
                                .productId((long) d.getProductId())
                                .quantity(d.getQuantity())
                                .price(d.getPrice())
                                .build();
                    }
                    return null;
                });
    }

    @Override
    public Future<Boolean> deleteOrderItemPermanent(Long orderItemId) {
        FindByIdOrderItemRequest grpcReq = FindByIdOrderItemRequest.newBuilder()
                .setOrderItemId(orderItemId.intValue())
                .build();

        return client.deleteOrderItemPermanent(grpcReq)
                .map(response -> response != null && "success".equalsIgnoreCase(response.getStatus()));
    }

    @Override
    public Future<Boolean> restoreAllOrderItem() {
        return client.restoreAllOrderItem(Empty.getDefaultInstance())
                .map(response -> response != null && "success".equalsIgnoreCase(response.getStatus()));
    }

    @Override
    public Future<Boolean> deleteAllOrderPermanent() {
        return client.deleteAllOrderItemPermanent(Empty.getDefaultInstance())
                .map(response -> response != null && "success".equalsIgnoreCase(response.getStatus()));
    }
}
