package io.example.merchant.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.common.domain.PagedResult;
import io.example.common.exception.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.FindAllMerchants;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.service.MerchantQueryService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

@Slf4j
public class MerchantQueryServiceImpl implements MerchantQueryService {
    private final MerchantQueryRepository queryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MerchantQueryServiceImpl(MerchantQueryRepository queryRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.queryRepository = queryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<PagedResult<Merchant>> getMerchants(FindAllMerchants req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.getMerchants");
        String cacheKey = String.format("merchants:list:%s:%d:%d", req.getSearch() != null ? req.getSearch() : "", req.getPage(), req.getPageSize());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getMerchants(req),
                        new TypeReference<PagedResult<Merchant>>() {}, tracingCtx, "get_merchants"))
                .recover(err -> handleError(tracingCtx, "get_merchants", err));
    }

    @Override
    public Future<PagedResult<Merchant>> getMerchantsActive(FindAllMerchants req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.getMerchantsActive");
        String cacheKey = String.format("merchants:active:%s:%d:%d", req.getSearch() != null ? req.getSearch() : "", req.getPage(), req.getPageSize());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getMerchantsActive(req),
                        new TypeReference<PagedResult<Merchant>>() {}, tracingCtx, "get_merchants_active"))
                .recover(err -> handleError(tracingCtx, "get_merchants_active", err));
    }

    @Override
    public Future<PagedResult<Merchant>> getMerchantsTrashed(FindAllMerchants req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.getMerchantsTrashed");
        String cacheKey = String.format("merchants:trashed:%s:%d:%d", req.getSearch() != null ? req.getSearch() : "", req.getPage(), req.getPageSize());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getMerchantsTrashed(req),
                        new TypeReference<PagedResult<Merchant>>() {}, tracingCtx, "get_merchants_trashed"))
                .recover(err -> handleError(tracingCtx, "get_merchants_trashed", err));
    }

    @Override
    public Future<Merchant> getMerchantById(Integer merchantId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.getMerchantById");
        String cacheKey = "merchant:detail:" + merchantId;

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getMerchantById(merchantId).compose(res -> {
                            if (res == null) {
                                return Future.failedFuture(new NotFoundException("Merchant not found"));
                            }
                            return Future.succeededFuture(res);
                        }),
                        new TypeReference<Merchant>() {}, tracingCtx, "get_merchant_by_id"))
                .recover(err -> handleError(tracingCtx, "get_merchant_by_id", err));
    }

    @Override
    public Future<Merchant> getMerchantByApiKey(String apiKey) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.getMerchantByApiKey");
        String cacheKey = "merchant:apikey:" + apiKey;

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getMerchantByApiKey(apiKey).compose(res -> {
                            if (res == null) {
                                return Future.failedFuture(new NotFoundException("Merchant not found for API Key"));
                            }
                            return Future.succeededFuture(res);
                        }),
                        new TypeReference<Merchant>() {}, tracingCtx, "get_merchant_by_api_key"))
                .recover(err -> handleError(tracingCtx, "get_merchant_by_api_key", err));
    }

    @Override
    public Future<List<Merchant>> getMerchantsByUserId(Integer userId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.getMerchantsByUserId");
        String cacheKey = "merchants:user:" + userId;

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getMerchantsByUserId(userId),
                        new TypeReference<List<Merchant>>() {}, tracingCtx, "get_merchants_by_user_id"))
                .recover(err -> handleError(tracingCtx, "get_merchants_by_user_id", err));
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

    private <T> Future<T> handleError(TracingMetrics.TracingContext ctx, String operation, Throwable err) {
        log.error("Merchant query service error in {}: {}", operation, err.getMessage());
        tracingMetrics.completeSpanError(ctx, operation, err.getMessage());
        return Future.failedFuture(err);
    }
}
