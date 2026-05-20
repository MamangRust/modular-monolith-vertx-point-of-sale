package io.example.transaction.repository;

import io.example.transaction.model.Transaction;
import io.vertx.core.Future;

public interface TransactionCommandRepository {
    Future<Transaction> createTransaction(String cardNumber, Integer amount, String paymentMethod, Integer merchantId, String status);
    Future<Transaction> updateTransaction(Long transactionId, String cardNumber, Integer amount, String paymentMethod, Integer merchantId, String status);
    Future<Transaction> trashTransaction(Long transactionId);
    Future<Transaction> restoreTransaction(Long transactionId);
    Future<Void> deleteTransactionPermanently(Long transactionId);
    Future<Integer> restoreAllTransactions();
    Future<Integer> deleteAllPermanentTransactions();
}
