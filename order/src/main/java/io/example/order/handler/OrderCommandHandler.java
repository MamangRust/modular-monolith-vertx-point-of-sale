package io.example.order.handler;

import java.util.stream.Collectors;

import com.google.protobuf.Empty;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.order.service.OrderCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.order.Order.ApiResponseOrder;
import pb.order.Order.ApiResponseOrderAll;
import pb.order.Order.ApiResponseOrderDelete;
import pb.order.Order.ApiResponseOrderDeleteAt;
import pb.order.Order.FindByIdOrderRequest;
import pb.order.VertxOrderCommandServiceGrpcServer;

@RequiredArgsConstructor
public class OrderCommandHandler implements VertxOrderCommandServiceGrpcServer.OrderCommandServiceApi {
        private final OrderCommandService service;

        @Override
        public Future<ApiResponseOrder> create(pb.order.Order.CreateOrderRequest req) {
                io.example.order.domain.requests.CreateOrderRequest domainReq = new io.example.order.domain.requests.CreateOrderRequest(
                                req.getMerchantId(),
                                req.getCashierId(),
                                req.getItemsList().stream().map(
                                                item -> new io.example.order.domain.requests.CreateOrderItemRequest(
                                                                (long) item.getProductId(),
                                                                item.getQuantity()))
                                                .collect(Collectors.toList()));

                return service.createOrder(domainReq)
                                .map(order -> ApiResponseOrder.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Order created successfully")
                                                .setData(ProtoConverter.toResponse(order))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrder> update(pb.order.Order.UpdateOrderRequest req) {
                io.example.order.domain.requests.UpdateOrderRequest domainReq = new io.example.order.domain.requests.UpdateOrderRequest(
                                req.getOrderId(),
                                req.getCashierId(),
                                req.getItemsList().stream().map(
                                                item -> new io.example.order.domain.requests.UpdateOrderItemRequest(
                                                                (long) item.getOrderItemId(),
                                                                (long) item.getProductId(),
                                                                item.getQuantity()))
                                                .collect(Collectors.toList()));

                return service.updateOrder(domainReq)
                                .map(order -> ApiResponseOrder.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Order updated successfully")
                                                .setData(ProtoConverter.toResponse(order))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderDeleteAt> trashedOrder(FindByIdOrderRequest req) {
                return service.trashedOrder((long) req.getId())
                                .map(order -> ApiResponseOrderDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Order trashed successfully")
                                                .setData(ProtoConverter.toResponseDeleteAt(order))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderDeleteAt> restoreOrder(FindByIdOrderRequest req) {
                return service.restoreOrder((long) req.getId())
                                .map(order -> ApiResponseOrderDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Order restored successfully")
                                                .setData(ProtoConverter.toResponseDeleteAt(order))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderDelete> deleteOrderPermanent(FindByIdOrderRequest req) {
                return service.deleteOrderPermanent((long) req.getId())
                                .map(res -> ApiResponseOrderDelete.newBuilder()
                                                .setStatus(res ? "success" : "failed")
                                                .setMessage(res ? "Order deleted permanently"
                                                                : "Failed to delete order")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderAll> restoreAllOrder(Empty req) {
                return service.restoreAllOrder()
                                .map(res -> ApiResponseOrderAll.newBuilder()
                                                .setStatus(res ? "success" : "failed")
                                                .setMessage(res ? "All orders restored"
                                                                : "Failed to restore all orders")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderAll> deleteAllOrderPermanent(Empty req) {
                return service.deleteAllOrderPermanent()
                                .map(res -> ApiResponseOrderAll.newBuilder()
                                                .setStatus(res ? "success" : "failed")
                                                .setMessage(res ? "All orders permanently deleted"
                                                                : "Failed to delete all orders")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}
