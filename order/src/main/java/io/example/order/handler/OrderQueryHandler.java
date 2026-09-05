package io.example.order.handler;

import java.util.stream.Collectors;

import io.example.order.domain.requests.FindAllOrderMerchant;
import io.example.order.domain.requests.FindAllOrders;
import io.example.order.domain.requests.MonthOrderMerchant;
import io.example.order.domain.requests.MonthTotalRevenue;
import io.example.order.domain.requests.MonthTotalRevenueMerchant;
import io.example.order.domain.requests.YearOrderMerchant;
import io.example.order.domain.requests.YearTotalRevenueMerchant;
import io.example.common.grpc.GrpcExceptionMapper;
import io.example.order.service.OrderQueryService;
import io.example.order.service.OrderStatByMerchantService;
import io.example.order.service.OrderStatsService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.order.Order.ApiResponseOrder;
import pb.order.Order.ApiResponseOrderMonthly;
import pb.order.Order.ApiResponseOrderMonthlyTotalRevenue;
import pb.order.Order.ApiResponseOrderYearly;
import pb.order.Order.ApiResponseOrderYearlyTotalRevenue;
import pb.order.Order.FindAllOrderMerchantRequest;
import pb.order.Order.FindAllOrderRequest;
import pb.order.Order.FindByIdOrderRequest;
import pb.order.Order.FindYearMonthTotalRevenue;
import pb.order.Order.FindYearMonthTotalRevenueById;
import pb.order.Order.FindYearMonthTotalRevenueByMerchant;
import pb.order.Order.FindYearOrder;
import pb.order.Order.FindYearOrderByMerchant;
import pb.order.Order.FindYearTotalRevenue;
import pb.order.Order.FindYearTotalRevenueById;
import pb.order.Order.FindYearTotalRevenueByMerchant;
import pb.order.OrderQuery.ApiResponsePaginationOrder;
import pb.order.OrderQuery.ApiResponsePaginationOrderDeleteAt;
import pb.order.VertxOrderQueryServiceGrpcServer;

@RequiredArgsConstructor
public class OrderQueryHandler implements VertxOrderQueryServiceGrpcServer.OrderQueryServiceApi {
        private final OrderQueryService queryService;
        private final OrderStatsService statsService;
        private final OrderStatByMerchantService statByMerchantService;

