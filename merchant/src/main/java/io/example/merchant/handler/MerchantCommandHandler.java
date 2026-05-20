package io.example.merchant.handler;

import com.google.protobuf.Empty;
import io.example.merchant.service.MerchantCommandService;
import io.vertx.core.Future;
import pb.merchant.Merchant.*;
import pb.merchant.VertxMerchantCommandServiceGrpcServer;
import pb.merchant.MerchantCommand.*;

public class MerchantCommandHandler implements VertxMerchantCommandServiceGrpcServer.MerchantCommandServiceApi {
    private final MerchantCommandService service;

    public MerchantCommandHandler(MerchantCommandService service) {
        this.service = service;
    }

    @Override
    public Future<ApiResponseMerchant> createMerchant(CreateMerchantRequest req) {
        io.example.merchant.domain.requests.CreateMerchantRequest domainReq = new io.example.merchant.domain.requests.CreateMerchantRequest();
        domainReq.setName(req.getName());
        domainReq.setUserId(req.getUserId());

        return service.createMerchant(domainReq)
                .map(m -> ApiResponseMerchant.newBuilder()
                        .setStatus("success")
                        .setMessage("Merchant created successfully")
                        .setData(ProtoConverter.toResponse(m))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchant.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchant> updateMerchant(UpdateMerchantRequest req) {
        io.example.merchant.domain.requests.UpdateMerchantRequest domainReq = new io.example.merchant.domain.requests.UpdateMerchantRequest();
        domainReq.setMerchantId(req.getMerchantId());
        domainReq.setName(req.getName());
        domainReq.setUserId(req.getUserId());
        domainReq.setStatus(req.getStatus());

        return service.updateMerchant(domainReq)
                .map(m -> ApiResponseMerchant.newBuilder()
                        .setStatus("success")
                        .setMessage("Merchant updated successfully")
                        .setData(ProtoConverter.toResponse(m))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchant.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchant> updateMerchantStatus(UpdateMerchantStatusRequest req) {
        io.example.merchant.domain.requests.UpdateMerchantStatusRequest domainReq = new io.example.merchant.domain.requests.UpdateMerchantStatusRequest();
        domainReq.setMerchantId(req.getMerchantId());
        domainReq.setStatus(req.getStatus());

        return service.updateMerchantStatus(domainReq)
                .map(m -> ApiResponseMerchant.newBuilder()
                        .setStatus("success")
                        .setMessage("Merchant status updated successfully")
                        .setData(ProtoConverter.toResponse(m))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchant.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantDeleteAt> trashedMerchant(FindByIdMerchantRequest req) {
        return service.trashMerchant(req.getMerchantId())
                .map(m -> ApiResponseMerchantDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Merchant trashed successfully")
                        .setData(ProtoConverter.toResponseDeleteAt(m))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDeleteAt.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantDeleteAt> restoreMerchant(FindByIdMerchantRequest req) {
        return service.restoreMerchant(req.getMerchantId())
                .map(m -> ApiResponseMerchantDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Merchant restored successfully")
                        .setData(ProtoConverter.toResponseDeleteAt(m))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDeleteAt.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantDelete> deleteMerchantPermanent(FindByIdMerchantRequest req) {
        return service.deleteMerchantPermanent(req.getMerchantId())
                .map(res -> ApiResponseMerchantDelete.newBuilder()
                        .setStatus("success")
                        .setMessage(res ? "Merchant permanently deleted" : "Merchant not found or already deleted")
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantDelete.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantAll> restoreAllMerchant(Empty req) {
        return service.restoreAllMerchant()
                .map(res -> ApiResponseMerchantAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All merchants restored successfully")
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantAll.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchantAll> deleteAllMerchantPermanent(Empty req) {
        return service.deleteAllMerchantPermanent()
                .map(res -> ApiResponseMerchantAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All merchants permanently deleted successfully")
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchantAll.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }
}
