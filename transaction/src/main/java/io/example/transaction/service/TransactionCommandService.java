package io.example.transaction.service;

import io.example.transaction.domain.requests.transactions.CreateTransactionRequest;
import io.example.transaction.domain.requests.transactions.UpdateTransactionRequest;
import io.example.transaction.domain.response.TransactionResponse;
import io.example.transaction.domain.response.TransactionResponseDeleteAt;
import io.vertx.core.Future;

public interface TransactionCommandService {
    Future<TransactionResponse> createTransaction(CreateTransactionRequest req);

    Future<TransactionResponse> updateTransaction(UpdateTransactionRequest req);

    Future<TransactionResponseDeleteAt> trashTransaction(Long req);

    Future<TransactionResponseDeleteAt> restoreTransaction(Long req);

    Future<Void> deletePermanent(Long req);

    Future<Void> restoreAllTransactions();

    Future<Void> deleteAllPermanentTransactions();
}
