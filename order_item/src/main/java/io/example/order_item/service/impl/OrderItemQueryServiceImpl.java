package io.example.order_item.service.impl;

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
import io.example.order_item.domain.requests.FindAllOrderItems;
import io.example.order_item.domain.response.order_item.OrderItemResponse;
import io.example.order_item.domain.response.order_item.OrderItemResponseDeleteAt;
import io.example.order_item.model.OrderItem;
import io.example.order_item.repository.OrderItemQueryRepository;
import io.example.order_item.service.OrderItemQueryService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderItemQueryServiceImpl implements OrderItemQueryService {
    private static final Logger log = LoggerFactory.getLogger(OrderItemQueryServiceImpl.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final OrderItemQueryRepository queryRepository;
    private final RedisService redis;
    private final TracingMetrics metrics;

    private static final String CACHE_PREFIX = "order_item:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private PagedResult<OrderItemResponse> mapPagination(PagedResult<OrderItem> res) {
        List<OrderItemResponse> data = res.getData().stream().map(OrderItemResponse::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    private PagedResult<OrderItemResponseDeleteAt> mapPaginationDeleteAt(PagedResult<OrderItem> res) {
        List<OrderItemResponseDeleteAt> data = res.getData().stream().map(OrderItemResponseDeleteAt::from).toList();
        return new PagedResult<>(data, res.getTotalRecords());
    }

    private void normalizeRequest(FindAllOrderItems req) {
        int page = req.getPage() > 0 ? req.getPage() - 1 : 0;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        req.setPage(page);
        req.setPageSize(pageSize);
        req.setSearch(keyword);
    }

    @Override
    public Future<PagedResult<OrderItemResponse>> getAll(FindAllOrderItems req) {
        normalizeRequest(req);
        var ctx = metrics.startSpan("OrderItemQueryService.getAll");
        String cacheKey = CACHE_PREFIX + "list:all:" + req.getSearch() + ":" + req.getPage() + ":" + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<OrderItem> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<OrderItem>>() {
                                    });
                            return Future.succeededFuture(mapPagination(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached order items: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getOrderItems(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPagination);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getAll", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getAll", e.getMessage()));
    }

    @Override
    public Future<PagedResult<OrderItemResponseDeleteAt>> getActive(FindAllOrderItems req) {
        normalizeRequest(req);
        var ctx = metrics.startSpan("OrderItemQueryService.getActive");
        String cacheKey = CACHE_PREFIX + "list:active:" + req.getSearch() + ":" + req.getPage() + ":"
                + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<OrderItem> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<OrderItem>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached active order items: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getOrderItemsActive(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPaginationDeleteAt);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getActive", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getActive", e.getMessage()));
    }

    @Override
    public Future<PagedResult<OrderItemResponseDeleteAt>> getTrashed(FindAllOrderItems req) {
        normalizeRequest(req);
        var ctx = metrics.startSpan("OrderItemQueryService.getTrashed");
        String cacheKey = CACHE_PREFIX + "list:trashed:" + req.getSearch() + ":" + req.getPage() + ":"
                + req.getPageSize();

        return redis.get(cacheKey)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            PagedResult<OrderItem> typedCached = mapper.readValue(jsonStr,
                                    new TypeReference<PagedResult<OrderItem>>() {
                                    });
                            return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached trashed order items: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getOrderItemsTrashed(req)
                            .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL).map(v -> res))
                            .map(this::mapPaginationDeleteAt);
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTrashed", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getTrashed", e.getMessage()));
    }

    @Override
    public Future<List<OrderItemResponse>> getByOrderId(Integer orderId) {
        var ctx = metrics.startSpan("OrderItemQueryService.getByOrderId",
                Attributes.builder().put("order.id", orderId).build());
        String key = CACHE_PREFIX + "order:" + orderId;

        return redis.get(key)
                .compose(jsonStr -> {
                    if (jsonStr != null && !jsonStr.isEmpty()) {
                        try {
                            List<OrderItem> cachedList = mapper.readValue(jsonStr,
                                    new TypeReference<List<OrderItem>>() {
                                    });
                            List<OrderItemResponse> responseList = cachedList.stream().map(OrderItemResponse::from)
                                    .toList();
                            return Future.succeededFuture(responseList);
                        } catch (Exception e) {
                            log.warn("Failed to deserialize cached order items by order: {}", e.getMessage());
                        }
                    }
                    return queryRepository.getOrderItemsByOrder((long) orderId)
                            .compose(dbList -> {
                                if (dbList == null || dbList.isEmpty()) {
                                    return Future.failedFuture(
                                            new NotFoundException("Order items not found for order id: " + orderId));
                                }
                                return redis.setJson(key, dbList, CACHE_TTL)
                                        .map(v -> dbList.stream().map(OrderItemResponse::from).toList());
                            });
                })
                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getByOrderId", "Success"))
                .onFailure(e -> metrics.completeSpanError(ctx, "getByOrderId", e.getMessage()));
    }
}