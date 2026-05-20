package io.example.transaction.service;

import java.util.List;
import io.example.transaction.model.*;
import io.vertx.core.Future;

public interface TransactionStatsService {
    Future<List<TransactionMonthlyAmountSuccess>> findMonthlyAmounts(int year);
    Future<List<TransactionYearlyAmountSuccess>> findYearlyAmounts(int year);
    Future<List<TransactionMonthlyAmountSuccess>> findMonthlyAmountsByCardNumber(String cardNumber, int year);
    Future<List<TransactionYearlyAmountSuccess>> findYearlyAmountsByCardNumber(String cardNumber, int year);

    Future<List<TransactionMonthlyMethod>> findMonthlyPaymentMethods(int year);
    Future<List<TransactionYearMethod>> findYearlyPaymentMethods(int year);
    Future<List<TransactionMonthlyMethod>> findMonthlyPaymentMethodsByCardNumber(String cardNumber, int year);
    Future<List<TransactionYearMethod>> findYearlyPaymentMethodsByCardNumber(String cardNumber, int year);

    Future<List<TransactionMonthlyAmountSuccess>> findMonthlyTransactionStatusSuccess(int year, int month);
    Future<List<TransactionYearlyAmountSuccess>> findYearlyTransactionStatusSuccess(int year);
    Future<List<TransactionMonthlyAmountFailed>> findMonthlyTransactionStatusFailed(int year, int month);
    Future<List<TransactionYearlyAmountFailed>> findYearlyTransactionStatusFailed(int year);

    Future<List<TransactionMonthlyAmountSuccess>> findMonthlyTransactionStatusSuccessByCardNumber(String cardNumber, int year, int month);
    Future<List<TransactionYearlyAmountSuccess>> findYearlyTransactionStatusSuccessByCardNumber(String cardNumber, int year);
    Future<List<TransactionMonthlyAmountFailed>> findMonthlyTransactionStatusFailedByCardNumber(String cardNumber, int year, int month);
    Future<List<TransactionYearlyAmountFailed>> findYearlyTransactionStatusFailedByCardNumber(String cardNumber, int year);
}
