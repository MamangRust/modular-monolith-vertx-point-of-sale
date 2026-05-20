package io.example.cashier.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.cashier.model.Cashier;
import io.example.cashier.repository.CashierQueryRepository;
import io.example.cashier.service.CashierQueryService;
import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class CashierQueryServiceImpl implements CashierQueryService {
    private final CashierQueryRepository queryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CashierQueryServiceImpl(CashierQueryRepository queryRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.queryRepository = queryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<PagedResult<Cashier>> getCashiers(String search, int page, int pageSize) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getCashiers");
        String cacheKey = String.format("cashiers:all:%d:%d:%s", page, pageSize, search != null ? search : "");

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.findAllCashiers(search, page, pageSize),
                        new TypeReference<PagedResult<Cashier>>() {}, tracingCtx, "get_all"))
                .recover(err -> handleError(tracingCtx, "get_all", err));
    }

    @Override
    public Future<Cashier> getCashierById(Long cashierId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getCashierById");
        String cacheKey = "cashier:id:" + cashierId;

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.findById(cashierId).compose(cashier -> {
                            if (cashier == null) {
                                return Future.failedFuture(new io.example.common.exception.NotFoundException("Cashier not found"));
                            }
                            return Future.succeededFuture(cashier);
                        }),
                        new TypeReference<Cashier>() {}, tracingCtx, "get_by_id"))
                .recover(err -> handleError(tracingCtx, "get_by_id", err));
    }

    @Override
    public Future<PagedResult<Cashier>> getCashiersActive(String search, int page, int pageSize) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getCashiersActive");
        String cacheKey = String.format("cashiers:active:%d:%d:%s", page, pageSize, search != null ? search : "");

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.findByActive(search, page, pageSize),
                        new TypeReference<PagedResult<Cashier>>() {}, tracingCtx, "get_active"))
                .recover(err -> handleError(tracingCtx, "get_active", err));
    }

    @Override
    public Future<PagedResult<Cashier>> getCashiersTrashed(String search, int page, int pageSize) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getCashiersTrashed");
        String cacheKey = String.format("cashiers:trashed:%d:%d:%s", page, pageSize, search != null ? search : "");

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.findByTrashed(search, page, pageSize),
                        new TypeReference<PagedResult<Cashier>>() {}, tracingCtx, "get_trashed"))
                .recover(err -> handleError(tracingCtx, "get_trashed", err));
    }

    @Override
    public Future<PagedResult<Cashier>> getCashiersByMerchant(Long merchantId, String search, int page, int pageSize) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.getCashiersByMerchant");
        String cacheKey = String.format("cashiers:merchant:%d:%d:%d:%s", merchantId, page, pageSize, search != null ? search : "");

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.findByMerchant(merchantId, search, page, pageSize),
                        new TypeReference<PagedResult<Cashier>>() {}, tracingCtx, "get_by_merchant"))
                .recover(err -> handleError(tracingCtx, "get_by_merchant", err));
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
        log.error("Cashier query service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
