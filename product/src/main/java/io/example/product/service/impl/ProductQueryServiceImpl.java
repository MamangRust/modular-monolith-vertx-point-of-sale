package io.example.product.service.impl;

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
import io.example.product.domain.requests.FindAllProducts;
import io.example.product.domain.requests.ProductByCategoryRequest;
import io.example.product.domain.requests.ProductByMerchantRequest;
import io.example.product.domain.response.ProductResponse;
import io.example.product.domain.response.ProductResponseDeleteAt;
import io.example.product.model.Product;
import io.example.product.repository.ProductQueryRepository;
import io.example.product.service.ProductQueryService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProductQueryServiceImpl implements ProductQueryService {
    private static final Logger log = LoggerFactory.getLogger(ProductQueryServiceImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ProductQueryRepository queryRepository;
    private final RedisService redis;
    private final TracingMetrics metrics;

    private static final String CACHE_PREFIX = "product:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private PagedResult<ProductResponse> mapPagination(PagedResult<Product> res) {
        List<ProductResponse> data = res.getData().stream().map(ProductResponse::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    private PagedResult<ProductResponseDeleteAt> mapPaginationDeleteAt(PagedResult<Product> res) {
        List<ProductResponseDeleteAt> data = res.getData().stream().map(ProductResponseDeleteAt::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    private void normalizeRequest(FindAllProducts req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSearch(keyword);
    }

    @Override
    public Future<PagedResult<ProductResponse>> getAll(FindAllProducts req) {
        normalizeRequest(req);
        var ctx = metrics.startSpan("ProductQueryService.getAll");
        String cacheKey = CACHE_PREFIX + "list:all:" + req.getSearch() + ":" + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Product> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Product>>() {
                                    });
                            return Future.succeededFuture(mapPagination(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached products: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getProducts(req)
                            .map(res -> {
                                redis.setJson(cacheKey, (Object) res, CACHE_TTL);
                                return mapPagination(res);
                            });
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getAll", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getAll", e.getMessage()));
    }

    @Override
    public Future<PagedResult<ProductResponseDeleteAt>> getActive(FindAllProducts req) {
        normalizeRequest(req);
        var ctx = metrics.startSpan("ProductQueryService.getActive");
        String cacheKey = CACHE_PREFIX + "list:active:" + req.getSearch() + ":" + req.getPage() + ":"
                + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Product> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Product>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached active products: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getProductsActive(req)
                            .map(res -> {
                                redis.setJson(cacheKey, (Object) res, CACHE_TTL);
                                return mapPaginationDeleteAt(res);
                            });
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getActive", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getActive", e.getMessage()));
    }

    @Override
    public Future<PagedResult<ProductResponseDeleteAt>> getTrashed(FindAllProducts req) {
        normalizeRequest(req);
        var ctx = metrics.startSpan("ProductQueryService.getTrashed");
        String cacheKey = CACHE_PREFIX + "list:trashed:" + req.getSearch() + ":" + req.getPage() + ":"
                + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Product> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Product>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached trashed products: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getProductsTrashed(req)
                            .map(res -> {
                                redis.setJson(cacheKey, (Object) res, CACHE_TTL);
                                return mapPaginationDeleteAt(res);
                            });
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTrashed", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getTrashed", e.getMessage()));
    }

    @Override
    public Future<PagedResult<ProductResponse>> getByMerchant(ProductByMerchantRequest req) {
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() != null ? req.getPageSize() : 10;
        req.setPage(page);
        req.setPageSize(pageSize);

        var ctx = metrics.startSpan("ProductQueryService.getByMerchant");
        String cacheKey = CACHE_PREFIX + "list:merchant:" + req.getMerchantId() + ":" + req.getSearch() + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Product> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Product>>() {
                                    });
                            return Future.succeededFuture(mapPagination(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached merchant products: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getProductsByMerchant(req)
                            .map(res -> {
                                redis.setJson(cacheKey, (Object) res, CACHE_TTL);
                                return mapPagination(res);
                            });
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getByMerchant", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getByMerchant", e.getMessage()));
    }

    @Override
    public Future<PagedResult<ProductResponse>> getByCategoryName(ProductByCategoryRequest req) {
        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() != null ? req.getPageSize() : 10;
        req.setPage(page);
        req.setPageSize(pageSize);

        var ctx = metrics.startSpan("ProductQueryService.getByCategoryName");
        String cacheKey = CACHE_PREFIX + "list:category:" + req.getCategoryName() + ":" + req.getSearch() + ":"
                + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<Product> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<Product>>() {
                                    });
                            return Future.succeededFuture(mapPagination(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached category products: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getProductsByCategoryName(req)
                            .map(res -> {
                                redis.setJson(cacheKey, (Object) res, CACHE_TTL);
                                return mapPagination(res);
                            });
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getByCategoryName", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getByCategoryName", e.getMessage()));
    }

    @Override
    public Future<ProductResponse> getById(Long id) {
        var ctx = metrics.startSpan("ProductQueryService.getById",
                Attributes.builder().put("product.id", id).build());
        String key = CACHE_PREFIX + id;

        return redis.getJson(key, Product.class)
                .compose(cached -> {
                    if (cached != null) {
                        return Future.succeededFuture(ProductResponse.from(cached));
                    }
                    return queryRepository.getProductById(id)
                            .compose(db -> {
                                if (db == null) {
                                    return Future.<Product>failedFuture(new NotFoundException("Product not found"));
                                }
                                return redis.setJson(key, db, CACHE_TTL).<Product>map(v -> db);
                            })
                            .map(ProductResponse::from);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getById", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getById", e.getMessage()));
    }
}