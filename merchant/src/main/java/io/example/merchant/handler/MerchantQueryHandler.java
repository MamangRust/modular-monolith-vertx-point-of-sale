package io.example.merchant.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.domain.requests.FindAllMerchants;
import io.example.merchant.service.MerchantQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.common.PaginationMeta;
import pb.merchant.Merchant.ApiResponseMerchant;
import pb.merchant.Merchant.FindAllMerchantRequest;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.MerchantQuery.ApiResponsePaginationMerchant;
import pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt;
import pb.merchant.VertxMerchantQueryServiceGrpcServer;

@RequiredArgsConstructor
public class MerchantQueryHandler implements VertxMerchantQueryServiceGrpcServer.MerchantQueryServiceApi {
        private final MerchantQueryService service;

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
        public Future<ApiResponsePaginationMerchant> findAllMerchant(FindAllMerchantRequest req) {
                FindAllMerchants domainReq = FindAllMerchants.builder()
                                .search(req.getSearch())
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .build();

                return service.findAll(domainReq)
                                .map(result -> ApiResponsePaginationMerchant.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Merchants retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toResponse)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                result.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchant> findByIdMerchant(FindByIdMerchantRequest req) {
                return service.findById((long) req.getMerchantId())
                                .map(m -> ApiResponseMerchant.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Merchant found successfully")
                                                .setData(ProtoConverter.toResponse(m))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationMerchantDeleteAt> findByActive(FindAllMerchantRequest req) {
                FindAllMerchants domainReq = FindAllMerchants.builder()
                                .search(req.getSearch())
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .build();

                return service.findByActive(domainReq)
                                .map(result -> ApiResponsePaginationMerchantDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Active merchants retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                result.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationMerchantDeleteAt> findByTrashed(FindAllMerchantRequest req) {
                FindAllMerchants domainReq = FindAllMerchants.builder()
                                .search(req.getSearch())
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .build();

                return service.findByTrashed(domainReq)
                                .map(result -> ApiResponsePaginationMerchantDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Trashed merchants retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                result.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}