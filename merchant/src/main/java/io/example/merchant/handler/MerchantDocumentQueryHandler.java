package io.example.merchant.handler;

import java.util.stream.Collectors;

import io.example.merchant.domain.requests.FindAllMerchantDocuments;
import io.example.merchant.service.MerchantDocumentQueryService;
import io.vertx.core.Future;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;
import pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocument;
import pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt;
import pb.merchant_document.VertxMerchantDocumentQueryServiceGrpcServer;

public class MerchantDocumentQueryHandler
                implements VertxMerchantDocumentQueryServiceGrpcServer.MerchantDocumentQueryServiceApi {
        private final MerchantDocumentQueryService service;

        public MerchantDocumentQueryHandler(MerchantDocumentQueryService service) {
                this.service = service;
        }

        @Override
        public Future<ApiResponsePaginationMerchantDocument> findAll(FindAllMerchantDocumentsRequest req) {
                FindAllMerchantDocuments domainReq = new FindAllMerchantDocuments();
                domainReq.setSearch(req.getSearch());
                domainReq.setPage(req.getPage());
                domainReq.setPageSize(req.getPageSize());

                return service.getDocuments(domainReq)
                                .map(result -> ApiResponsePaginationMerchantDocument.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Documents retrieved")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toDocumentResponse)
                                                                .collect(Collectors.toList()))
                                                .setPaginationMeta(ProtoConverter.toPaginationMeta(result,
                                                                req.getPage(), req.getPageSize()))
                                                .build())
                                .recover(err -> Future
                                                .succeededFuture(ApiResponsePaginationMerchantDocument.newBuilder()
                                                                .setStatus("error")
                                                                .setMessage(err.getMessage())
                                                                .build()));
        }

        @Override
        public Future<ApiResponsePaginationMerchantDocumentAt> findAllActive(FindAllMerchantDocumentsRequest req) {
                FindAllMerchantDocuments domainReq = new FindAllMerchantDocuments();
                domainReq.setSearch(req.getSearch());
                domainReq.setPage(req.getPage());
                domainReq.setPageSize(req.getPageSize());

                return service.getDocumentsActive(domainReq)
                                .map(result -> ApiResponsePaginationMerchantDocumentAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Active documents retrieved")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toDocumentResponseDeleteAt)
                                                                .collect(Collectors.toList()))
                                                .setPaginationMeta(ProtoConverter.toPaginationMeta(result,
                                                                req.getPage(), req.getPageSize()))
                                                .build())
                                .recover(err -> Future
                                                .succeededFuture(ApiResponsePaginationMerchantDocumentAt.newBuilder()
                                                                .setStatus("error")
                                                                .setMessage(err.getMessage())
                                                                .build()));
        }

        @Override
        public Future<ApiResponsePaginationMerchantDocumentAt> findAllTrashed(FindAllMerchantDocumentsRequest req) {
                FindAllMerchantDocuments domainReq = new FindAllMerchantDocuments();
                domainReq.setSearch(req.getSearch());
                domainReq.setPage(req.getPage());
                domainReq.setPageSize(req.getPageSize());

                return service.getDocumentsTrashed(domainReq)
                                .map(result -> ApiResponsePaginationMerchantDocumentAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Trashed documents retrieved")
                                                .addAllData(result.getData().stream()
                                                                .map(ProtoConverter::toDocumentResponseDeleteAt)
                                                                .collect(Collectors.toList()))
                                                .setPaginationMeta(ProtoConverter.toPaginationMeta(result,
                                                                req.getPage(), req.getPageSize()))
                                                .build())
                                .recover(err -> Future
                                                .succeededFuture(ApiResponsePaginationMerchantDocumentAt.newBuilder()
                                                                .setStatus("error")
                                                                .setMessage(err.getMessage())
                                                                .build()));
        }

        @Override
        public Future<ApiResponseMerchantDocument> findById(FindMerchantDocumentByIdRequest req) {
                return service.getDocumentById(req.getDocumentId())
                                .map(doc -> ApiResponseMerchantDocument.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Document found")
                                                .setData(ProtoConverter.toDocumentResponse(doc))
                                                .build())
                                .recover(err -> Future.succeededFuture(ApiResponseMerchantDocument.newBuilder()
                                                .setStatus("error")
                                                .setMessage(err.getMessage())
                                                .build()));
        }
}
