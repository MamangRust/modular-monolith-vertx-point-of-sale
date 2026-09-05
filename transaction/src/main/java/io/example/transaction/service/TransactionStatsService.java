package io.example.transaction.service;

import java.util.List;

import io.example.transaction.domain.requests.transactions.MonthAmountTransactionMerchant;
import io.example.transaction.domain.requests.transactions.MonthAmountTransactionRequest;
import io.example.transaction.domain.requests.transactions.MonthMethodTransactionMerchantRequest;
import io.example.transaction.domain.requests.transactions.MonthMethodTransactionRequest;
import io.example.transaction.domain.requests.transactions.YearAmountTransactionMerchant;
import io.example.transaction.domain.requests.transactions.YearMethodTransactionMerchantRequest;
import io.example.transaction.domain.response.TransactionMonthlyAmountFailedResponse;
import io.example.transaction.domain.response.TransactionMonthlyAmountSuccessResponse;
import io.example.transaction.domain.response.TransactionMonthlyMethodResponse;
import io.example.transaction.domain.response.TransactionYearlyAmountFailedResponse;
import io.example.transaction.domain.response.TransactionYearlyAmountSuccessResponse;
import io.example.transaction.domain.response.TransactionYearlyMethodResponse;
import io.vertx.core.Future;

public interface TransactionStatsService {
        Future<List<TransactionMonthlyAmountSuccessResponse>> findMonthlyTransactionStatusSuccess(
                        MonthAmountTransactionRequest req);

        Future<List<TransactionYearlyAmountSuccessResponse>> findYearlyTransactionStatusSuccess(int year);

        Future<List<TransactionMonthlyAmountFailedResponse>> findMonthlyTransactionStatusFailed(
                        MonthAmountTransactionRequest req);

        Future<List<TransactionYearlyAmountFailedResponse>> findYearlyTransactionStatusFailed(int year);

        Future<List<TransactionMonthlyAmountSuccessResponse>> findMonthlyTransactionStatusSuccessByMerchant(
                        MonthAmountTransactionMerchant req);

        Future<List<TransactionYearlyAmountSuccessResponse>> findYearlyTransactionStatusSuccessByMerchant(
                        YearAmountTransactionMerchant req);

        Future<List<TransactionMonthlyAmountFailedResponse>> findMonthlyTransactionStatusFailedByMerchant(
                        MonthAmountTransactionMerchant req);

        Future<List<TransactionYearlyAmountFailedResponse>> findYearlyTransactionStatusFailedByMerchant(
                        YearAmountTransactionMerchant req);

        Future<List<TransactionMonthlyMethodResponse>> findMonthlyPaymentMethodsSuccess(
                        MonthMethodTransactionRequest req);

        Future<List<TransactionYearlyMethodResponse>> findYearlyPaymentMethodsSuccess(int year);

        Future<List<TransactionMonthlyMethodResponse>> findMonthlyPaymentMethodsFailed(
                        MonthMethodTransactionRequest req);

        Future<List<TransactionYearlyMethodResponse>> findYearlyPaymentMethodsFailed(int year);

        Future<List<TransactionMonthlyMethodResponse>> findMonthlyPaymentMethodsByMerchantSuccess(
                        MonthMethodTransactionMerchantRequest req);

        Future<List<TransactionYearlyMethodResponse>> findYearlyPaymentMethodsByMerchantSuccess(
                        YearMethodTransactionMerchantRequest req);

        Future<List<TransactionMonthlyMethodResponse>> findMonthlyPaymentMethodsByMerchantFailed(
                        MonthMethodTransactionMerchantRequest req);

        Future<List<TransactionYearlyMethodResponse>> findYearlyPaymentMethodsByMerchantFailed(
                        YearMethodTransactionMerchantRequest req);
}