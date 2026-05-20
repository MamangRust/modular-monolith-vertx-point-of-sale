package io.example.transaction.handler;

import io.example.transaction.service.TransactionQueryService;
import io.vertx.core.Future;
import pb.transaction.Transaction.ApiResponseTransaction;
import pb.transaction.Transaction.ApiResponseTransactions;
import pb.transaction.Transaction.FindByIdTransactionRequest;
import pb.transaction.TransactionQuery.*;

public class TransactionQueryHandler implements pb.transaction.VertxTransactionQueryServiceGrpcServer.TransactionQueryServiceApi {
    private final TransactionQueryService service;

    public TransactionQueryHandler(TransactionQueryService service) {
        this.service = service;
    }

    private pb.common.PaginationMeta toMeta(io.example.common.model.PaginationMeta meta) {
        if (meta == null) return pb.common.PaginationMeta.getDefaultInstance();
        return pb.common.PaginationMeta.newBuilder()
                .setCurrentPage(meta.currentPage())
                .setPageSize(meta.pageSize())
                .setTotalPages(meta.totalPages())
                .setTotalRecords(meta.totalRecords())
                .build();
    }

    @Override
    public Future<ApiResponsePaginationTransaction> findAllTransaction(FindAllTransactionRequest req) {
        return service.findAllTransaction(req)
                .map(resp -> ApiResponsePaginationTransaction.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromTransactionResponse).toList())
                        .setPaginationMeta(toMeta(resp.pagination()))
                        .build());
    }

    @Override
    public Future<ApiResponsePaginationTransaction> findAllTransactionByCardNumber(FindAllTransactionCardNumberRequest req) {
        return service.findAllTransactionByCardNumber(req)
                .map(resp -> ApiResponsePaginationTransaction.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromTransactionResponse).toList())
                        .setPaginationMeta(toMeta(resp.pagination()))
                        .build());
    }

    @Override
    public Future<ApiResponseTransaction> findByIdTransaction(FindByIdTransactionRequest req) {
        return service.findByIdTransaction(req)
                .map(resp -> {
                    var builder = ApiResponseTransaction.newBuilder()
                            .setStatus(resp.status())
                            .setMessage(resp.message());
                    if (resp.data() != null) {
                        builder.setData(ProtoConverter.fromTransactionResponse(resp.data()));
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<ApiResponseTransactions> findTransactionByMerchantId(FindTransactionByMerchantIdRequest req) {
        return service.findTransactionByMerchantId(req)
                .map(resp -> {
                    var builder = ApiResponseTransactions.newBuilder()
                            .setStatus(resp.status())
                            .setMessage(resp.message());
                    if (resp.data() != null) {
                        builder.addAllData(resp.data().stream().map(ProtoConverter::fromTransactionResponse).toList());
                    }
                    return builder.build();
                });
    }

    @Override
    public Future<ApiResponsePaginationTransactionDeleteAt> findByActiveTransaction(FindAllTransactionRequest req) {
        return service.findByActiveTransaction(req)
                .map(resp -> ApiResponsePaginationTransactionDeleteAt.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromTransactionResponseDeleteAt).toList())
                        .setPaginationMeta(toMeta(resp.pagination()))
                        .build());
    }

    @Override
    public Future<ApiResponsePaginationTransactionDeleteAt> findByTrashedTransaction(FindAllTransactionRequest req) {
        return service.findByTrashedTransaction(req)
                .map(resp -> ApiResponsePaginationTransactionDeleteAt.newBuilder()
                        .setStatus(resp.status())
                        .setMessage(resp.message())
                        .addAllData(resp.data().stream().map(ProtoConverter::fromTransactionResponseDeleteAt).toList())
                        .setPaginationMeta(toMeta(resp.pagination()))
                        .build());
    }
}
