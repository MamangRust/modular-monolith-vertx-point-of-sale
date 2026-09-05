package io.example.merchant.service.impl;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.FindAllMerchantDocuments;
import io.example.merchant.domain.response.MerchantDocumentResponse;
import io.example.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.service.MerchantDocumentQueryService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MerchantDocumentQueryServiceImpl implements MerchantDocumentQueryService {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final MerchantDocumentQueryRepository queryRepository;
    private final RedisService redis;
    private final TracingMetrics metrics;

    private static final String CACHE_PREFIX = "document:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private PagedResult<MerchantDocumentResponse> mapPagination(PagedResult<MerchantDocument> res) {
        List<MerchantDocumentResponse> data = res.getData().stream().map(MerchantDocumentResponse::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    private PagedResult<MerchantDocumentResponseDeleteAt> mapPaginationDeleteAt(PagedResult<MerchantDocument> res) {
        List<MerchantDocumentResponseDeleteAt> data = res.getData().stream().map(MerchantDocumentResponseDeleteAt::from)
                .toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    @Override
    public Future<PagedResult<MerchantDocumentResponse>> findAll(FindAllMerchantDocuments req) {
        var ctx = metrics.startSpan("MerchantDocumentQueryService.findAll");
        String cacheKey = CACHE_PREFIX + "list:all:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<MerchantDocument> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<MerchantDocument>>() {
                                    });
                            return Future.succeededFuture(mapPagination(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached documents: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getDocuments(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPagination);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getDocuments", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getDocuments", e.getMessage()));
    }

    @Override
    public Future<MerchantDocumentResponse> findById(Long documentId) {
        var ctx = metrics.startSpan("MerchantDocumentQueryService.findById",
                Attributes.builder().put("document.id", documentId).build());
        String key = CACHE_PREFIX + "detail:" + documentId;

        return redis.getJson(key, MerchantDocument.class)
                .compose(cached -> {
                    if (cached != null) {
                        return Future.succeededFuture(MerchantDocumentResponse.from(cached));
                    }
                    return queryRepository.getDocumentById(documentId)
                            .compose(db -> {
                                if (db == null) {
                                    return Future.<MerchantDocument>failedFuture(
                                            new NotFoundException("Document not found"));
                                }
                                return redis.setJson(key, db, CACHE_TTL).<MerchantDocument>map(v -> db);
                            })
                            .map(MerchantDocumentResponse::from);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getDocumentById", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getDocumentById", e.getMessage()));
    }

    @Override
    public Future<PagedResult<MerchantDocumentResponseDeleteAt>> findByActive(FindAllMerchantDocuments req) {
        var ctx = metrics.startSpan("MerchantDocumentQueryService.findByActive");
        String cacheKey = CACHE_PREFIX + "list:active:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<MerchantDocument> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<MerchantDocument>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached active documents: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getDocumentsActive(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPaginationDeleteAt);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getDocumentsActive", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getDocumentsActive", e.getMessage()));
    }

    @Override
    public Future<PagedResult<MerchantDocumentResponseDeleteAt>> findByTrashed(FindAllMerchantDocuments req) {
        var ctx = metrics.startSpan("MerchantDocumentQueryService.findByTrashed");
        String cacheKey = CACHE_PREFIX + "list:trashed:" + (req.getSearch() != null ? req.getSearch() : "") + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<MerchantDocument> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<MerchantDocument>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached trashed documents: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getDocumentsTrashed(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPaginationDeleteAt);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getDocumentsTrashed", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getDocumentsTrashed", e.getMessage()));
    }
}