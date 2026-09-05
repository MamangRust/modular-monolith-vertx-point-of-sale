package io.example.apigateway.handler;

import static io.example.apigateway.utils.GrpcGatewayUtils.sendResponse;

import io.example.apigateway.utils.GrpcGatewayUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.merchant.Merchant;
import pb.merchant.MerchantCommand;
import pb.merchant.VertxMerchantCommandServiceGrpcClient;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;
import pb.merchant_document.MerchantDocumentCommand;
import pb.merchant_document.MerchantDocumentOuterClass;
import pb.merchant_document.VertxMerchantDocumentCommandServiceGrpcClient;
import pb.merchant_document.VertxMerchantDocumentQueryServiceGrpcClient;

@Slf4j
@RequiredArgsConstructor
public class MerchantProxyHandler {
        private final VertxMerchantQueryServiceGrpcClient queryClient;
        private final VertxMerchantCommandServiceGrpcClient commandClient;
        private final VertxMerchantDocumentCommandServiceGrpcClient docCommandClient;
        private final VertxMerchantDocumentQueryServiceGrpcClient docQueryClient;

        // =========================================================================
        // QUERIES
        // =========================================================================

        public void getAllMerchants(RoutingContext ctx) {
                var req = Merchant.FindAllMerchantRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findAllMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getActiveMerchants(RoutingContext ctx) {
                var req = Merchant.FindAllMerchantRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findByActive(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getTrashedMerchants(RoutingContext ctx) {
                var req = Merchant.FindAllMerchantRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                queryClient.findByTrashed(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getMerchantById(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = Merchant.FindByIdMerchantRequest.newBuilder().setMerchantId(id).build();
                queryClient.findByIdMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        // =========================================================================
        // COMMANDS (LIFECYCLE)
        // =========================================================================

        public void createMerchant(RoutingContext ctx) {
                JsonObject body = ctx.body().asJsonObject();
                var req = MerchantCommand.CreateMerchantRequest.newBuilder()
                                .setUserId(GrpcGatewayUtils.getJsonInteger(body, "user_id", 0))
                                .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
                                .build();
                commandClient.createMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 201))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void updateMerchant(RoutingContext ctx) {
                JsonObject body = ctx.body().asJsonObject();
                var req = MerchantCommand.UpdateMerchantRequest.newBuilder()
                                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                                .setUserId(GrpcGatewayUtils.getJsonInteger(body, "user_id", 0))
                                .setName(GrpcGatewayUtils.getJsonString(body, "name", ""))
                                .setStatus(GrpcGatewayUtils.getJsonString(body, "status", ""))
                                .build();
                commandClient.updateMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void updateMerchantStatus(RoutingContext ctx) {
                JsonObject body = ctx.body().asJsonObject();
                var req = MerchantCommand.UpdateMerchantStatusRequest.newBuilder()
                                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                                .setStatus(GrpcGatewayUtils.getJsonString(body, "status", ""))
                                .build();
                commandClient.updateMerchantStatus(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void trashMerchant(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = Merchant.FindByIdMerchantRequest.newBuilder().setMerchantId(id).build();
                commandClient.trashedMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void restoreMerchant(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = Merchant.FindByIdMerchantRequest.newBuilder().setMerchantId(id).build();
                commandClient.restoreMerchant(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void deleteMerchantPermanently(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "merchantId");
                var req = Merchant.FindByIdMerchantRequest.newBuilder().setMerchantId(id).build();
                commandClient.deleteMerchantPermanent(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void restoreAllMerchants(RoutingContext ctx) {
                commandClient.restoreAllMerchant(com.google.protobuf.Empty.getDefaultInstance())
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void deleteAllPermanentMerchants(RoutingContext ctx) {
                commandClient.deleteAllMerchantPermanent(com.google.protobuf.Empty.getDefaultInstance())
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        // =========================================================================
        // MERCHANT DOCUMENTS — QUERIES
        // =========================================================================

        public void getAllMerchantDocuments(RoutingContext ctx) {
                var req = MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                docQueryClient.findAll(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getActiveMerchantDocuments(RoutingContext ctx) {
                var req = MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                docQueryClient.findAllActive(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getTrashedMerchantDocuments(RoutingContext ctx) {
                var req = MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.newBuilder()
                                .setSearch(GrpcGatewayUtils.getQueryString(ctx, "search", ""))
                                .setPage(GrpcGatewayUtils.getQueryInt(ctx, "page", 1))
                                .setPageSize(GrpcGatewayUtils.getQueryInt(ctx, "pageSize", 10))
                                .build();
                docQueryClient.findAllTrashed(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void getMerchantDocumentById(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "documentId");
                var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                                .setDocumentId(id)
                                .build();
                docQueryClient.findById(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        // =========================================================================
        // MERCHANT DOCUMENTS — COMMANDS
        // =========================================================================

        public void createMerchantDocument(RoutingContext ctx) {
                JsonObject body = ctx.body().asJsonObject();
                var req = MerchantDocumentCommand.CreateMerchantDocumentRequest.newBuilder()
                                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                                .setDocumentType(GrpcGatewayUtils.getJsonString(body, "document_type", ""))
                                .setDocumentUrl(GrpcGatewayUtils.getJsonString(body, "document_url", ""))
                                .build();
                docCommandClient.create(req)
                                .onSuccess(r -> sendResponse(ctx, r, 201))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void updateMerchantDocument(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "documentId");
                JsonObject body = ctx.body().asJsonObject();
                var req = MerchantDocumentCommand.UpdateMerchantDocumentRequest.newBuilder()
                                .setDocumentId(id)
                                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                                .setDocumentType(GrpcGatewayUtils.getJsonString(body, "document_type", ""))
                                .setDocumentUrl(GrpcGatewayUtils.getJsonString(body, "document_url", ""))
                                .build();
                docCommandClient.update(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void updateMerchantDocumentStatus(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "documentId");
                JsonObject body = ctx.body().asJsonObject();
                var req = MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest.newBuilder()
                                .setDocumentId(id)
                                .setMerchantId(GrpcGatewayUtils.getJsonInteger(body, "merchant_id", 0))
                                .setStatus(GrpcGatewayUtils.getJsonString(body, "status", ""))
                                .setNote(GrpcGatewayUtils.getJsonString(body, "note", ""))
                                .build();
                docCommandClient.updateStatus(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void trashMerchantDocument(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "documentId");
                var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                                .setDocumentId(id)
                                .build();
                docCommandClient.trashed(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void restoreMerchantDocument(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "documentId");
                var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                                .setDocumentId(id)
                                .build();
                docCommandClient.restore(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void deleteMerchantDocumentPermanently(RoutingContext ctx) {
                int id = GrpcGatewayUtils.getSafePathInt(ctx, "documentId");
                var req = MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest.newBuilder()
                                .setDocumentId(id)
                                .build();
                docCommandClient.deletePermanent(req)
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void restoreAllMerchantDocuments(RoutingContext ctx) {
                docCommandClient.restoreAll(com.google.protobuf.Empty.getDefaultInstance())
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }

        public void deleteAllPermanentMerchantDocuments(RoutingContext ctx) {
                docCommandClient.deleteAllPermanent(com.google.protobuf.Empty.getDefaultInstance())
                                .onSuccess(r -> sendResponse(ctx, r, 200))
                                .onFailure(err -> GrpcGatewayUtils.handleError(ctx, err));
        }
}