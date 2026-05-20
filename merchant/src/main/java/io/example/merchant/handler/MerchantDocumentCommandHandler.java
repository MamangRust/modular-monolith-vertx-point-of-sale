package io.example.merchant.handler;

import com.google.protobuf.Empty;
import io.example.merchant.service.MerchantDocumentCommandService;
import io.vertx.core.Future;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;
import pb.merchant_document.MerchantDocumentCommand.*;
import pb.merchant_document.VertxMerchantDocumentCommandServiceGrpcServer;

public class MerchantDocumentCommandHandler implements VertxMerchantDocumentCommandServiceGrpcServer.MerchantDocumentCommandServiceApi {
    private final MerchantDocumentCommandService service;

    public MerchantDocumentCommandHandler(MerchantDocumentCommandService service) {
        this.service = service;
    }

    @Override
    public Future<ApiResponseMerchantDocument> create(CreateMerchantDocumentRequest req) {
        io.example.merchant.domain.requests.CreateMerchantDocumentRequest domainReq = new io.example.merchant.domain.requests.CreateMerchantDocumentRequest();
        domainReq.setMerchantId(req.getMerchantId());
        domainReq.setDocumentType(req.getDocumentType());
        domainReq.setDocumentUrl(req.getDocumentUrl());

        return service.createMerchantDocument(domainReq)
                .map(doc -> ApiResponseMerchantDocument.newBuilder()
                        .setStatus("success")
                        .setMessage("Document created successfully")
                        .setData(ProtoConverter.toDocumentResponse(doc))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDocument.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantDocument> update(UpdateMerchantDocumentRequest req) {
        io.example.merchant.domain.requests.UpdateMerchantDocumentRequest domainReq = new io.example.merchant.domain.requests.UpdateMerchantDocumentRequest();
        domainReq.setDocumentId(req.getDocumentId());
        domainReq.setMerchantId(req.getMerchantId());
        domainReq.setDocumentType(req.getDocumentType());
        domainReq.setDocumentUrl(req.getDocumentUrl());
        domainReq.setNote(req.getNote());
        domainReq.setStatus(req.getStatus());

        return service.updateMerchantDocument(domainReq)
                .map(doc -> ApiResponseMerchantDocument.newBuilder()
                        .setStatus("success")
                        .setMessage("Document updated successfully")
                        .setData(ProtoConverter.toDocumentResponse(doc))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDocument.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantDocument> updateStatus(UpdateMerchantDocumentStatusRequest req) {
        io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest domainReq = new io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest();
        domainReq.setDocumentId(req.getDocumentId());
        domainReq.setMerchantId(req.getMerchantId());
        domainReq.setNote(req.getNote());
        domainReq.setStatus(req.getStatus());

        return service.updateMerchantDocumentStatus(domainReq)
                .map(doc -> ApiResponseMerchantDocument.newBuilder()
                        .setStatus("success")
                        .setMessage("Document status updated successfully")
                        .setData(ProtoConverter.toDocumentResponse(doc))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDocument.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantDocumentDeleteAt> trashed(FindMerchantDocumentByIdRequest req) {
        return service.trashMerchantDocument(req.getDocumentId())
                .map(doc -> ApiResponseMerchantDocumentDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Document trashed successfully")
                        .setData(ProtoConverter.toDocumentResponseDeleteAt(doc))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDocumentDeleteAt.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantDocumentDeleteAt> restore(FindMerchantDocumentByIdRequest req) {
        return service.restoreMerchantDocument(req.getDocumentId())
                .map(doc -> ApiResponseMerchantDocumentDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Document restored successfully")
                        .setData(ProtoConverter.toDocumentResponseDeleteAt(doc))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDocumentDeleteAt.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantDocumentDelete> deletePermanent(FindMerchantDocumentByIdRequest req) {
        return service.deleteMerchantDocumentPermanent(req.getDocumentId())
                .map(res -> ApiResponseMerchantDocumentDelete.newBuilder()
                        .setStatus("success")
                        .setMessage(res ? "Document permanently deleted" : "Document not found or already deleted")
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDocumentDelete.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantDocumentAll> restoreAll(Empty req) {
        return service.restoreAllMerchantDocument()
                .map(res -> ApiResponseMerchantDocumentAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All documents restored successfully")
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDocumentAll.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantDocumentAll> deleteAllPermanent(Empty req) {
        return service.deleteAllMerchantDocumentPermanent()
                .map(res -> ApiResponseMerchantDocumentAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All documents permanently deleted successfully")
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDocumentAll.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }
}
