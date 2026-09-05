package io.example.order_item.handler;

import com.google.protobuf.Empty;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.order_item.service.OrderItemCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.order_item.OrderItem.ApiResponseOrderItem;
import pb.order_item.OrderItem.ApiResponseOrderItemAll;
import pb.order_item.OrderItem.ApiResponseOrderItemDelete;
import pb.order_item.OrderItem.ApiResponseOrderItemDeleteAt;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;
import pb.order_item.OrderItemCommand.CreateOrderItemRequest;
import pb.order_item.OrderItemCommand.UpdateOrderItemRequest;
import pb.order_item.VertxOrderItemCommandServiceGrpcServer;

@RequiredArgsConstructor
public class OrderItemCommandHandler implements VertxOrderItemCommandServiceGrpcServer.OrderItemCommandServiceApi {
    private final OrderItemCommandService service;

    @Override
    public Future<ApiResponseOrderItem> createOrderItem(CreateOrderItemRequest req) {
        io.example.order_item.domain.requests.CreateOrderItemRequest domainReq = io.example.order_item.domain.requests.CreateOrderItemRequest
                .builder()
                .orderId((long) req.getOrderId())
                .productId((long) req.getProductId())
                .quantity(req.getQuantity())
                .price(req.getPrice())
                .build();

        return service.create(domainReq)
                .map(item -> ApiResponseOrderItem.newBuilder()
                        .setStatus("success")
                        .setMessage("Order item created successfully")
                        .setData(ProtoConverter.fromOrderItemResponse(item))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseOrderItem> updateOrderItem(UpdateOrderItemRequest req) {
        io.example.order_item.domain.requests.UpdateOrderItemRequest domainReq = io.example.order_item.domain.requests.UpdateOrderItemRequest
                .builder()
                .orderItemId((long) req.getOrderItemId())
                .orderId((long) req.getOrderId())
                .productId((long) req.getProductId())
                .quantity(req.getQuantity())
                .price(req.getPrice())
                .build();

        return service.update(domainReq)
                .map(item -> ApiResponseOrderItem.newBuilder()
                        .setStatus("success")
                        .setMessage("Order item updated successfully")
                        .setData(ProtoConverter.fromOrderItemResponse(item))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseOrderItemDeleteAt> trashedOrderItem(FindByIdOrderItemRequest req) {
        return service.trash((long) req.getOrderItemId())
                .map(item -> ApiResponseOrderItemDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Order item trashed successfully")
                        .setData(ProtoConverter.fromOrderItemResponseDeleteAt(item))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseOrderItemDeleteAt> restoreOrderItem(FindByIdOrderItemRequest req) {
        return service.restore((long) req.getOrderItemId())
                .map(item -> ApiResponseOrderItemDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Order item restored successfully")
                        .setData(ProtoConverter.fromOrderItemResponseDeleteAt(item))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseOrderItemDelete> deleteOrderItemPermanent(FindByIdOrderItemRequest req) {
        return service.deletePermanent((long) req.getOrderItemId())
                .map(v -> ApiResponseOrderItemDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Order item permanently deleted successfully")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseOrderItemAll> restoreAllOrderItem(Empty req) {
        return service.restoreAll()
                .map(v -> ApiResponseOrderItemAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All order items restored successfully")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseOrderItemAll> deleteAllOrderItemPermanent(Empty req) {
        return service.deleteAllPermanent()
                .map(v -> ApiResponseOrderItemAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All trashed order items permanently deleted successfully")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }
}