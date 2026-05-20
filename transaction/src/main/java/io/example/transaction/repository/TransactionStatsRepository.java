package io.example.transaction.repository;

import java.util.List;
import io.example.transaction.model.*;
import io.vertx.core.Future;

public interface TransactionStatsRepository {
    Future<List<TransactionMonthlyAmountSuccess>> getMonthlyAmountTransactionSuccess(int year, int month);
    Future<List<TransactionYearlyAmountSuccess>> getYearlyAmountTransactionSuccess(int year);
    Future<List<TransactionMonthlyAmountFailed>> getMonthlyAmountTransactionFailed(int year, int month);
    Future<List<TransactionYearlyAmountFailed>> getYearlyAmountTransactionFailed(int year);

    Future<List<TransactionMonthlyMethod>> getMonthlyTransactionMethodsSuccess(int year, int month);
    Future<List<TransactionMonthlyMethod>> getMonthlyTransactionMethodsFailed(int year, int month);
    Future<List<TransactionYearMethod>> getYearlyTransactionMethodsSuccess(int year);
    Future<List<TransactionYearMethod>> getYearlyTransactionMethodsFailed(int year);

    Future<List<TransactionMonthlyAmountSuccess>> getMonthlyAmountTransactionSuccessByMerchant(int merchantId, int year, int month);
    Future<List<TransactionYearlyAmountSuccess>> getYearlyAmountTransactionSuccessByMerchant(int merchantId, int year);
    Future<List<TransactionMonthlyAmountFailed>> getMonthlyAmountTransactionFailedByMerchant(int merchantId, int year, int month);
    Future<List<TransactionYearlyAmountFailed>> getYearlyAmountTransactionFailedByMerchant(int merchantId, int year);

    Future<List<TransactionMonthlyMethod>> getMonthlyTransactionMethodsByMerchantSuccess(int merchantId, int year, int month);
    Future<List<TransactionMonthlyMethod>> getMonthlyTransactionMethodsByMerchantFailed(int merchantId, int year, int month);
    Future<List<TransactionYearMethod>> getYearlyTransactionMethodsByMerchantSuccess(int merchantId, int year);
    Future<List<TransactionYearMethod>> getYearlyTransactionMethodsByMerchantFailed(int merchantId, int year);
}
