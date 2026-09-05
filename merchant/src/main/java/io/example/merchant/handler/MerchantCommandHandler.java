package io.example.merchant.handler;

import com.google.protobuf.Empty;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.merchant.domain.requests.CreateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantStatusRequest;
import io.example.merchant.service.MerchantCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.merchant.Merchant.ApiResponseMerchant;
import pb.merchant.Merchant.ApiResponseMerchantDeleteAt;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.MerchantCommand.ApiResponseMerchantAll;
import pb.merchant.MerchantCommand.ApiResponseMerchantDelete;

@RequiredArgsConstructor
public class MerchantCommandHandler
                implements pb.merchant.VertxMerchantCommandServiceGrpcServer.MerchantCommandServiceApi {
        private final MerchantCommandService service;

        @Override
        public Future<ApiResponseMerchant> createMerchant(pb.merchant.MerchantCommand.CreateMerchantRequest req) {
                CreateMerchantRequest domainReq = CreateMerchantRequest.builder()
                                .name(req.getName())
                                .userId(req.getUserId())
                                .build();

                return service.createMerchant(domainReq)
                                .map(m -> ApiResponseMerchant.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Merchant created successfully")
                                                .setData(ProtoConverter.toResponse(m))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchant> updateMerchant(pb.merchant.MerchantCommand.UpdateMerchantRequest req) {
                UpdateMerchantRequest domainReq = UpdateMerchantRequest.builder()
                                .merchantId(req.getMerchantId())
                                .name(req.getName())
                                .userId(req.getUserId())
                                .status(req.getStatus())
                                .build();

                return service.updateMerchant(domainReq)
                                .map(m -> ApiResponseMerchant.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Merchant updated successfully")
                                                .setData(ProtoConverter.toResponse(m))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchant> updateMerchantStatus(
                        pb.merchant.MerchantCommand.UpdateMerchantStatusRequest req) {
                UpdateMerchantStatusRequest domainReq = UpdateMerchantStatusRequest.builder()
                                .merchantId(req.getMerchantId())
                                .status(req.getStatus())
                                .build();

                return service.updateMerchantStatus(domainReq)
                                .map(m -> ApiResponseMerchant.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Merchant status updated successfully")
                                                .setData(ProtoConverter.toResponse(m))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDeleteAt> trashedMerchant(FindByIdMerchantRequest req) {
                return service.trashedMerchant((long) req.getMerchantId())
                                .map(m -> ApiResponseMerchantDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Merchant trashed successfully")
                                                .setData(ProtoConverter.toResponseDeleteAt(m))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDeleteAt> restoreMerchant(FindByIdMerchantRequest req) {
                return service.restoreMerchant((long) req.getMerchantId())
                                .map(m -> ApiResponseMerchantDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Merchant restored successfully")
                                                .setData(ProtoConverter.toResponseDeleteAt(m))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantDelete> deleteMerchantPermanent(FindByIdMerchantRequest req) {
                return service.deleteMerchantPermanent(Long.valueOf(req.getMerchantId()))
                                .<ApiResponseMerchantDelete>map(res -> ApiResponseMerchantDelete.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Merchant deleted successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantAll> restoreAllMerchant(Empty req) {
                return service.restoreAllMerchant()
                                .map(res -> ApiResponseMerchantAll.newBuilder()
                                                .setStatus("success")
                                                .setMessage("All merchants restored successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseMerchantAll> deleteAllMerchantPermanent(Empty req) {
                return service.deleteAllMerchantPermanent()
                                .map(res -> ApiResponseMerchantAll.newBuilder()
                                                .setStatus("success")
                                                .setMessage("All merchants permanently deleted successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}