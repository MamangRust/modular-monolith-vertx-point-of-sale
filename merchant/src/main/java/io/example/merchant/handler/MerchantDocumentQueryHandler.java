package io.example.merchant.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.domain.requests.FindAllMerchantDocuments;
import io.example.merchant.service.MerchantDocumentQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.common.PaginationMeta;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;
import pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocument;
import pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt;
import pb.merchant_document.VertxMerchantDocumentQueryServiceGrpcServer;

@RequiredArgsConstructor
public class MerchantDocumentQueryHandler
                implements VertxMerchantDocumentQueryServiceGrpcServer.MerchantDocumentQueryServiceApi {
        private final MerchantDocumentQueryService service;

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
        public Future<ApiResponsePaginationMerchantDocument> findAll(FindAllMerchantDocumentsRequest req) {
                FindAllMerchantDocuments domainReq = FindAllMerchantDocuments.builder()
                                .search(req.getSearch())
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .build();

                return service.findAll(domainReq)
                                .map(result -> ApiResponsePaginationMerchantDocument.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Active documents retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toDocumentResponse)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                result.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationMerchantDocumentAt> findAllActive(FindAllMerchantDocumentsRequest req) {
                FindAllMerchantDocuments domainReq = FindAllMerchantDocuments.builder()
                                .search(req.getSearch())
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .build();

                return service.findByActive(domainReq)
                                .map(result -> ApiResponsePaginationMerchantDocumentAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Active documents retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toDocumentResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                result.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponsePaginationMerchantDocumentAt> findAllTrashed(FindAllMerchantDocumentsRequest req) {
                FindAllMerchantDocuments domainReq = FindAllMerchantDocuments.builder()
                                .search(req.getSearch())
                                .page(req.getPage())
                                .pageSize(req.getPageSize())
                                .build();

                return service.findByTrashed(domainReq)
                                .map(result -> ApiResponsePaginationMerchantDocumentAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Trashed documents retrieved successfully")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toDocumentResponseDeleteAt)
                                                                .toList())
                                                .setPagination(toMeta(req.getPage(), req.getPageSize(),
                                                                result.getTotalRecords()))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDocument> findById(FindMerchantDocumentByIdRequest req) {
                return service.findById((long) req.getDocumentId())
                                .map(doc -> ApiResponseMerchantDocument.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Document found successfully")
                                                .setData(ProtoConverter.toDocumentResponse(doc))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}