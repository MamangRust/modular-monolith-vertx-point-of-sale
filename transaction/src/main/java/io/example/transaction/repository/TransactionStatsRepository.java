package io.example.transaction.repository;

import java.util.List;

import io.example.transaction.domain.requests.transactions.MonthAmountTransactionMerchant;
import io.example.transaction.domain.requests.transactions.MonthAmountTransactionRequest;
import io.example.transaction.domain.requests.transactions.MonthMethodTransactionMerchantRequest;
import io.example.transaction.domain.requests.transactions.MonthMethodTransactionRequest;
import io.example.transaction.domain.requests.transactions.YearAmountTransactionMerchant;
import io.example.transaction.domain.requests.transactions.YearMethodTransactionMerchantRequest;
import io.example.transaction.model.TransactionMonthlyAmountFailed;
import io.example.transaction.model.TransactionMonthlyAmountSuccess;
import io.example.transaction.model.TransactionMonthlyMethod;
import io.example.transaction.model.TransactionYearMethod;
import io.example.transaction.model.TransactionYearlyAmountFailed;
import io.example.transaction.model.TransactionYearlyAmountSuccess;
import io.vertx.core.Future;

public interface TransactionStatsRepository {
    Future<List<TransactionMonthlyAmountSuccess>> getMonthlyAmountTransactionSuccess(MonthAmountTransactionRequest req);

    Future<List<TransactionYearlyAmountSuccess>> getYearlyAmountTransactionSuccess(Integer year);

    Future<List<TransactionMonthlyAmountFailed>> getMonthlyAmountTransactionFailed(MonthAmountTransactionRequest req);

    Future<List<TransactionYearlyAmountFailed>> getYearlyAmountTransactionFailed(Integer year);

    Future<List<TransactionMonthlyMethod>> getMonthlyTransactionMethodsSuccess(MonthMethodTransactionRequest req);

    Future<List<TransactionMonthlyMethod>> getMonthlyTransactionMethodsFailed(MonthMethodTransactionRequest req);

    Future<List<TransactionYearMethod>> getYearlyTransactionMethodsSuccess(Integer year);

    Future<List<TransactionYearMethod>> getYearlyTransactionMethodsFailed(Integer year);

    Future<List<TransactionMonthlyAmountSuccess>> getMonthlyAmountTransactionSuccessByMerchant(
            MonthAmountTransactionMerchant req);

    Future<List<TransactionYearlyAmountSuccess>> getYearlyAmountTransactionSuccessByMerchant(
            YearAmountTransactionMerchant req);

    Future<List<TransactionMonthlyAmountFailed>> getMonthlyAmountTransactionFailedByMerchant(
            MonthAmountTransactionMerchant req);

    Future<List<TransactionYearlyAmountFailed>> getYearlyAmountTransactionFailedByMerchant(
            YearAmountTransactionMerchant req);

    Future<List<TransactionMonthlyMethod>> getMonthlyTransactionMethodsByMerchantSuccess(
            MonthMethodTransactionMerchantRequest req);

    Future<List<TransactionMonthlyMethod>> getMonthlyTransactionMethodsByMerchantFailed(
            MonthMethodTransactionMerchantRequest req);

    Future<List<TransactionYearMethod>> getYearlyTransactionMethodsByMerchantSuccess(
            YearMethodTransactionMerchantRequest req);

    Future<List<TransactionYearMethod>> getYearlyTransactionMethodsByMerchantFailed(
            YearMethodTransactionMerchantRequest req);
}