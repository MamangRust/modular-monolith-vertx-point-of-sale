package io.example.cashier.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.CashierStatsRepository;
import io.example.cashier.service.CashierStatsService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
public class CashierStatsServiceImpl implements CashierStatsService {
    private final CashierStatsRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CashierStatsServiceImpl(CashierStatsRepository statsRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.statsRepository = statsRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<List<CashierMonthTotalSales>> getMonthlyTotalSales(int year, int month) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getMonthlyTotalSalesCashier");
        String cacheKey = String.format("cashier:report:monthly_total:%d:%d", year, month);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getMonthlyTotalSales(year, month),
                        new TypeReference<List<CashierMonthTotalSales>>() {}, tracingCtx, "report_monthly_total"))
                .recover(err -> handleError(tracingCtx, "report_monthly_total", err));
    }

    @Override
    public Future<List<CashierYearTotalSales>> getYearlyTotalSales(int year) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getYearlyTotalSalesCashier");
        String cacheKey = String.format("cashier:report:yearly_total:%d", year);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getYearlyTotalSales(year),
                        new TypeReference<List<CashierYearTotalSales>>() {}, tracingCtx, "report_yearly_total"))
                .recover(err -> handleError(tracingCtx, "report_yearly_total", err));
    }

    @Override
    public Future<List<CashierMonthSales>> getMonthlyCashier(int year) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getMonthlyCashier");
        String cacheKey = String.format("cashier:report:monthly_cashier:%d", year);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getMonthlyCashier(year),
                        new TypeReference<List<CashierMonthSales>>() {}, tracingCtx, "report_monthly_cashier"))
                .recover(err -> handleError(tracingCtx, "report_monthly_cashier", err));
    }

    @Override
    public Future<List<CashierYearSales>> getYearlyCashier(int year) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getYearlyCashier");
        String cacheKey = String.format("cashier:report:yearly_cashier:%d", year);

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> statsRepository.getYearlyCashier(year),
                        new TypeReference<List<CashierYearSales>>() {}, tracingCtx, "report_yearly_cashier"))
                .recover(err -> handleError(tracingCtx, "report_yearly_cashier", err));
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
        log.error("Cashier stats service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
