package io.example.order_item.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.example.common.domain.PagedResult;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.order_item.domain.requests.FindAllOrderItems;
import io.example.order_item.model.OrderItem;
import io.example.order_item.model.OrderItemResponse;
import io.example.order_item.model.OrderItemResponseDeleteAt;
import io.example.order_item.repository.OrderItemQueryRepository;
import io.example.order_item.service.OrderItemQueryService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class OrderItemQueryServiceImpl implements OrderItemQueryService {
    private final OrderItemQueryRepository queryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderItemQueryServiceImpl(
            OrderItemQueryRepository queryRepository,
            RedisService redisService,
            TracingMetrics tracingMetrics) {
        this.queryRepository = queryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<ApiResponsePagination<List<OrderItemResponse>>> getAll(FindAllOrderItems req) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("OrderItemService.getAll");
        Span span = Span.fromContext(tracingContext.getContext());

        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSearch(keyword);

        log.info("Fetching all order items | search={}, page={}, pageSize={}", keyword, page, pageSize);

        String cacheKey = String.format("order_items:page:%d:search:%s", page, keyword);

        return redisService.get(cacheKey)
                .compose(cached -> {
                    span.setAttribute("cache.hit", cached != null);
                    return handleCacheOrRepo(
                            cached,
                            cacheKey,
                            () -> queryRepository.getOrderItems(req),
                            new TypeReference<PagedResult<OrderItem>>() {
                            },
                            result -> mapToPagedResponse(result, req),
                            tracingContext,
                            "get_all",
                            Duration.ofMinutes(10));
                })
                .map(response -> {
                    span.setAttribute("records.count", response.data() != null ? response.data().size() : 0);
                    span.setAttribute("records.total", response.pagination() != null ? response.pagination().totalRecords() : 0);
                    return response;
                })
                .recover(err -> {
                    log.error("Failed to fetch order items", err);
                    tracingMetrics.completeSpanError(tracingContext, "get_all", err.getMessage());
                    return Future.succeededFuture(
                            ApiResponsePagination.error("Failed to fetch data: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponsePagination<List<OrderItemResponseDeleteAt>>> getActive(FindAllOrderItems req) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("OrderItemService.getActive");
        Span span = Span.fromContext(tracingContext.getContext());

        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSearch(keyword);

        log.info("Fetching active order items | search={}, page={}, pageSize={}", keyword, page, pageSize);

        String cacheKey = String.format("order_items:active:page:%d:search:%s", page, keyword);

        return redisService.get(cacheKey)
                .compose(cached -> {
                    span.setAttribute("cache.hit", cached != null);
                    return handleCacheOrRepo(
                            cached,
                            cacheKey,
                            () -> queryRepository.getOrderItemsActive(req),
                            new TypeReference<PagedResult<OrderItem>>() {
                            },
                            result -> mapToPagedResponseDeleteAt(result, req),
                            tracingContext,
                            "get_active",
                            Duration.ofMinutes(10));
                })
                .map(response -> {
                    span.setAttribute("records.count", response.data() != null ? response.data().size() : 0);
                    span.setAttribute("records.total", response.pagination() != null ? response.pagination().totalRecords() : 0);
                    return response;
                })
                .recover(err -> {
                    log.error("Failed to fetch active order items", err);
                    tracingMetrics.completeSpanError(tracingContext, "get_active", err.getMessage());
                    return Future.succeededFuture(
                            ApiResponsePagination.error("Failed to fetch data: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponsePagination<List<OrderItemResponseDeleteAt>>> getTrashed(FindAllOrderItems req) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("OrderItemService.getTrashed");
        Span span = Span.fromContext(tracingContext.getContext());

        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSearch(keyword);

        log.info("Fetching trashed order items | search={}, page={}, pageSize={}", keyword, page, pageSize);

        String cacheKey = String.format("order_items:trashed:page:%d:search:%s", page, keyword);

        return redisService.get(cacheKey)
                .compose(cached -> {
                    span.setAttribute("cache.hit", cached != null);
                    return handleCacheOrRepo(
                            cached,
                            cacheKey,
                            () -> queryRepository.getOrderItemsTrashed(req),
                            new TypeReference<PagedResult<OrderItem>>() {
                            },
                            result -> mapToPagedResponseDeleteAt(result, req),
                            tracingContext,
                            "get_trashed",
                            Duration.ofMinutes(10));
                })
                .map(response -> {
                    span.setAttribute("records.count", response.data() != null ? response.data().size() : 0);
                    span.setAttribute("records.total", response.pagination() != null ? response.pagination().totalRecords() : 0);
                    return response;
                })
                .recover(err -> {
                    log.error("Failed to fetch trashed order items", err);
                    tracingMetrics.completeSpanError(tracingContext, "get_trashed", err.getMessage());
                    return Future.succeededFuture(
                            ApiResponsePagination.error("Failed to fetch data: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<List<OrderItemResponse>>> getByOrderId(Integer orderId) {
        TracingMetrics.TracingContext tracingContext = tracingMetrics.startSpan("OrderItemService.getByOrderId");
        Span span = Span.fromContext(tracingContext.getContext());
        span.setAttribute("order.id", orderId);

        String cacheKey = String.format("order_items:order:%d", orderId);

        return redisService.get(cacheKey)
                .compose(cached -> {
                    span.setAttribute("cache.hit", cached != null);
                    return handleCacheOrRepo(
                            cached,
                            cacheKey,
                            () -> queryRepository.getOrderItemsByOrder(orderId.longValue()),
                            new TypeReference<List<OrderItem>>() {
                            },
                            items -> {
                                List<OrderItemResponse> data = items.stream()
                                        .map(OrderItemResponse::from)
                                        .collect(Collectors.toList());
                                return ApiResponse.success("Order items fetched", data);
                            },
                            tracingContext,
                            "get_by_order",
                            Duration.ofMinutes(10));
                })
                .recover(err -> {
                    log.error("Failed to fetch items for order {}", orderId, err);
                    tracingMetrics.completeSpanError(tracingContext, "get_by_order", err.getMessage());
                    return Future.succeededFuture(
                            ApiResponse.error("Failed to fetch items: " + err.getMessage()));
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
                log.warn("Cache parse error", e);
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

    private ApiResponsePagination<List<OrderItemResponse>> mapToPagedResponse(PagedResult<OrderItem> result,
                                                                              FindAllOrderItems req) {
        List<OrderItemResponse> data = result.getData().stream()
                .map(OrderItemResponse::from)
                .collect(Collectors.toList());
        return ApiResponsePagination.success(
                "Data fetched", data,
                new PaginationMeta(req.getPage() + 1, req.getPageSize(),
                        (int) Math.ceil((double) result.getTotalRecords() / req.getPageSize()),
                        result.getTotalRecords()));
    }

    private ApiResponsePagination<List<OrderItemResponseDeleteAt>> mapToPagedResponseDeleteAt(
            PagedResult<OrderItem> result, FindAllOrderItems req) {
        List<OrderItemResponseDeleteAt> data = result.getData().stream()
                .map(OrderItemResponseDeleteAt::from)
                .collect(Collectors.toList());
        return ApiResponsePagination.success(
                "Data fetched", data,
                new PaginationMeta(req.getPage() + 1, req.getPageSize(),
                        (int) Math.ceil((double) result.getTotalRecords() / req.getPageSize()),
                        result.getTotalRecords()));
    }
}
