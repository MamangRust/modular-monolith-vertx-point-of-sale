package io.example.cashier.handler;

import io.example.cashier.service.CashierCommandService;
import io.vertx.core.Future;
import pb.cashier.Cashier.ApiResponseCashier;
import pb.cashier.Cashier.ApiResponseCashierDeleteAt;
import pb.cashier.Cashier.CreateCashierRequest;
import pb.cashier.Cashier.FindByIdCashierRequest;
import pb.cashier.Cashier.UpdateCashierRequest;
import pb.cashier.CashierCommand.ApiResponseCashierAll;
import pb.cashier.CashierCommand.ApiResponseCashierDelete;

public class CashierCommandHandler implements pb.cashier.VertxCashierCommandServiceGrpcServer.CashierCommandServiceApi {
        private final CashierCommandService commandService;

        public CashierCommandHandler(CashierCommandService commandService) {
                this.commandService = commandService;
        }

        @Override
        public Future<ApiResponseCashier> createCashier(CreateCashierRequest request) {
                return commandService
                                .createCashier((long) request.getMerchantId(), (long) request.getUserId(),
                                                request.getName())
                                .map(cashier -> ApiResponseCashier.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier created successfully")
                                                .setData(ProtoConverter.toCashierResponse(cashier))
                                                .build());
        }

        @Override
        public Future<ApiResponseCashier> updateCashier(UpdateCashierRequest request) {
                return commandService.updateCashier((long) request.getCashierId(), request.getName())
                                .map(cashier -> ApiResponseCashier.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier updated successfully")
                                                .setData(ProtoConverter.toCashierResponse(cashier))
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierDeleteAt> trashedCashier(FindByIdCashierRequest request) {
                return commandService.trashCashier((long) request.getId())
                                .map(cashier -> ApiResponseCashierDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier trashed successfully")
                                                .setData(ProtoConverter.toCashierResponseDeleteAt(cashier))
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierDeleteAt> restoreCashier(FindByIdCashierRequest request) {
                return commandService.restoreCashier((long) request.getId())
                                .map(cashier -> ApiResponseCashierDeleteAt.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier restored successfully")
                                                .setData(ProtoConverter.toCashierResponseDeleteAt(cashier))
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierDelete> deleteCashierPermanent(FindByIdCashierRequest request) {
                return commandService.deleteCashierPermanent((long) request.getId())
                                .map(deleted -> ApiResponseCashierDelete.newBuilder()
                                                .setStatus("success")
                                                .setMessage("Cashier permanently deleted successfully")
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierAll> restoreAllCashier(com.google.protobuf.Empty request) {
                return commandService.restoreAllCashier()
                                .map(restored -> ApiResponseCashierAll.newBuilder()
                                                .setStatus("success")
                                                .setMessage("All cashiers restored successfully")
                                                .build());
        }

        @Override
        public Future<ApiResponseCashierAll> deleteAllCashierPermanent(com.google.protobuf.Empty request) {
                return commandService.deleteAllCashierPermanent()
                                .map(deleted -> ApiResponseCashierAll.newBuilder()
                                                .setStatus("success")
                                                .setMessage("All cashiers permanently deleted successfully")
                                                .build());
        }
}
