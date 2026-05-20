package io.example.transaction.repository;

import java.util.List;
import io.example.common.domain.PagedResult;
import io.example.transaction.model.Transaction;
import io.vertx.core.Future;

public interface TransactionQueryRepository {
    Future<PagedResult<Transaction>> getTransactions(String search, int page, int pageSize);
    Future<PagedResult<Transaction>> getTransactionsActive(String search, int page, int pageSize);
    Future<PagedResult<Transaction>> getTransactionsTrashed(String search, int page, int pageSize);
    Future<PagedResult<Transaction>> getTransactionsByCardNumber(String cardNumber, String search, int page, int pageSize);
    Future<List<Transaction>> getTransactionsByMerchantId(Integer merchantId);
    Future<Transaction> getTransactionById(Long transactionId);
}
