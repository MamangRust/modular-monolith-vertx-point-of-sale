package io.example.transaction.service;

import io.example.common.domain.PagedResult;
import io.example.transaction.domain.requests.transactions.FindAllTransactionByMerchantRequest;
import io.example.transaction.domain.requests.transactions.FindAllTransactionRequest;
import io.example.transaction.domain.response.TransactionResponse;
import io.example.transaction.domain.response.TransactionResponseDeleteAt;
import io.vertx.core.Future;

public interface TransactionQueryService {

        Future<PagedResult<TransactionResponse>> findAllTransaction(FindAllTransactionRequest req);

        Future<PagedResult<TransactionResponse>> findAllTransactionByMerchant(FindAllTransactionByMerchantRequest req);

        Future<TransactionResponse> findByIdTransaction(Long req);

        Future<PagedResult<TransactionResponseDeleteAt>> findByActiveTransaction(FindAllTransactionRequest req);

        Future<PagedResult<TransactionResponseDeleteAt>> findByTrashedTransaction(FindAllTransactionRequest req);
}