        @Override
        public Future<ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenue(FindYearMonthTotalRevenue req) {
                MonthTotalRevenue domainReq = new MonthTotalRevenue(req.getYear(), req.getMonth());
                return statsService.findMonthlyTotalRevenue(domainReq)
                                .map(list -> ApiResponseOrderMonthlyTotalRevenue.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total revenue retrieved")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toMonthlyTotalRevenueResponse)
                                                                .collect(Collectors.toList()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenue(FindYearTotalRevenue req) {
                return statsService.findYearlyTotalRevenue(req.getYear())
                                .map(list -> ApiResponseOrderYearlyTotalRevenue.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total revenue retrieved")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toYearlyTotalRevenueResponse)
                                                                .collect(Collectors.toList()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenueById(
                        FindYearMonthTotalRevenueById req) {
                return Future.succeededFuture(ApiResponseOrderMonthlyTotalRevenue.newBuilder()
                                .setStatus("success")
                                .setMessage("Not implemented for order stats directly")
                                .build());
        }

        @Override
        public Future<ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenueById(FindYearTotalRevenueById req) {
                return Future.succeededFuture(ApiResponseOrderYearlyTotalRevenue.newBuilder()
                                .setStatus("success")
                                .setMessage("Not implemented for order stats directly")
                                .build());
        }

        @Override
        public Future<ApiResponseOrderMonthlyTotalRevenue> findMonthlyTotalRevenueByMerchant(
                        FindYearMonthTotalRevenueByMerchant req) {
                MonthTotalRevenueMerchant domainReq = new MonthTotalRevenueMerchant((long) req.getMerchantId(),
                                req.getYear(), req.getMonth());
                return statByMerchantService.findMonthlyTotalRevenueByMerchant(domainReq)
                                .map(list -> ApiResponseOrderMonthlyTotalRevenue.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly total revenue by merchant retrieved")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toMonthlyTotalRevenueResponse)
                                                                .collect(Collectors.toList()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderYearlyTotalRevenue> findYearlyTotalRevenueByMerchant(
                        FindYearTotalRevenueByMerchant req) {
                YearTotalRevenueMerchant domainReq = new YearTotalRevenueMerchant((long) req.getMerchantId(),
                                req.getYear());
                return statByMerchantService.findYearlyTotalRevenueByMerchant(domainReq)
                                .map(list -> ApiResponseOrderYearlyTotalRevenue.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly total revenue by merchant retrieved")
                                                .addAllData(list.stream()
                                                                .map(ProtoConverter::toYearlyTotalRevenueResponse)
                                                                .collect(Collectors.toList()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationOrder> findAll(FindAllOrderRequest req) {
                FindAllOrders domainReq = new FindAllOrders(req.getPage(), req.getPageSize(), req.getSearch());
                return queryService.findAll(domainReq)
                                .map(result -> ApiResponsePaginationOrder.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Orders retrieved successfully")
                                                .addAllData(result.getData().stream().map(ProtoConverter::toResponse)
                                                                .collect(Collectors.toList()))
                                                .setPagination(ProtoConverter.toPaginationMeta(result, req.getPage(),
                                                                req.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationOrder> findByMerchant(FindAllOrderMerchantRequest req) {
                FindAllOrderMerchant domainReq = new FindAllOrderMerchant(req.getPage(), req.getPageSize(),
                                req.getSearch(), (long) req.getMerchantId());
                return queryService.findByMerchant(domainReq)
                                .map(result -> ApiResponsePaginationOrder.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Orders by merchant retrieved successfully")
                                                .addAllData(result.getData().stream().map(ProtoConverter::toResponse)
                                                                .collect(Collectors.toList()))
                                                .setPagination(ProtoConverter.toPaginationMeta(result, req.getPage(),
                                                                req.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrder> findById(FindByIdOrderRequest req) {
                return queryService.findById((long) req.getId())
                                .compose(order -> {
                                        if (order == null) {
                                                return Future.failedFuture(
                                                                new io.example.common.exception.grpc.NotFoundException("Order not found"));
                                        }
                                        return Future.succeededFuture(ApiResponseOrder.newBuilder()
                                                        .setStatus("success")
                                                        .setMessage("Order found")
                                                        .setData(ProtoConverter.toResponse(order))
                                                        .build());
                                })
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderMonthly> findMonthlyRevenue(FindYearOrder req) {
                return statsService.findMonthlyOrder(req.getYear())
                                .map(list -> ApiResponseOrderMonthly.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly revenue statistics retrieved")
                                                .addAllData(list.stream().map(ProtoConverter::toMonthlyResponse)
                                                                .collect(Collectors.toList()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderYearly> findYearlyRevenue(FindYearOrder req) {
                return statsService.findYearlyOrder(req.getYear())
                                .map(list -> ApiResponseOrderYearly.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly revenue statistics retrieved")
                                                .addAllData(list.stream().map(ProtoConverter::toYearlyResponse)
                                                                .collect(Collectors.toList()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderMonthly> findMonthlyRevenueByMerchant(FindYearOrderByMerchant req) {
                MonthOrderMerchant domainReq = new MonthOrderMerchant((long) req.getMerchantId(), req.getYear());
                return statByMerchantService.findMonthlyOrderByMerchant(domainReq)
                                .map(list -> ApiResponseOrderMonthly.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Monthly revenue by merchant retrieved")
                                                .addAllData(list.stream().map(ProtoConverter::toMonthlyResponse)
                                                                .collect(Collectors.toList()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseOrderYearly> findYearlyRevenueByMerchant(FindYearOrderByMerchant req) {
                YearOrderMerchant domainReq = new YearOrderMerchant((long) req.getMerchantId(), req.getYear());
                return statByMerchantService.findYearlyOrderByMerchant(domainReq)
                                .map(list -> ApiResponseOrderYearly.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Yearly revenue by merchant retrieved")
                                                .addAllData(list.stream().map(ProtoConverter::toYearlyResponse)
                                                                .collect(Collectors.toList()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationOrderDeleteAt> findByActive(FindAllOrderRequest req) {
                FindAllOrders domainReq = new FindAllOrders(req.getPage(), req.getPageSize(), req.getSearch());
                return queryService.findByActive(domainReq)
                                .map(result -> ApiResponsePaginationOrderDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Active orders retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toResponseDeleteAt)
                                                                .collect(Collectors.toList()))
                                                .setPagination(ProtoConverter.toPaginationMeta(result, req.getPage(),
                                                                req.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationOrderDeleteAt> findByTrashed(FindAllOrderRequest req) {
                FindAllOrders domainReq = new FindAllOrders(req.getPage(), req.getPageSize(), req.getSearch());
                return queryService.findByTrashed(domainReq)
                                .map(result -> ApiResponsePaginationOrderDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Trashed orders retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toResponseDeleteAt)
                                                                .collect(Collectors.toList()))
                                                .setPagination(ProtoConverter.toPaginationMeta(result, req.getPage(),
                                                                req.getPageSize()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}
