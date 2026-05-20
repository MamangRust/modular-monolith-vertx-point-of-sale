package io.example.cashier.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.CashierStatByIdRepository;
import io.example.cashier.service.CashierStatsByIdService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
public class CashierStatsByIdServiceImpl implements CashierStatsByIdService {
    private final CashierStatByIdRepository statByIdRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CashierStatsByIdServiceImpl(CashierStatByIdRepository statByIdRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.statByIdRepository = statByIdRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<List<CashierMonthTotalSales>> getMonthlyTotalSalesById(int year, int month, Long cashierId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getMonthlyTotalSalesById");
        String cacheKey = String.format("cashier:report:monthly_total:%d:%d:cashier:%d", year, month, cashierId);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statByIdRepository.getMonthlyTotalSalesById(year, month, cashierId),
                        new TypeReference<List<CashierMonthTotalSales>>() {}, tracingCtx, "report_monthly_total_by_id"))
                .recover(err -> handleError(tracingCtx, "report_monthly_total_by_id", err));
    }

    @Override
    public Future<List<CashierYearTotalSales>> getYearlyTotalSalesById(int year, Long cashierId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getYearlyTotalSalesById");
        String cacheKey = String.format("cashier:report:yearly_total:%d:cashier:%d", year, cashierId);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statByIdRepository.getYearlyTotalSalesById(year, cashierId),
                        new TypeReference<List<CashierYearTotalSales>>() {}, tracingCtx, "report_yearly_total_by_id"))
                .recover(err -> handleError(tracingCtx, "report_yearly_total_by_id", err));
    }

    @Override
    public Future<List<CashierMonthSales>> getMonthlyCashierById(int year, Long cashierId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getMonthlyCashierById");
        String cacheKey = String.format("cashier:report:monthly_cashier:%d:cashier:%d", year, cashierId);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statByIdRepository.getMonthlyCashierById(year, cashierId),
                        new TypeReference<List<CashierMonthSales>>() {}, tracingCtx, "report_monthly_by_cashier"))
                .recover(err -> handleError(tracingCtx, "report_monthly_by_cashier", err));
    }

    @Override
    public Future<List<CashierYearSales>> getYearlyCashierById(int year, Long cashierId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getYearlyCashierById");
        String cacheKey = String.format("cashier:report:yearly_cashier:%d:cashier:%d", year, cashierId);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statByIdRepository.getYearlyCashierById(year, cashierId),
                        new TypeReference<List<CashierYearSales>>() {}, tracingCtx, "report_yearly_by_cashier"))
                .recover(err -> handleError(tracingCtx, "report_yearly_by_cashier", err));
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
        log.error("Cashier stats by id service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
