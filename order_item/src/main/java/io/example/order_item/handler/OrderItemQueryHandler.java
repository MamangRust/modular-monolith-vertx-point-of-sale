package io.example.order_item.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.order_item.domain.requests.FindAllOrderItems;
import io.example.order_item.service.OrderItemQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.common.PaginationMeta;
import pb.order_item.OrderItem.ApiResponsesOrderItem;
import pb.order_item.OrderItem.FindAllOrderItemRequest;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;
import pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItem;
import pb.order_item.OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt;
import pb.order_item.VertxOrderItemServiceGrpcServer;

@RequiredArgsConstructor
public class OrderItemQueryHandler implements VertxOrderItemServiceGrpcServer.OrderItemServiceApi {
        private final OrderItemQueryService service;

        private PaginationMeta toMeta(int page, int pageSize, int totalRecords) {
                int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalRecords / pageSize) : 0;
                return PaginationMeta.newBuilder()
                                .setCurrentPage(page)
                                .setPageSize(pageSize)
                                .setTotalPages(totalPages)
                                .setTotalRecords(totalRecords)
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
                                .map(result -> ApiResponsePaginationOrderItem.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Order items retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::fromOrderItemResponse)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                result.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationOrderItemDeleteAt> findByActive(FindAllOrderItemRequest req) {
                FindAllOrderItems domainReq = FindAllOrderItems.builder()
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .search(req.getSearch())
                                .build();

                return service.getActive(domainReq)
                                .map(result -> ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Active order items retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::fromOrderItemResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                result.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationOrderItemDeleteAt> findByTrashed(FindAllOrderItemRequest req) {
                FindAllOrderItems domainReq = FindAllOrderItems.builder()
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .search(req.getSearch())
                                .build();

                return service.getTrashed(domainReq)
                                .map(result -> ApiResponsePaginationOrderItemDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Trashed order items retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::fromOrderItemResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                result.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsesOrderItem> findOrderItemByOrder(FindByIdOrderItemRequest req) {
                return service.getByOrderId(req.getOrderItemId())
                                .map(list -> ApiResponsesOrderItem.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Order items fetched successfully")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::fromOrderItemResponse)
                                                                .toList())
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}