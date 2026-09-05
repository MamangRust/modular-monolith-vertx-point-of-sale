package io.example.merchant.service.impl;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.FindAllMerchants;
import io.example.merchant.domain.response.MerchantResponse;
import io.example.merchant.domain.response.MerchantResponseDeleteAt;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.service.MerchantQueryService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MerchantQueryServiceImpl implements MerchantQueryService {
    private static final Logger log = LoggerFactory.getLogger(MerchantQueryServiceImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final MerchantQueryRepository queryRepository;
    private final RedisService redis;
    private final TracingMetrics metrics;

    private static final String CACHE_PREFIX = "merchant:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    // --- Helper Mapping ---
    private PagedResult<MerchantResponse> mapPagination(PagedResult<Merchant> res) {
        List<MerchantResponse> data = res.getData().stream().map(MerchantResponse::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    private PagedResult<MerchantResponseDeleteAt> mapPaginationDeleteAt(PagedResult<Merchant> res) {
        List<MerchantResponseDeleteAt> data = res.getData().stream().map(MerchantResponseDeleteAt::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    @Override
    public Future<PagedResult<MerchantResponse>> findAll(FindAllMerchants req) {
        var ctx = metrics.startSpan("MerchantQueryService.findAll");
        String cacheKey = CACHE_PREFIX + "list:all:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Merchant> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Merchant>>() {
                                    });
                            return Future.succeededFuture(mapPagination(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached merchants: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getMerchants(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPagination);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "findAll", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "findAll", e.getMessage()));
    }

    @Override
    public Future<MerchantResponse> findById(Long merchantId) {
        var ctx = metrics.startSpan("MerchantQueryService.findById",
                Attributes.builder().put("merchant.id", merchantId).build());
        String key = CACHE_PREFIX + "detail:" + merchantId;

        return redis.getJson(key, Merchant.class)
                .compose(cached -> {
                    if (cached != null) {
                        return Future.succeededFuture(MerchantResponse.from(cached));
                    }
                    return queryRepository.getMerchantById(merchantId)
                            .compose(db -> {
                                if (db == null) {
                                    return Future.<Merchant>failedFuture(new NotFoundException("Merchant not found"));
                                }
                                return redis.setJson(key, db, CACHE_TTL).<Merchant>map(v -> db);
                            })
                            .map(MerchantResponse::from);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "findById", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "findById", e.getMessage()));
    }

    @Override
    public Future<PagedResult<MerchantResponseDeleteAt>> findByActive(FindAllMerchants req) {
        var ctx = metrics.startSpan("MerchantQueryService.findByActive");
        String cacheKey = CACHE_PREFIX + "list:active:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Merchant> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Merchant>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached active merchants: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getMerchantsActive(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPaginationDeleteAt);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "findByActive", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "findByActive", e.getMessage()));
    }

    @Override
    public Future<PagedResult<MerchantResponseDeleteAt>> findByTrashed(FindAllMerchants req) {
        var ctx = metrics.startSpan("MerchantQueryService.findByTrashed");
        String cacheKey = CACHE_PREFIX + "list:trashed:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Merchant> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Merchant>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached trashed merchants: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getMerchantsTrashed(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPaginationDeleteAt);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "findByTrashed", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "findByTrashed", e.getMessage()));
    }
}