package io.example.transaction.service;

import java.util.List;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.vertx.core.Future;
import pb.transaction.TransactionQuery.FindAllTransactionRequest;
import pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest;
import pb.transaction.Transaction.FindByIdTransactionRequest;
import pb.transaction.TransactionQuery.FindTransactionByMerchantIdRequest;

public interface TransactionQueryService {
    Future<ApiResponsePagination<List<TransactionResponse>>> findAllTransaction(FindAllTransactionRequest req);
    Future<ApiResponsePagination<List<TransactionResponse>>> findAllTransactionByCardNumber(FindAllTransactionCardNumberRequest req);
    Future<ApiResponse<TransactionResponse>> findByIdTransaction(FindByIdTransactionRequest req);
    Future<ApiResponse<List<TransactionResponse>>> findTransactionByMerchantId(FindTransactionByMerchantIdRequest req);
    Future<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByActiveTransaction(FindAllTransactionRequest req);
    Future<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByTrashedTransaction(FindAllTransactionRequest req);
}
