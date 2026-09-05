package io.example.transaction.handler;

import com.google.protobuf.Empty;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.transaction.domain.requests.transactions.CreateTransactionRequest;
import io.example.transaction.domain.requests.transactions.UpdateTransactionRequest;
import io.example.transaction.service.TransactionCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.transaction.ApiResponseTransaction;
import pb.transaction.ApiResponseTransactionAll;
import pb.transaction.ApiResponseTransactionDelete;
import pb.transaction.ApiResponseTransactionDeleteAt;
import pb.transaction.FindByIdTransactionRequest;

@RequiredArgsConstructor
public class TransactionCommandHandler
        implements pb.transaction.VertxTransactionCommandServiceGrpcServer.TransactionCommandServiceApi {

    private final TransactionCommandService service;

    @Override
    public Future<ApiResponseTransaction> create(pb.transaction.CreateTransactionRequest req) {
        var domain = CreateTransactionRequest.builder()
                .orderID(req.getOrderId())
                .merchantId(req.getMerchantId())
                .paymentMethod(req.getPaymentMethod())
                .amount(req.getAmount())
                .paymentStatus(req.getPaymentStatus())
                .build();

        return service.createTransaction(domain)
                .map(resp -> ApiResponseTransaction.newBuilder()
                        .setStatus("success")
                        .setMessage("Transaction created successfully")
                        .setData(ProtoConverter.fromTransactionResponse(resp))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseTransaction> update(pb.transaction.UpdateTransactionRequest req) {
        var domain = UpdateTransactionRequest.builder()
                .transactionID(req.getTransactionId())
                .orderID(req.getOrderId())
                .merchantId(req.getMerchantId())
                .paymentMethod(req.getPaymentMethod())
                .amount(req.getAmount())
                .paymentStatus(req.getPaymentStatus())
                .build();

        return service.updateTransaction(domain)
                .map(resp -> ApiResponseTransaction.newBuilder()
                        .setStatus("success")
                        .setMessage("Transaction updated successfully")
                        .setData(ProtoConverter.fromTransactionResponse(resp))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseTransactionDeleteAt> trashedTransaction(FindByIdTransactionRequest req) {
        return service.trashTransaction((long) req.getTransactionId())
                .map(resp -> ApiResponseTransactionDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Transaction trashed successfully")
                        .setData(ProtoConverter.fromTransactionResponseDeleteAt(resp))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseTransactionDeleteAt> restoreTransaction(FindByIdTransactionRequest req) {
        return service.restoreTransaction((long) req.getTransactionId())
                .map(resp -> ApiResponseTransactionDeleteAt.newBuilder()
                        .setStatus("success")
                        .setMessage("Transaction restored successfully")
                        .setData(ProtoConverter.fromTransactionResponseDeleteAt(resp))
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseTransactionDelete> deleteTransactionPermanent(FindByIdTransactionRequest req) {
        return service.deletePermanent((long) req.getTransactionId())
                .map(resp -> ApiResponseTransactionDelete.newBuilder()
                        .setStatus("success")
                        .setMessage("Transaction permanently deleted successfully")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseTransactionAll> restoreAllTransaction(Empty req) {
        return service.restoreAllTransactions()
                .map(resp -> ApiResponseTransactionAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All transactions restored successfully")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }

    @Override
    public Future<ApiResponseTransactionAll> deleteAllTransactionPermanent(Empty req) {
        return service.deleteAllPermanentTransactions()
                .map(resp -> ApiResponseTransactionAll.newBuilder()
                        .setStatus("success")
                        .setMessage("All transactions permanently deleted successfully")
                        .build())
                .recover(GrpcExceptionMapper::toFailedFuture);
    }
}