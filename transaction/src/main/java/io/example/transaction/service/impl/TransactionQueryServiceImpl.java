package io.example.transaction.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.example.common.domain.PagedResult;
import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.model.ApiResponsePagination;
import io.example.common.model.PaginationMeta;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.model.Transaction;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.example.transaction.repository.TransactionQueryRepository;
import io.example.transaction.service.TransactionQueryService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import pb.transaction.TransactionQuery.FindAllTransactionRequest;
import pb.transaction.TransactionQuery.FindAllTransactionCardNumberRequest;
import pb.transaction.Transaction.FindByIdTransactionRequest;
import pb.transaction.TransactionQuery.FindTransactionByMerchantIdRequest;

public class TransactionQueryServiceImpl implements TransactionQueryService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionQueryServiceImpl.class);

    private final TransactionQueryRepository repo;
    private final RedisService redis;
    private final TracingMetrics metrics;

    private static final String CACHE_PREFIX = "transaction:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    public TransactionQueryServiceImpl(
            TransactionQueryRepository repo,
            RedisService redis,
            TracingMetrics metrics) {
        this.repo = repo;
        this.redis = redis;
        this.metrics = metrics;
    }

    @Override
    public Future<ApiResponsePagination<List<TransactionResponse>>> findAllTransaction(FindAllTransactionRequest req) {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan("TransactionQueryService.findAllTransaction");
        Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

        int page = req.getPage() > 0 ? req.getPage() : 1;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        String cacheKey = String.format("%sall:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

        return redis.getJson(cacheKey, ApiResponsePagination.class)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("transaction.cache_hit", true);
                        metrics.completeSpanSuccess(tracingContext, "get_all", "Transactions fetched from cache");
                        @SuppressWarnings("unchecked")
                        ApiResponsePagination<List<TransactionResponse>> typedCached = (ApiResponsePagination<List<TransactionResponse>>) cached;
                        return Future.succeededFuture(typedCached);
                    }
                    span.setAttribute("transaction.cache_hit", false);
                    return repo.getTransactions(keyword, page, pageSize)
                            .map(result -> mapTransactionPagination(result, page, pageSize))
                            .compose(response -> redis.setJson(cacheKey, response, CACHE_TTL).map(response));
                })
                .onSuccess(response -> {
                    span.setAttribute("transactions.count", (long) response.data().size());
                    span.setAttribute("transactions.total_records", (long) response.pagination().totalRecords());
                    metrics.completeSpanSuccess(tracingContext, "get_all", "Transactions fetched successfully");
                })
                .recover(throwable -> {
                    logger.error("Failed to fetch transactions", throwable);
                    metrics.completeSpanError(tracingContext, "get_all", throwable.getMessage());
                    return Future.succeededFuture(ApiResponsePagination.error("Failed to fetch transactions: " + throwable.getMessage()));
                });
    }

    @Override
    public Future<ApiResponsePagination<List<TransactionResponse>>> findAllTransactionByCardNumber(FindAllTransactionCardNumberRequest req) {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan("TransactionQueryService.findAllTransactionByCardNumber");
        Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

        int page = req.getPage() > 0 ? req.getPage() : 1;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";
        String card = req.getCardNumber();

        String cacheKey = String.format("%scard:%s:p:%d:s:%d:k:%s", CACHE_PREFIX, card, page, pageSize, keyword);

        return redis.getJson(cacheKey, ApiResponsePagination.class)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("transaction.cache_hit", true);
                        metrics.completeSpanSuccess(tracingContext, "get_by_card", "Transactions fetched from cache");
                        @SuppressWarnings("unchecked")
                        ApiResponsePagination<List<TransactionResponse>> typedCached = (ApiResponsePagination<List<TransactionResponse>>) cached;
                        return Future.succeededFuture(typedCached);
                    }
                    span.setAttribute("transaction.cache_hit", false);
                    return repo.getTransactionsByCardNumber(card, keyword, page, pageSize)
                            .map(result -> mapTransactionPagination(result, page, pageSize))
                            .compose(response -> redis.setJson(cacheKey, response, CACHE_TTL).map(response));
                })
                .onSuccess(response -> {
                    span.setAttribute("transactions.count", (long) response.data().size());
                    span.setAttribute("transactions.total_records", (long) response.pagination().totalRecords());
                    metrics.completeSpanSuccess(tracingContext, "get_by_card", "Transactions fetched successfully");
                })
                .recover(throwable -> {
                    logger.error("Failed to fetch transactions by card number", throwable);
                    metrics.completeSpanError(tracingContext, "get_by_card", throwable.getMessage());
                    return Future.succeededFuture(ApiResponsePagination.error("Failed to fetch transactions: " + throwable.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<TransactionResponse>> findByIdTransaction(FindByIdTransactionRequest req) {
        long id = req.getTransactionId();
        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "TransactionQueryService.findByIdTransaction",
                Attributes.builder().put("transaction.id", id).build());
        Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

        String cacheKey = CACHE_PREFIX + "id:" + id;

        return redis.getJson(cacheKey, Transaction.class)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("transaction.cache_hit", true);
                        metrics.completeSpanSuccess(tracingContext, "get_by_id", "Transaction fetched from cache");
                        return Future.succeededFuture(ApiResponse.success("Transaction fetched successfully (from cache)", TransactionResponse.from(cached)));
                    }
                    span.setAttribute("transaction.cache_hit", false);
                    return repo.getTransactionById(id)
                            .compose(tx -> {
                                if (tx == null) {
                                    return Future.failedFuture(new NotFoundException("Transaction not found"));
                                }
                                return redis.setJson(cacheKey, tx, CACHE_TTL).map(tx);
                            })
                            .map(tx -> {
                                metrics.completeSpanSuccess(tracingContext, "get_by_id", "Transaction fetched from database");
                                return ApiResponse.success("Transaction fetched successfully", TransactionResponse.from(tx));
                            });
                })
                .recover(err -> {
                    logger.error("Failed to fetch transaction by id: {}", id, err);
                    metrics.completeSpanError(tracingContext, "get_by_id", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to fetch transaction: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<List<TransactionResponse>>> findTransactionByMerchantId(FindTransactionByMerchantIdRequest req) {
        int merchantId = req.getMerchantId();
        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "TransactionQueryService.findTransactionByMerchantId",
                Attributes.builder().put("merchant.id", (long) merchantId).build());

        String cacheKey = CACHE_PREFIX + "merchant:" + merchantId;

        return redis.getJsonList(cacheKey, Transaction.class)
                .compose(cached -> {
                    if (cached != null && !cached.isEmpty()) {
                        metrics.completeSpanSuccess(tracingContext, "get_by_merchant", "Transactions fetched from cache");
                        List<TransactionResponse> responses = cached.stream().map(TransactionResponse::from).toList();
                        return Future.succeededFuture(ApiResponse.success("Transactions fetched successfully (from cache)", responses));
                    }
                    return repo.getTransactionsByMerchantId(merchantId)
                            .compose(list -> {
                                if (list == null || list.isEmpty()) {
                                    return Future.succeededFuture(List.<Transaction>of());
                                }
                                return redis.setJsonList(cacheKey, list, CACHE_TTL).map(list);
                            })
                            .map(list -> {
                                metrics.completeSpanSuccess(tracingContext, "get_by_merchant", "Transactions fetched from database");
                                List<TransactionResponse> responses = list.stream().map(TransactionResponse::from).toList();
                                return ApiResponse.success("Transactions fetched successfully", responses);
                            });
                })
                .recover(err -> {
                    logger.error("Failed to fetch transactions by merchant: {}", merchantId, err);
                    metrics.completeSpanError(tracingContext, "get_by_merchant", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to fetch transactions: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByActiveTransaction(FindAllTransactionRequest req) {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan("TransactionQueryService.findByActiveTransaction");
        Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

        int page = req.getPage() > 0 ? req.getPage() : 1;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        String cacheKey = String.format("%sactive:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

        return redis.getJson(cacheKey, ApiResponsePagination.class)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("transaction.cache_hit", true);
                        metrics.completeSpanSuccess(tracingContext, "get_active", "Active transactions fetched from cache");
                        @SuppressWarnings("unchecked")
                        ApiResponsePagination<List<TransactionResponseDeleteAt>> typedCached = (ApiResponsePagination<List<TransactionResponseDeleteAt>>) cached;
                        return Future.succeededFuture(typedCached);
                    }
                    span.setAttribute("transaction.cache_hit", false);
                    return repo.getTransactionsActive(keyword, page, pageSize)
                            .map(result -> mapTransactionPaginationDeleteAt(result, page, pageSize))
                            .compose(response -> redis.setJson(cacheKey, response, CACHE_TTL).map(response));
                })
                .onSuccess(response -> {
                    span.setAttribute("transactions.count", (long) response.data().size());
                    span.setAttribute("transactions.total_records", (long) response.pagination().totalRecords());
                    metrics.completeSpanSuccess(tracingContext, "get_active", "Active transactions fetched successfully");
                })
                .recover(throwable -> {
                    logger.error("Failed to fetch active transactions", throwable);
                    metrics.completeSpanError(tracingContext, "get_active", throwable.getMessage());
                    return Future.succeededFuture(ApiResponsePagination.error("Failed to fetch active transactions: " + throwable.getMessage()));
                });
    }

    @Override
    public Future<ApiResponsePagination<List<TransactionResponseDeleteAt>>> findByTrashedTransaction(FindAllTransactionRequest req) {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan("TransactionQueryService.findByTrashedTransaction");
        Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

        int page = req.getPage() > 0 ? req.getPage() : 1;
        int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
        String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

        String cacheKey = String.format("%strashed:p:%d:s:%d:k:%s", CACHE_PREFIX, page, pageSize, keyword);

        return redis.getJson(cacheKey, ApiResponsePagination.class)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("transaction.cache_hit", true);
                        metrics.completeSpanSuccess(tracingContext, "get_trashed", "Trashed transactions fetched from cache");
                        @SuppressWarnings("unchecked")
                        ApiResponsePagination<List<TransactionResponseDeleteAt>> typedCached = (ApiResponsePagination<List<TransactionResponseDeleteAt>>) cached;
                        return Future.succeededFuture(typedCached);
                    }
                    span.setAttribute("transaction.cache_hit", false);
                    return repo.getTransactionsTrashed(keyword, page, pageSize)
                            .map(result -> mapTransactionPaginationDeleteAt(result, page, pageSize))
                            .compose(response -> redis.setJson(cacheKey, response, CACHE_TTL).map(response));
                })
                .onSuccess(response -> {
                    span.setAttribute("transactions.count", (long) response.data().size());
                    span.setAttribute("transactions.total_records", (long) response.pagination().totalRecords());
                    metrics.completeSpanSuccess(tracingContext, "get_trashed", "Trashed transactions fetched successfully");
                })
                .recover(throwable -> {
                    logger.error("Failed to fetch trashed transactions", throwable);
                    metrics.completeSpanError(tracingContext, "get_trashed", throwable.getMessage());
                    return Future.succeededFuture(ApiResponsePagination.error("Failed to fetch trashed transactions: " + throwable.getMessage()));
                });
    }

    private ApiResponsePagination<List<TransactionResponse>> mapTransactionPagination(
            PagedResult<Transaction> result, int page, int pageSize) {
        int totalRecords = result.getTotalRecords();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        List<TransactionResponse> data = result.getData().stream().map(TransactionResponse::from).toList();
        return new ApiResponsePagination<>("success", "Transactions found", data, new PaginationMeta(page, pageSize, totalPages, totalRecords));
    }

    private ApiResponsePagination<List<TransactionResponseDeleteAt>> mapTransactionPaginationDeleteAt(
            PagedResult<Transaction> result, int page, int pageSize) {
        int totalRecords = result.getTotalRecords();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        List<TransactionResponseDeleteAt> data = result.getData().stream().map(TransactionResponseDeleteAt::from).toList();
        return new ApiResponsePagination<>("success", "Transactions found", data, new PaginationMeta(page, pageSize, totalPages, totalRecords));
    }
}
