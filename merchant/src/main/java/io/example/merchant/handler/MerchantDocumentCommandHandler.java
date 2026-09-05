package io.example.merchant.handler;

import com.google.protobuf.Empty;
import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.domain.requests.CreateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest;
import io.example.merchant.service.MerchantDocumentCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;
import pb.merchant_document.MerchantDocumentCommand.*;
import pb.merchant_document.VertxMerchantDocumentCommandServiceGrpcServer;

@RequiredArgsConstructor
public class MerchantDocumentCommandHandler
                implements VertxMerchantDocumentCommandServiceGrpcServer.MerchantDocumentCommandServiceApi {
        private final MerchantDocumentCommandService service;

        @Override
        public Future<ApiResponseMerchantDocument> create(
                        pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest req) {
                CreateMerchantDocumentRequest domainReq = CreateMerchantDocumentRequest.builder()
                                .merchantId(req.getMerchantId())
                                .documentType(req.getDocumentType())
                                .documentUrl(req.getDocumentUrl())
                                .note(req.getNote())
                                .build();

                return service.createMerchantDocument(domainReq)
                                .map(doc -> ApiResponseMerchantDocument.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Document created successfully")
                                                .setData(ProtoConverter.toDocumentResponse(doc))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDocument> update(
                        pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest req) {
                UpdateMerchantDocumentRequest domainReq = UpdateMerchantDocumentRequest.builder()
                                .documentId(req.getDocumentId())
                                .merchantId(req.getMerchantId())
                                .documentType(req.getDocumentType())
                                .documentUrl(req.getDocumentUrl())
                                .note(req.getNote())
                                .status(req.getStatus())
                                .build();

                return service.updateMerchantDocument(domainReq)
                                .map(doc -> ApiResponseMerchantDocument.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Document updated successfully")
                                                .setData(ProtoConverter.toDocumentResponse(doc))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDocument> updateStatus(
                        pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest req) {
                UpdateMerchantDocumentStatusRequest domainReq = UpdateMerchantDocumentStatusRequest.builder()
                                .documentId(req.getDocumentId())
                                .merchantId(req.getMerchantId())
                                .note(req.getNote())
                                .status(req.getStatus())
                                .build();

                return service.updateMerchantDocumentStatus(domainReq)
                                .map(doc -> ApiResponseMerchantDocument.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Document status updated successfully")
                                                .setData(ProtoConverter.toDocumentResponse(doc))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDocumentDeleteAt> trashed(FindMerchantDocumentByIdRequest req) {
                return service.trashedMerchantDocument((long) req.getDocumentId())
                                .map(doc -> ApiResponseMerchantDocumentDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Document trashed successfully")
                                                .setData(ProtoConverter.toDocumentResponseDeleteAt(doc))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDocumentDeleteAt> restore(FindMerchantDocumentByIdRequest req) {
                return service.restoreMerchantDocument((long) req.getDocumentId())
                                .map(doc -> ApiResponseMerchantDocumentDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Document restored successfully")
                                                .setData(ProtoConverter.toDocumentResponseDeleteAt(doc))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDocumentDelete> deletePermanent(FindMerchantDocumentByIdRequest req) {
                return service.deleteMerchantDocumentPermanent((long) req.getDocumentId())
                                .map(res -> ApiResponseMerchantDocumentDelete.newBuilder()
                                                .setStatus("success")
                                                .setMessage(res ? "Document permanently deleted"
                                                                : "Document not found or already deleted")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDocumentAll> restoreAll(Empty req) {
                return service.restoreAllMerchantDocument()
                                .map(res -> ApiResponseMerchantDocumentAll.newBuilder()
                                                .setStatus("success")
                                                .setMessage("All documents restored successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDocumentAll> deleteAllPermanent(Empty req) {
                return service.deleteAllMerchantDocumentPermanent()
                                .map(res -> ApiResponseMerchantDocumentAll.newBuilder()
                                                .setStatus("success")
                                                .setMessage("All documents permanently deleted successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}