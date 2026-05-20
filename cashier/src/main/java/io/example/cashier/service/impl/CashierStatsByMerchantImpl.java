package io.example.cashier.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.CashierStatByMerchantRepository;
import io.example.cashier.service.CashierStatsByMerchant;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
public class CashierStatsByMerchantImpl implements CashierStatsByMerchant {
    private final CashierStatByMerchantRepository statByMerchantRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CashierStatsByMerchantImpl(CashierStatByMerchantRepository statByMerchantRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.statByMerchantRepository = statByMerchantRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<List<CashierMonthTotalSales>> getMonthlyTotalSalesByMerchant(int year, int month, Long merchantId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getMonthlyTotalSalesByMerchant");
        String cacheKey = String.format("cashier:report:monthly_total:%d:%d:merchant:%d", year, month, merchantId);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statByMerchantRepository.getMonthlyTotalSalesByMerchant(year, month, merchantId),
                        new TypeReference<List<CashierMonthTotalSales>>() {}, tracingCtx, "report_monthly_total_by_merchant"))
                .recover(err -> handleError(tracingCtx, "report_monthly_total_by_merchant", err));
    }

    @Override
    public Future<List<CashierYearTotalSales>> getYearlyTotalSalesByMerchant(int year, Long merchantId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getYearlyTotalSalesByMerchant");
        String cacheKey = String.format("cashier:report:yearly_total:%d:merchant:%d", year, merchantId);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statByMerchantRepository.getYearlyTotalSalesByMerchant(year, merchantId),
                        new TypeReference<List<CashierYearTotalSales>>() {}, tracingCtx, "report_yearly_total_by_merchant"))
                .recover(err -> handleError(tracingCtx, "report_yearly_total_by_merchant", err));
    }

    @Override
    public Future<List<CashierMonthSales>> getMonthlyCashierByMerchant(int year, Long merchantId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getMonthlyCashierByMerchant");
        String cacheKey = String.format("cashier:report:monthly_cashier:%d:merchant:%d", year, merchantId);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statByMerchantRepository.getMonthlyCashierByMerchant(year, merchantId),
                        new TypeReference<List<CashierMonthSales>>() {}, tracingCtx, "report_monthly_by_merchant"))
                .recover(err -> handleError(tracingCtx, "report_monthly_by_merchant", err));
    }

    @Override
    public Future<List<CashierYearSales>> getYearlyCashierByMerchant(int year, Long merchantId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getYearlyCashierByMerchant");
        String cacheKey = String.format("cashier:report:yearly_cashier:%d:merchant:%d", year, merchantId);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statByMerchantRepository.getYearlyCashierByMerchant(year, merchantId),
                        new TypeReference<List<CashierYearSales>>() {}, tracingCtx, "report_yearly_by_merchant"))
                .recover(err -> handleError(tracingCtx, "report_yearly_by_merchant", err));
    }

    private <T> Future<T> handleCacheOrRepo(String cached, String cacheKey,
                                            java.util.concurrent.Callable<Future<T>> repoCall, TypeReference<T> typeRef,
                                            TracingMetrics.TracingContext tracingCtx, String operation) {
        if (cached != null) {
            try {
                T data = objectMapper.readValue(cached, typeRef);
                return Future.succeededFuture(data);
            } catch (Exception e) {
                log.warn("Cache parse error", e);
            }
        }
        try {
            return repoCall.call().compose(res -> {
                if (res != null) {
                    redisService.set(cacheKey, Json.encode(res), Duration.ofMinutes(30));
                }
                tracingMetrics.completeSpanSuccess(tracingCtx, operation, "Success");
                return Future.succeededFuture(res);
            });
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    private <T> Future<T> handleError(TracingMetrics.TracingContext ctx, String methodName, Throwable err) {
        log.error("Cashier stats by merchant service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
