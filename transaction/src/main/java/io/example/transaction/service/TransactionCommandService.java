package io.example.transaction.service;

import io.example.common.model.ApiResponse;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.vertx.core.Future;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;
import pb.transaction.Transaction.FindByIdTransactionRequest;

public interface TransactionCommandService {
    Future<ApiResponse<TransactionResponse>> createTransaction(CreateTransactionRequest req);
    Future<ApiResponse<TransactionResponse>> updateTransaction(UpdateTransactionRequest req);
    Future<ApiResponse<TransactionResponseDeleteAt>> trashTransaction(FindByIdTransactionRequest req);
    Future<ApiResponse<TransactionResponseDeleteAt>> restoreTransaction(FindByIdTransactionRequest req);
    Future<ApiResponse<Void>> deletePermanent(FindByIdTransactionRequest req);
    Future<ApiResponse<Void>> restoreAllTransactions();
    Future<ApiResponse<Void>> deleteAllPermanentTransactions();
}
