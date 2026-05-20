package io.example.order_item.handler;

import com.google.protobuf.Empty;
import io.example.order_item.service.OrderItemCommandService;
import io.vertx.core.Future;
import pb.order_item.OrderItem.*;
import pb.order_item.OrderItemCommand.*;

public class OrderItemCommandHandler implements pb.order_item.VertxOrderItemCommandServiceGrpcServer.OrderItemCommandServiceApi {
    private final OrderItemCommandService service;

    public OrderItemCommandHandler(OrderItemCommandService service) {
        this.service = service;
    }

    @Override
    public Future<ApiResponseOrderItem> createOrderItem(CreateOrderItemRequest req) {
        io.example.order_item.domain.requests.CreateOrderItemRequest domainReq = io.example.order_item.domain.requests.CreateOrderItemRequest.builder()
                .orderId((long) req.getOrderId())
                .productId((long) req.getProductId())
                .quantity(req.getQuantity())
                .price(req.getPrice())
                .build();

        return service.create(domainReq)
                .map(resp -> {
                    var builder = ApiResponseOrderItem.newBuilder()
                            .setStatus(resp.status())
                            .setMessage(resp.message());
                    if (resp.data() != null) {
                        builder.setData(ProtoConverter.fromOrderItemResponse(resp.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<ApiResponseOrderItem> updateOrderItem(UpdateOrderItemRequest req) {
        io.example.order_item.domain.requests.UpdateOrderItemRequest domainReq = io.example.order_item.domain.requests.UpdateOrderItemRequest.builder()
                .orderItemId((long) req.getOrderItemId())
                .orderId((long) req.getOrderId())
                .productId((long) req.getProductId())
                .quantity(req.getQuantity())
                .price(req.getPrice())
                .build();

        return service.update(domainReq)
                .map(resp -> {
                    var builder = ApiResponseOrderItem.newBuilder()
                            .setStatus(resp.status())
                            .setMessage(resp.message());
                    if (resp.data() != null) {
                        builder.setData(ProtoConverter.fromOrderItemResponse(resp.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<ApiResponseOrderItemDeleteAt> trashedOrderItem(FindByIdOrderItemRequest req) {
        return service.trash((long) req.getOrderItemId())
                .map(resp -> {
                    var builder = ApiResponseOrderItemDeleteAt.newBuilder()
                            .setStatus(resp.status())
                            .setMessage(resp.message());
                    if (resp.data() != null) {
                        builder.setData(ProtoConverter.fromOrderItemResponseDeleteAt(resp.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<ApiResponseOrderItemDeleteAt> restoreOrderItem(FindByIdOrderItemRequest req) {
        return service.restore((long) req.getOrderItemId())
                .map(resp -> {
                    var builder = ApiResponseOrderItemDeleteAt.newBuilder()
                            .setStatus(resp.status())
                            .setMessage(resp.message());
                    if (resp.data() != null) {
                        builder.setData(ProtoConverter.fromOrderItemResponseDeleteAt(resp.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<ApiResponseOrderItemDelete> deleteOrderItemPermanent(FindByIdOrderItemRequest req) {
        return service.deletePermanent((long) req.getOrderItemId())
                .map(resp -> ApiResponseOrderItemDelete.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .build());
    }

    @Override
    public Future<ApiResponseOrderItemAll> restoreAllOrderItem(Empty req) {
        return service.restoreAll()
                .map(resp -> ApiResponseOrderItemAll.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .build());
    }

    @Override
    public Future<ApiResponseOrderItemAll> deleteAllOrderItemPermanent(Empty req) {
        return service.deleteAllPermanent()
                .map(resp -> ApiResponseOrderItemAll.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .build());
    }
}
