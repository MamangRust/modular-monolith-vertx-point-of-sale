package io.example.cashier.service.impl;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.cashier.domain.requests.cashier.FindAllCashierMerchant;
import io.example.cashier.domain.requests.cashier.FindAllCashiers;
import io.example.cashier.domain.response.cashier.CashierResponse;
import io.example.cashier.domain.response.cashier.CashierResponseDeleteAt;
import io.example.cashier.model.Cashier;
import io.example.cashier.repository.CashierQueryRepository;
import io.example.cashier.service.CashierQueryService;
import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CashierQueryServiceImpl implements CashierQueryService {
    private static final Logger log = LoggerFactory.getLogger(CashierQueryServiceImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final CashierQueryRepository queryRepository;
    private final RedisService redis;
    private final TracingMetrics metrics;

    private static final String CACHE_PREFIX = "cashier:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private PagedResult<CashierResponse> mapPagination(PagedResult<Cashier> res) {
        List<CashierResponse> data = res.getData().stream().map(CashierResponse::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    private PagedResult<CashierResponseDeleteAt> mapPaginationDeleteAt(PagedResult<Cashier> res) {
        List<CashierResponseDeleteAt> data = res.getData().stream().map(CashierResponseDeleteAt::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    @Override
    public Future<PagedResult<CashierResponse>> getCashiers(FindAllCashiers req) {
        var ctx = metrics.startSpan("CashierQueryService.getCashiers");
        String cacheKey = CACHE_PREFIX + "list:all:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Cashier> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Cashier>>() {
                                    });
                            return Future.succeededFuture(mapPagination(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached cashiers: {}", e.getMessage());
                        }
                    }
                    return queryRepository.findAllCashiers(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPagination);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCashiers", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getCashiers", e.getMessage()));
    }

    @Override
    public Future<CashierResponse> getCashierById(Long cashierId) {
        var ctx = metrics.startSpan("CashierQueryService.getCashierById",
                Attributes.builder().put("cashier.id", cashierId).build());
        String key = CACHE_PREFIX + cashierId;

        return redis.getJson(key, Cashier.class)
                .compose(cached -> {
                    if (cached != null) {
                        return Future.succeededFuture(CashierResponse.from(cached));
                    }
                    return queryRepository.findById(cashierId)
                            .compose(db -> {
                                if (db == null) {
                                    return Future.<Cashier>failedFuture(new NotFoundException("Cashier not found"));
                                }
                                return redis.setJson(key, db, CACHE_TTL).<Cashier>map(v -> db);
                            })
                            .map(CashierResponse::from);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCashierById", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getCashierById", e.getMessage()));
    }

    @Override
    public Future<PagedResult<CashierResponseDeleteAt>> getCashiersActive(FindAllCashiers req) {
        var ctx = metrics.startSpan("CashierQueryService.getCashiersActive");
        String cacheKey = CACHE_PREFIX + "list:active:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Cashier> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Cashier>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached active cashiers: {}", e.getMessage());
                        }
                    }
                    return queryRepository.findByActive(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPaginationDeleteAt);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCashiersActive", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getCashiersActive", e.getMessage()));
    }

    @Override
    public Future<PagedResult<CashierResponseDeleteAt>> getCashiersTrashed(FindAllCashiers req) {
        var ctx = metrics.startSpan("CashierQueryService.getCashiersTrashed");
        String cacheKey = CACHE_PREFIX + "list:trashed:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Cashier> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Cashier>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached trashed cashiers: {}", e.getMessage());
                        }
                    }
                    return queryRepository.findByTrashed(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPaginationDeleteAt);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCashiersTrashed", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getCashiersTrashed", e.getMessage()));
    }

    @Override
    public Future<PagedResult<CashierResponse>> getCashiersByMerchant(FindAllCashierMerchant req) {
        var ctx = metrics.startSpan("CashierQueryService.getCashiersByMerchant");
        String cacheKey = CACHE_PREFIX + "list:merchant:" + req.getMerchantId() + ":"
                + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Cashier> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Cashier>>() {
                                    });
                            return Future.succeededFuture(mapPagination(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached merchant cashiers: {}", e.getMessage());
                        }
                    }
                    return queryRepository.findByMerchant(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPagination);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getCashiersByMerchant", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getCashiersByMerchant", e.getMessage()));
    }
}