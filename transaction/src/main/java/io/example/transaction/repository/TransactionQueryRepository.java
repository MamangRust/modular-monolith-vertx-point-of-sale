package io.example.transaction.repository;

import io.example.common.domain.PagedResult;
import io.example.transaction.domain.requests.transactions.FindAllTransactionByMerchantRequest;
import io.example.transaction.domain.requests.transactions.FindAllTransactionRequest;
import io.example.transaction.model.Transaction;
import io.vertx.core.Future;

public interface TransactionQueryRepository {
    public Future<PagedResult<Transaction>> getTransactions(FindAllTransactionRequest req);

    Future<PagedResult<Transaction>> getTransactionsActive(FindAllTransactionRequest req);

    Future<PagedResult<Transaction>> getTransactionsTrashed(FindAllTransactionRequest req);

    Future<PagedResult<Transaction>> getTransactionByMerchant(FindAllTransactionByMerchantRequest req);

    Future<Transaction> getTransactionById(Long transactionId);

    Future<Transaction> getTransactionByOrderId(Long orderId);

    Future<Transaction> findByTrashedId(Long transactionId);
}
