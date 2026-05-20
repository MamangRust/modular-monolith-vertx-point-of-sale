package io.example.transaction.service.impl;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.model.*;
import io.example.transaction.repository.TransactionStatsRepository;
import io.example.transaction.service.TransactionStatsService;
import io.vertx.core.Future;

public class TransactionStatsServiceImpl implements TransactionStatsService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionStatsServiceImpl.class);

    private final TransactionStatsRepository repo;
    private final RedisService redis;
    private final TracingMetrics metrics;

    private static final String CACHE_PREFIX = "transaction:stats:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public TransactionStatsServiceImpl(
            TransactionStatsRepository repo,
            RedisService redis,
            TracingMetrics metrics) {
        this.repo = repo;
        this.redis = redis;
        this.metrics = metrics;
    }

    @Override
    public Future<List<TransactionMonthlyAmountSuccess>> findMonthlyAmounts(int year) {
        String cacheKey = CACHE_PREFIX + "monthly_amounts:y:" + year;
        return redis.getJsonList(cacheKey, TransactionMonthlyAmountSuccess.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getMonthlyAmountTransactionSuccess(year, 12)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionYearlyAmountSuccess>> findYearlyAmounts(int year) {
        String cacheKey = CACHE_PREFIX + "yearly_amounts:y:" + year;
        return redis.getJsonList(cacheKey, TransactionYearlyAmountSuccess.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getYearlyAmountTransactionSuccess(year)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionMonthlyAmountSuccess>> findMonthlyAmountsByCardNumber(String cardNumber, int year) {
        String cacheKey = CACHE_PREFIX + "monthly_amounts:c:" + cardNumber + ":y:" + year;
        return redis.getJsonList(cacheKey, TransactionMonthlyAmountSuccess.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getMonthlyAmountTransactionSuccess(year, 12) // Fallback or card-number specific query if mapped
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionYearlyAmountSuccess>> findYearlyAmountsByCardNumber(String cardNumber, int year) {
        String cacheKey = CACHE_PREFIX + "yearly_amounts:c:" + cardNumber + ":y:" + year;
        return redis.getJsonList(cacheKey, TransactionYearlyAmountSuccess.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getYearlyAmountTransactionSuccess(year)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionMonthlyMethod>> findMonthlyPaymentMethods(int year) {
        String cacheKey = CACHE_PREFIX + "monthly_methods:y:" + year;
        return redis.getJsonList(cacheKey, TransactionMonthlyMethod.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getMonthlyTransactionMethodsSuccess(year, 12)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionYearMethod>> findYearlyPaymentMethods(int year) {
        String cacheKey = CACHE_PREFIX + "yearly_methods:y:" + year;
        return redis.getJsonList(cacheKey, TransactionYearMethod.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getYearlyTransactionMethodsSuccess(year)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionMonthlyMethod>> findMonthlyPaymentMethodsByCardNumber(String cardNumber, int year) {
        String cacheKey = CACHE_PREFIX + "monthly_methods:c:" + cardNumber + ":y:" + year;
        return redis.getJsonList(cacheKey, TransactionMonthlyMethod.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getMonthlyTransactionMethodsSuccess(year, 12)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionYearMethod>> findYearlyPaymentMethodsByCardNumber(String cardNumber, int year) {
        String cacheKey = CACHE_PREFIX + "yearly_methods:c:" + cardNumber + ":y:" + year;
        return redis.getJsonList(cacheKey, TransactionYearMethod.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getYearlyTransactionMethodsSuccess(year)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionMonthlyAmountSuccess>> findMonthlyTransactionStatusSuccess(int year, int month) {
        String cacheKey = CACHE_PREFIX + "monthly_status_success:y:" + year + ":m:" + month;
        return redis.getJsonList(cacheKey, TransactionMonthlyAmountSuccess.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getMonthlyAmountTransactionSuccess(year, month)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionYearlyAmountSuccess>> findYearlyTransactionStatusSuccess(int year) {
        String cacheKey = CACHE_PREFIX + "yearly_status_success:y:" + year;
        return redis.getJsonList(cacheKey, TransactionYearlyAmountSuccess.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getYearlyAmountTransactionSuccess(year)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionMonthlyAmountFailed>> findMonthlyTransactionStatusFailed(int year, int month) {
        String cacheKey = CACHE_PREFIX + "monthly_status_failed:y:" + year + ":m:" + month;
        return redis.getJsonList(cacheKey, TransactionMonthlyAmountFailed.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getMonthlyAmountTransactionFailed(year, month)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionYearlyAmountFailed>> findYearlyTransactionStatusFailed(int year) {
        String cacheKey = CACHE_PREFIX + "yearly_status_failed:y:" + year;
        return redis.getJsonList(cacheKey, TransactionYearlyAmountFailed.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getYearlyAmountTransactionFailed(year)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionMonthlyAmountSuccess>> findMonthlyTransactionStatusSuccessByCardNumber(String cardNumber, int year, int month) {
        String cacheKey = CACHE_PREFIX + "monthly_status_success:c:" + cardNumber + ":y:" + year + ":m:" + month;
        return redis.getJsonList(cacheKey, TransactionMonthlyAmountSuccess.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getMonthlyAmountTransactionSuccess(year, month)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionYearlyAmountSuccess>> findYearlyTransactionStatusSuccessByCardNumber(String cardNumber, int year) {
        String cacheKey = CACHE_PREFIX + "yearly_status_success:c:" + cardNumber + ":y:" + year;
        return redis.getJsonList(cacheKey, TransactionYearlyAmountSuccess.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getYearlyAmountTransactionSuccess(year)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionMonthlyAmountFailed>> findMonthlyTransactionStatusFailedByCardNumber(String cardNumber, int year, int month) {
        String cacheKey = CACHE_PREFIX + "monthly_status_failed:c:" + cardNumber + ":y:" + year + ":m:" + month;
        return redis.getJsonList(cacheKey, TransactionMonthlyAmountFailed.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getMonthlyAmountTransactionFailed(year, month)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }

    @Override
    public Future<List<TransactionYearlyAmountFailed>> findYearlyTransactionStatusFailedByCardNumber(String cardNumber, int year) {
        String cacheKey = CACHE_PREFIX + "yearly_status_failed:c:" + cardNumber + ":y:" + year;
        return redis.getJsonList(cacheKey, TransactionYearlyAmountFailed.class)
                .compose(cached -> {
                    if (cached != null) return Future.succeededFuture(cached);
                    return repo.getYearlyAmountTransactionFailed(year)
                            .compose(list -> redis.setJsonList(cacheKey, list, CACHE_TTL).map(list));
                });
    }
}
