package io.example.product.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PagedResult;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.product.domain.requests.FindAllProducts;
import io.example.product.domain.requests.ProductByCategoryRequest;
import io.example.product.domain.requests.ProductByMerchantRequest;
import io.example.product.model.Product;
import io.example.product.model.ProductResponse;
import io.example.product.model.ProductResponseDeleteAt;
import io.example.product.repository.ProductQueryRepository;
import io.example.product.service.ProductQueryService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.Json;

public class ProductQueryServiceImpl implements ProductQueryService {
    private static final Logger logger = LoggerFactory.getLogger(ProductQueryServiceImpl.class);

    private final ProductQueryRepository repository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductQueryServiceImpl(
            ProductQueryRepository repository,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.repository = repository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<ApiResponsePagination<List<ProductResponse>>> getAll(FindAllProducts req) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("ProductQueryService.getAll");
        Span span = Span.fromContext(tracingContext.getContext());

        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSearch(keyword);

        logger.info("Fetching all products | search={}, page={}, pageSize={}", keyword, page, pageSize);
        String cacheKey = String.format("products:page:%d:search:%s", page, keyword);

        return redisService.get(cacheKey)
                .compose(cached -> {
                    span.setAttribute("cache.hit", cached != null);
                    return handleCacheOrRepo(
                            cached, cacheKey,
                            () -> repository.getProducts(req),
                            new TypeReference<PagedResult<Product>>() {
                            },
                            result -> mapToPagedResponse(result, req),
                            tracingContext, "get_all", Duration.ofMinutes(10));
                })
                .map(response -> {
                    span.setAttribute("records.count", response.data().size());
                    span.setAttribute("records.total", response.pagination().totalRecords());
                    return response;
                })
                .recover(err -> {
                    logger.error("Failed to fetch products", err);
                    tracingMetrics.completeSpanError(tracingContext, "get_all", err.getMessage());
                    return Future
                            .succeededFuture(ApiResponsePagination.error("Failed to fetch data: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponsePagination<List<ProductResponse>>> getActive(FindAllProducts req) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("ProductQueryService.getActive");
        Span span = Span.fromContext(tracingContext.getContext());

        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSearch(keyword);

        logger.info("Fetching active products | search={}, page={}, pageSize={}", keyword, page, pageSize);
        String cacheKey = String.format("products:active:page:%d:search:%s", page, keyword);

        return redisService.get(cacheKey)
                .compose(cached -> {
                    span.setAttribute("cache.hit", cached != null);
                    return handleCacheOrRepo(
                            cached, cacheKey,
                            () -> repository.getProductsActive(req),
                            new TypeReference<PagedResult<Product>>() {
                            },
                            result -> mapToPagedResponse(result, req),
                            tracingContext, "get_active", Duration.ofMinutes(10));
                })
                .map(response -> {
                    span.setAttribute("records.count", response.data().size());
                    span.setAttribute("records.total", response.pagination().totalRecords());
                    return response;
                })
                .recover(err -> {
                    logger.error("Failed to fetch active products", err);
                    tracingMetrics.completeSpanError(tracingContext, "get_active", err.getMessage());
                    return Future
                            .succeededFuture(ApiResponsePagination.error("Failed to fetch data: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponsePagination<List<ProductResponseDeleteAt>>> getTrashed(FindAllProducts req) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("ProductQueryService.getTrashed");
        Span span = Span.fromContext(tracingContext.getContext());

        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSearch(keyword);

        logger.info("Fetching trashed products | search={}, page={}, pageSize={}", keyword, page, pageSize);
        String cacheKey = String.format("products:trashed:page:%d:search:%s", page, keyword);

        return redisService.get(cacheKey)
                .compose(cached -> {
                    span.setAttribute("cache.hit", cached != null);
                    return handleCacheOrRepo(
                            cached, cacheKey,
                            () -> repository.getProductsTrashed(req),
                            new TypeReference<PagedResult<Product>>() {
                            },
                            result -> mapToPagedResponseDeleteAt(result, req),
                            tracingContext, "get_trashed", Duration.ofMinutes(10));
                })
                .map(response -> {
                    span.setAttribute("records.count", response.data().size());
                    span.setAttribute("records.total", response.pagination().totalRecords());
                    return response;
                })
                .recover(err -> {
                    logger.error("Failed to fetch trashed products", err);
                    tracingMetrics.completeSpanError(tracingContext, "get_trashed", err.getMessage());
                    return Future
                            .succeededFuture(ApiResponsePagination.error("Failed to fetch data: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponsePagination<List<ProductResponse>>> getByMerchant(ProductByMerchantRequest req) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("ProductQueryService.getByMerchant");
        Span span = Span.fromContext(tracingContext.getContext());
        span.setAttribute("merchant.id", req.getMerchantId() != null ? req.getMerchantId().longValue() : 0);

        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() != null ? req.getPageSize() : 10;

        req.setPage(page);
        req.setPageSize(pageSize);

        String cacheKey = String.format("products:merchant:%d:search:%s:cat:%s:min:%s:max:%s:page:%d:size:%d",
                req.getMerchantId(), req.getSearch(), req.getCategoryId(), req.getMinPrice(), req.getMaxPrice(), page,
                pageSize);

        return redisService.get(cacheKey)
                .compose(cached -> {
                    span.setAttribute("cache.hit", cached != null);
                    return handleCacheOrRepo(
                            cached, cacheKey,
                            () -> repository.getProductsByMerchant(req),
                            new TypeReference<PagedResult<Product>>() {
                            },
                            result -> mapToPagedResponse(result, page, pageSize),
                            tracingContext, "get_by_merchant", Duration.ofMinutes(10));
                })
                .map(response -> {
                    span.setAttribute("records.count", response.data().size());
                    span.setAttribute("records.total", response.pagination().totalRecords());
                    return response;
                })
                .recover(err -> {
                    logger.error("Failed to fetch products by merchant", err);
                    tracingMetrics.completeSpanError(tracingContext, "get_by_merchant", err.getMessage());
                    return Future
                            .succeededFuture(ApiResponsePagination.error("Failed to fetch data: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponsePagination<List<ProductResponse>>> getByCategoryName(ProductByCategoryRequest req) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics
                .startSpan("ProductQueryService.getByCategoryName");
        Span span = Span.fromContext(tracingContext.getContext());

        int page = req.getPage() != null && req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() != null ? req.getPageSize() : 10;

        req.setPage(page);
        req.setPageSize(pageSize);

        String cacheKey = String.format("products:category:%s:search:%s:min:%s:max:%s:page:%d:size:%d",
                req.getCategoryName(), req.getSearch(), req.getMinPrice(), req.getMaxPrice(), page, pageSize);

        return redisService.get(cacheKey)
                .compose(cached -> {
                    span.setAttribute("cache.hit", cached != null);
                    return handleCacheOrRepo(
                            cached, cacheKey,
                            () -> repository.getProductsByCategoryName(req),
                            new TypeReference<PagedResult<Product>>() {
                            },
                            result -> mapToPagedResponse(result, page, pageSize),
                            tracingContext, "get_by_category", Duration.ofMinutes(10));
                })
                .map(response -> {
                    span.setAttribute("records.count", response.data().size());
                    span.setAttribute("records.total", response.pagination().totalRecords());
                    return response;
                })
                .recover(err -> {
                    logger.error("Failed to fetch products by category", err);
                    tracingMetrics.completeSpanError(tracingContext, "get_by_category", err.getMessage());
                    return Future
                            .succeededFuture(ApiResponsePagination.error("Failed to fetch data: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<ProductResponse>> getById(Long id) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan(
                "ProductQueryService.getById",
                io.opentelemetry.api.common.Attributes.builder().put("id", id).build());
        Span span = Span.fromContext(tracingContext.getContext());

        logger.info("Fetching product by id: {}", id);
        String cacheKey = "product:" + id;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    span.setAttribute("cache.hit", cached != null);
                    return handleCacheOrRepo(
                            cached, cacheKey,
                            () -> repository.getProductById(id).map(data -> {
                                if (data == null)
                                    throw new NotFoundException("Product not found");
                                return data;
                            }),
                            new TypeReference<Product>() {
                            },
                            data -> ApiResponse.success("Success", ProductResponse.from(data)),
                            tracingContext, "get_by_id", Duration.ofMinutes(60));
                })
                .recover(err -> {
                    logger.error("Failed to fetch by id", err);
                    tracingMetrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error(err.getMessage()));
                });
    }

    private <T, R> Future<R> handleCacheOrRepo(String cached, String cacheKey,
            java.util.concurrent.Callable<Future<T>> repoCall, TypeReference<T> typeRef,
            java.util.function.Function<T, R> mapper,
            TracingMetrics.TracingContext tracingCtx, String operation, Duration ttl) {

        if (cached != null) {
            try {
                T data = objectMapper.readValue(cached, typeRef);
                tracingMetrics.completeSpanSuccess(tracingCtx, operation, "Success");
                return Future.succeededFuture(mapper.apply(data));
            } catch (Exception e) {
                logger.warn("Cache parse error", e);
            }
        }

        try {
            return repoCall.call().map(res -> {
                redisService.set(cacheKey, Json.encode(res), ttl);
                tracingMetrics.completeSpanSuccess(tracingCtx, operation, "Success");
                return mapper.apply(res);
            });
        } catch (Exception e) {
            return Future.failedFuture(e);
        }
    }

    private ApiResponsePagination<List<ProductResponse>> mapToPagedResponse(PagedResult<Product> result,
            FindAllProducts req) {
        return mapToPagedResponse(result, req.getPage(), req.getPageSize());
    }

    private ApiResponsePagination<List<ProductResponse>> mapToPagedResponse(PagedResult<Product> result, int page,
            int pageSize) {
        List<ProductResponse> data = result.getData().stream()
                .map(ProductResponse::from)
                .collect(Collectors.toList());
        return new ApiResponsePagination<>(
                "success", "Data fetched", data,
                new PaginationMeta(page, pageSize,
                        (int) Math.ceil((double) result.getTotalRecords() / pageSize),
                        result.getTotalRecords()));
    }

    private ApiResponsePagination<List<ProductResponseDeleteAt>> mapToPagedResponseDeleteAt(PagedResult<Product> result,
            FindAllProducts req) {
        List<ProductResponseDeleteAt> data = result.getData().stream()
                .map(ProductResponseDeleteAt::from)
                .collect(Collectors.toList());
        return new ApiResponsePagination<>(
                "success", "Data fetched", data,
                new PaginationMeta(req.getPage(), req.getPageSize(),
                        (int) Math.ceil((double) result.getTotalRecords() / req.getPageSize()),
                        result.getTotalRecords()));
    }
}
