package io.example.merchant.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.common.domain.PagedResult;
import io.example.common.exception.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.FindAllMerchantDocuments;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.service.MerchantDocumentQueryService;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class MerchantDocumentQueryServiceImpl implements MerchantDocumentQueryService {
    private final MerchantDocumentQueryRepository queryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MerchantDocumentQueryServiceImpl(MerchantDocumentQueryRepository queryRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.queryRepository = queryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<PagedResult<MerchantDocument>> getDocuments(FindAllMerchantDocuments req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.getDocuments");
        String cacheKey = String.format("documents:list:%s:%d:%d", req.getSearch() != null ? req.getSearch() : "", req.getPage(), req.getPageSize());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getDocuments(req),
                        new TypeReference<PagedResult<MerchantDocument>>() {}, tracingCtx, "get_documents"))
                .recover(err -> handleError(tracingCtx, "get_documents", err));
    }

    @Override
    public Future<PagedResult<MerchantDocument>> getDocumentsActive(FindAllMerchantDocuments req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.getDocumentsActive");
        String cacheKey = String.format("documents:active:%s:%d:%d", req.getSearch() != null ? req.getSearch() : "", req.getPage(), req.getPageSize());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getDocumentsActive(req),
                        new TypeReference<PagedResult<MerchantDocument>>() {}, tracingCtx, "get_documents_active"))
                .recover(err -> handleError(tracingCtx, "get_documents_active", err));
    }

    @Override
    public Future<PagedResult<MerchantDocument>> getDocumentsTrashed(FindAllMerchantDocuments req) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.getDocumentsTrashed");
        String cacheKey = String.format("documents:trashed:%s:%d:%d", req.getSearch() != null ? req.getSearch() : "", req.getPage(), req.getPageSize());

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getDocumentsTrashed(req),
                        new TypeReference<PagedResult<MerchantDocument>>() {}, tracingCtx, "get_documents_trashed"))
                .recover(err -> handleError(tracingCtx, "get_documents_trashed", err));
    }

    @Override
    public Future<MerchantDocument> getDocumentById(Integer documentId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.getDocumentById");
        String cacheKey = "document:detail:" + documentId;

        return redisService.get(cacheKey)
                .compose(cached -> handleCacheOrRepo(cached, cacheKey,
                        () -> queryRepository.getDocumentById(documentId).compose(res -> {
                            if (res == null) {
                                return Future.failedFuture(new NotFoundException("Merchant document not found"));
                            }
                            return Future.succeededFuture(res);
                        }),
                        new TypeReference<MerchantDocument>() {}, tracingCtx, "get_document_by_id"))
                .recover(err -> handleError(tracingCtx, "get_document_by_id", err));
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
        log.error("MerchantDocument query service error in {}: {}", operation, err.getMessage());
        tracingMetrics.completeSpanError(ctx, operation, err.getMessage());
        return Future.failedFuture(err);
    }
}
