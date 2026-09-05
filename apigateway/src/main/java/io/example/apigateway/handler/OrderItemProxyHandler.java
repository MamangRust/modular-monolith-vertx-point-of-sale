package io.example.apigateway.handler;

import static io.example.apigateway.utils.GrpcGatewayUtils.sendResponse;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.order_item.OrderItem;
import pb.order_item.VertxOrderItemServiceGrpcClient;

@Slf4j
@RequiredArgsConstructor
public class OrderItemProxyHandler {
    private final VertxOrderItemServiceGrpcClient queryClient;

    public void findAll(RoutingContext ctx) {
        var req = OrderItem.FindAllOrderItemRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findAll(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findActive(RoutingContext ctx) {
        var req = OrderItem.FindAllOrderItemRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findByActive(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findTrashed(RoutingContext ctx) {
        var req = OrderItem.FindAllOrderItemRequest.newBuilder()
                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                .build();
        queryClient.findByTrashed(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }

    public void findByOrder(RoutingContext ctx) {
        int orderId = GrpcGatewayUtils.getSafePathInt(ctx, "orderId");
        var req = OrderItem.FindByIdOrderItemRequest.newBuilder().setOrderItemId(orderId).build();
        queryClient.findOrderItemByOrder(req)
                .onSuccess(resp -> sendResponse(ctx, resp, 200))
                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
    }
}
