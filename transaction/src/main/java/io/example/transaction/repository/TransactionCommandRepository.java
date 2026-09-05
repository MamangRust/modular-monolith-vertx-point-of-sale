package io.example.transaction.repository;

import io.example.transaction.domain.requests.transactions.CreateTransactionRequest;
import io.example.transaction.domain.requests.transactions.UpdateTransactionRequest;
import io.example.transaction.model.Transaction;
import io.vertx.core.Future;

public interface TransactionCommandRepository {
    Future<Transaction> createTransaction(CreateTransactionRequest req);

    Future<Transaction> updateTransaction(UpdateTransactionRequest req);

    Future<Transaction> trashTransaction(Long transactionId);

    Future<Transaction> restoreTransaction(Long transactionId);

    Future<Boolean> deleteTransactionPermanently(Long transactionId);

    Future<Integer> restoreAllTransactions();

    Future<Integer> deleteAllPermanentTransactions();
}
