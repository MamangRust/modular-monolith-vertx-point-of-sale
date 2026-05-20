package io.example.order_item.handler;

import io.example.order_item.domain.requests.FindAllOrderItems;
import io.example.order_item.service.OrderItemQueryService;
import io.vertx.core.Future;
import pb.order_item.OrderItem.*;
import pb.order_item.OrderItemQuery.*;

public class OrderItemQueryHandler implements pb.order_item.VertxOrderItemServiceGrpcServer.OrderItemServiceApi {
    private final OrderItemQueryService service;

    public OrderItemQueryHandler(OrderItemQueryService service) {
        this.service = service;
    }

    private pb.common.PaginationMeta toMeta(io.example.common.model.PaginationMeta meta) {
        if (meta == null) return pb.common.PaginationMeta.getDefaultInstance();
        return pb.common.PaginationMeta.newBuilder()
                .setCurrentPage(meta.currentPage())
                .setPageSize(meta.pageSize())
                .setTotalPages(meta.totalPages())
                .setTotalRecords(meta.totalRecords())
                .build();
    }

    @Override
    public Future<ApiResponsePaginationOrderItem> findAll(FindAllOrderItemRequest req) {
        FindAllOrderItems domainReq = FindAllOrderItems.builder()
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .search(req.getSearch())
                .build();

        return service.getAll(domainReq)
                .map(resp -> ApiResponsePaginationOrderItem.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromOrderItemResponse).toList())
                        .setPaginationMeta(toMeta(resp.pagination()))
                        .build());
    }

    @Override
    public Future<ApiResponsePaginationOrderItemDeleteAt> findByActive(FindAllOrderItemRequest req) {
        FindAllOrderItems domainReq = FindAllOrderItems.builder()
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .search(req.getSearch())
                .build();

        return service.getActive(domainReq)
                .map(resp -> ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromOrderItemResponseDeleteAt).toList())
                        .setPaginationMeta(toMeta(resp.pagination()))
                        .build());
    }

    @Override
    public Future<ApiResponsePaginationOrderItemDeleteAt> findByTrashed(FindAllOrderItemRequest req) {
        FindAllOrderItems domainReq = FindAllOrderItems.builder()
                .page(req.getPage())
                .pageSize(req.getPageSize())
                .search(req.getSearch())
                .build();

        return service.getTrashed(domainReq)
                .map(resp -> ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromOrderItemResponseDeleteAt).toList())
                        .setPaginationMeta(toMeta(resp.pagination()))
                        .build());
    }

    @Override
    public Future<ApiResponsesOrderItem> findOrderItemByOrder(FindByIdOrderItemRequest req) {
        return service.getByOrderId(req.getOrderItemId())
                .map(resp -> {
                    var builder = ApiResponsesOrderItem.newBuilder()
                            .setStatus(resp.status())
                            .setMessage(resp.message());
                    if (resp.data() != null) {
                        builder.addAllData(resp.data().stream().map(ProtoConverter::fromOrderItemResponse).toList());
                    }
                    return builder.build();
                });
    }
}
