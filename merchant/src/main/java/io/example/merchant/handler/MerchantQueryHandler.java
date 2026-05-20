package io.example.merchant.handler;

import io.example.merchant.domain.requests.FindAllMerchants;
import io.example.merchant.service.MerchantQueryService;
import io.vertx.core.Future;
import pb.merchant.Merchant.*;
import pb.merchant.MerchantQuery.*;
import pb.merchant.VertxMerchantQueryServiceGrpcServer;

import java.util.stream.Collectors;

public class MerchantQueryHandler implements VertxMerchantQueryServiceGrpcServer.MerchantQueryServiceApi {
    private final MerchantQueryService service;

    public MerchantQueryHandler(MerchantQueryService service) {
        this.service = service;
    }

    @Override
    public Future<ApiResponsePaginationMerchant> findAllMerchant(FindAllMerchantRequest req) {
        FindAllMerchants domainReq = new FindAllMerchants();
        domainReq.setSearch(req.getSearch());
        domainReq.setPage(req.getPage());
        domainReq.setPageSize(req.getPageSize());

        return service.getMerchants(domainReq)
                .map(result -> ApiResponsePaginationMerchant.newBuilder()
                        .setStatus("success")
                        .setMessage("Merchants retrieved")
                        .addAllData(result.getData().stream().map(ProtoConverter::toResponse).collect(Collectors.toList()))
                        .setPaginationMeta(ProtoConverter.toPaginationMeta(result, req.getPage(), req.getPageSize()))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponsePaginationMerchant.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchant> findByIdMerchant(FindByIdMerchantRequest req) {
        return service.getMerchantById(req.getMerchantId())
                .map(m -> ApiResponseMerchant.newBuilder()
                        .setStatus("success")
                        .setMessage("Merchant found")
                        .setData(ProtoConverter.toResponse(m))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchant.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponseMerchant> findByApiKey(FindByApiKeyRequest req) {
        return service.getMerchantByApiKey(req.getApiKey())
                .map(m -> ApiResponseMerchant.newBuilder()
                        .setStatus("success")
                        .setMessage("Merchant found")
                        .setData(ProtoConverter.toResponse(m))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponseMerchant.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponsesMerchant> findByMerchantUserId(FindByMerchantUserIdRequest req) {
        return service.getMerchantsByUserId(req.getUserId())
                .map(list -> ApiResponsesMerchant.newBuilder()
                        .setStatus("success")
                        .setMessage("Merchants found")
                        .addAllData(list.stream().map(ProtoConverter::toResponse).collect(Collectors.toList()))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponsesMerchant.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponsePaginationMerchantDeleteAt> findByActive(FindAllMerchantRequest req) {
        FindAllMerchants domainReq = new FindAllMerchants();
        domainReq.setSearch(req.getSearch());
        domainReq.setPage(req.getPage());
        domainReq.setPageSize(req.getPageSize());

        return service.getMerchantsActive(domainReq)
                .map(result -> ApiResponsePaginationMerchantDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Active merchants retrieved")
                        .addAllData(result.getData().stream().map(ProtoConverter::toResponseDeleteAt).collect(Collectors.toList()))
                        .setPaginationMeta(ProtoConverter.toPaginationMeta(result, req.getPage(), req.getPageSize()))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponsePaginationMerchantDeleteAt.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }

    @Override
    public Future<ApiResponsePaginationMerchantDeleteAt> findByTrashed(FindAllMerchantRequest req) {
        FindAllMerchants domainReq = new FindAllMerchants();
        domainReq.setSearch(req.getSearch());
        domainReq.setPage(req.getPage());
        domainReq.setPageSize(req.getPageSize());

        return service.getMerchantsTrashed(domainReq)
                .map(result -> ApiResponsePaginationMerchantDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Trashed merchants retrieved")
                        .addAllData(result.getData().stream().map(ProtoConverter::toResponseDeleteAt).collect(Collectors.toList()))
                        .setPaginationMeta(ProtoConverter.toPaginationMeta(result, req.getPage(), req.getPageSize()))
                        .build())
                .recover(err -> Future.succeededFuture(ApiResponsePaginationMerchantDeleteAt.newBuilder()
                        .setStatus("error")
                        .setMessage(err.getMessage())
                        .build()));
    }
}
