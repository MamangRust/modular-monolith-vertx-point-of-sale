package io.example.cashier.handler;

import com.google.protobuf.Empty;

import io.example.cashier.domain.requests.cashier.CreateCashierRequest;
import io.example.cashier.domain.requests.cashier.UpdateCashierRequest;
import io.example.cashier.service.CashierCommandService;
import io.example.common.grpc.GrpcExceptionMapper;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.cashier.Cashier.ApiResponseCashier;
import pb.cashier.Cashier.ApiResponseCashierDeleteAt;
import pb.cashier.Cashier.FindByIdCashierRequest;
import pb.cashier.CashierCommand.ApiResponseCashierAll;
import pb.cashier.CashierCommand.ApiResponseCashierDelete;

@RequiredArgsConstructor
public class CashierCommandHandler implements pb.cashier.VertxCashierCommandServiceGrpcServer.CashierCommandServiceApi {
        private final CashierCommandService commandService;

        @Override
        public Future<ApiResponseCashier> createCashier(pb.cashier.Cashier.CreateCashierRequest request) {
                CreateCashierRequest domainReq = CreateCashierRequest.builder()
                                .merchantId(request.getMerchantId())
                                .userId(request.getUserId())
                                .name(request.getName())
                                .build();

                return commandService.createCashier(domainReq)
                                .map(cashier -> ApiResponseCashier.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier created successfully")
                                                .setData(ProtoConverter.toCashierResponse(cashier))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashier> updateCashier(pb.cashier.Cashier.UpdateCashierRequest request) {
                UpdateCashierRequest domainReq = UpdateCashierRequest.builder()
                                .cashierId(request.getCashierId())
                                .name(request.getName())
                                .build();

                return commandService.updateCashier(domainReq)
                                .map(cashier -> ApiResponseCashier.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier updated successfully")
                                                .setData(ProtoConverter.toCashierResponse(cashier))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierDeleteAt> trashedCashier(FindByIdCashierRequest request) {
                return commandService.trashCashier((long) request.getId())
                                .map(cashier -> ApiResponseCashierDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier trashed successfully")
                                                .setData(ProtoConverter.toCashierResponseDeleteAt(cashier))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierDeleteAt> restoreCashier(FindByIdCashierRequest request) {
                return commandService.restoreCashier((long) request.getId())
                                .map(cashier -> ApiResponseCashierDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier restored successfully")
                                                .setData(ProtoConverter.toCashierResponseDeleteAt(cashier))
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierDelete> deleteCashierPermanent(FindByIdCashierRequest request) {
                return commandService.deleteCashierPermanent((long) request.getId())
                                .map(deleted -> ApiResponseCashierDelete.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier permanently deleted successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierAll> restoreAllCashier(Empty request) {
                return commandService.restoreAllCashier()
                                .map(v -> ApiResponseCashierAll.newBuilder()
                                                .setStatus("success")
                                                .setMessage("All cashiers restored successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }

        @Override
        public Future<ApiResponseCashierAll> deleteAllCashierPermanent(Empty request) {
                return commandService.deleteAllCashierPermanent()
                                .map(v -> ApiResponseCashierAll.newBuilder()
                                                .setStatus("success")
                                                .setMessage("All cashiers permanently deleted successfully")
                                                .build())
                                .recover(GrpcExceptionMapper::toFailedFuture);
        }
}