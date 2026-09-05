package io.example.transaction.service.impl;

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
import io.example.transaction.domain.requests.transactions.FindAllTransactionByMerchantRequest;
import io.example.transaction.domain.requests.transactions.FindAllTransactionRequest;
import io.example.transaction.domain.response.TransactionResponse;
import io.example.transaction.domain.response.TransactionResponseDeleteAt;
import io.example.transaction.model.Transaction;
import io.example.transaction.repository.TransactionQueryRepository;
import io.example.transaction.service.TransactionQueryService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransactionQueryServiceImpl implements TransactionQueryService {
        private static final Logger log = LoggerFactory.getLogger(TransactionQueryServiceImpl.class);
        private static final ObjectMapper mapper = new ObjectMapper();

        private final TransactionQueryRepository queryRepository;
        private final RedisService redis;
        private final TracingMetrics metrics;

        private static final String CACHE_PREFIX = "transaction:";
        private static final Duration CACHE_TTL = Duration.ofMinutes(10);

        private PagedResult<TransactionResponse> mapPagination(PagedResult<Transaction> res) {
                List<TransactionResponse> data = res.getData().stream().map(TransactionResponse::from).toList();
                return new PagedResult<>(data, res.getTotalRecords());
        }

        private PagedResult<TransactionResponseDeleteAt> mapPaginationDeleteAt(PagedResult<Transaction> res) {
                List<TransactionResponseDeleteAt> data = res.getData().stream().map(TransactionResponseDeleteAt::from)
                                .toList();
                return new PagedResult<>(data, res.getTotalRecords());
        }

        @Override
        public Future<PagedResult<TransactionResponse>> findAllTransaction(FindAllTransactionRequest req) {
                var ctx = metrics.startSpan("TransactionQueryService.findAllTransaction");
                String cacheKey = CACHE_PREFIX + "list:all:" + req.getSearch() + ":" + req.getPage() + ":"
                                + req.getPageSize();

                return redis.get(cacheKey)
                                .compose(jsonStr -> {
                                        if (jsonStr != null && !jsonStr.isEmpty()) {
                                                try {
                                                        PagedResult<Transaction> typedCached = mapper.readValue(jsonStr,
                                                                        new TypeReference<PagedResult<Transaction>>() {
                                                                        });
                                                        return Future.succeededFuture(mapPagination(typedCached));
                                                } catch (Exception e) {
                                                        log.warn("Failed to deserialize cached transactions: {}",
                                                                        e.getMessage());
                                                }
                                        }
                                        return queryRepository.getTransactions(req)
                                                        .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL)
                                                                        .map(v -> res))
                                                        .map(this::mapPagination);
                                })
                                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "findAllTransaction", "Success"))
                                .onFailure(e -> metrics.completeSpanError(ctx, "findAllTransaction", e.getMessage()));
        }

        @Override
        public Future<PagedResult<TransactionResponse>> findAllTransactionByMerchant(
                        FindAllTransactionByMerchantRequest req) {
                var ctx = metrics.startSpan("TransactionQueryService.findAllTransactionByMerchant");

                String cacheKey = CACHE_PREFIX + "list:merchant:" + req.getMerchantId() + ":"
                                + req.getSearch() + ":" + req.getPage() + ":" + req.getPageSize();

                return redis.get(cacheKey)
                                .compose(jsonStr -> {
                                        if (jsonStr != null && !jsonStr.isEmpty()) {
                                                try {
                                                        PagedResult<Transaction> typedCached = mapper.readValue(jsonStr,
                                                                        new TypeReference<PagedResult<Transaction>>() {
                                                                        });
                                                        return Future.succeededFuture(mapPagination(typedCached));
                                                } catch (Exception e) {
                                                        log.warn("Failed to deserialize cached transactions by merchant: {}",
                                                                        e.getMessage());
                                                }
                                        }
                                        return queryRepository.getTransactionByMerchant(req)
                                                        .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL)
                                                                        .map(v -> res))
                                                        .map(this::mapPagination);
                                })
                                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "findAllTransactionByMerchant",
                                                "Success"))
                                .onFailure(e -> metrics.completeSpanError(ctx, "findAllTransactionByMerchant",
                                                e.getMessage()));
        }

        @Override
        public Future<TransactionResponse> findByIdTransaction(Long req) {
                var ctx = metrics.startSpan("TransactionQueryService.findByIdTransaction",
                                Attributes.builder().put("transaction.id", req).build());
                String key = CACHE_PREFIX + "id:" + req;

                return redis.getJson(key, Transaction.class)
                                .compose(cached -> {
                                        if (cached != null) {
                                                return Future.succeededFuture(TransactionResponse.from(cached));
                                        }
                                        return queryRepository.getTransactionById(req)
                                                        .compose(db -> {
                                                                if (db == null) {
                                                                        return Future.<Transaction>failedFuture(
                                                                                        new NotFoundException(
                                                                                                        "Transaction not found"));
                                                                }
                                                                return redis.setJson(key, db, CACHE_TTL)
                                                                                .<Transaction>map(v -> db);
                                                        })
                                                        .map(TransactionResponse::from);
                                })
                                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "findByIdTransaction", "Success"))
                                .onFailure(e -> metrics.completeSpanError(ctx, "findByIdTransaction", e.getMessage()));
        }

        @Override
        public Future<PagedResult<TransactionResponseDeleteAt>> findByActiveTransaction(
                        FindAllTransactionRequest req) {
                var ctx = metrics.startSpan("TransactionQueryService.findByActiveTransaction");
                String cacheKey = CACHE_PREFIX + "list:active:" + req.getSearch() + ":" + req.getPage() + ":"
                                + req.getPageSize();

                return redis.get(cacheKey)
                                .compose(jsonStr -> {
                                        if (jsonStr != null && !jsonStr.isEmpty()) {
                                                try {
                                                        PagedResult<Transaction> typedCached = mapper.readValue(jsonStr,
                                                                        new TypeReference<PagedResult<Transaction>>() {
                                                                        });
                                                        return Future.succeededFuture(
                                                                        mapPaginationDeleteAt(typedCached));
                                                } catch (Exception e) {
                                                        log.warn("Failed to deserialize cached active transactions: {}",
                                                                        e.getMessage());
                                                }
                                        }
                                        return queryRepository.getTransactionsActive(req)
                                                        .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL)
                                                                        .map(v -> res))
                                                        .map(this::mapPaginationDeleteAt);
                                })
                                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "findByActiveTransaction", "Success"))
                                .onFailure(e -> metrics.completeSpanError(ctx, "findByActiveTransaction",
                                                e.getMessage()));
        }

        @Override
        public Future<PagedResult<TransactionResponseDeleteAt>> findByTrashedTransaction(
                        FindAllTransactionRequest req) {
                var ctx = metrics.startSpan("TransactionQueryService.findByTrashedTransaction");
                String cacheKey = CACHE_PREFIX + "list:trashed:" + req.getSearch() + ":" + req.getPage() + ":"
                                + req.getPageSize();

                return redis.get(cacheKey)
                                .compose(jsonStr -> {
                                        if (jsonStr != null && !jsonStr.isEmpty()) {
                                                try {
                                                        PagedResult<Transaction> typedCached = mapper.readValue(jsonStr,
                                                                        new TypeReference<PagedResult<Transaction>>() {
                                                                        });
                                                        return Future.succeededFuture(
                                                                        mapPaginationDeleteAt(typedCached));
                                                } catch (Exception e) {
                                                        log.warn("Failed to deserialize cached trashed transactions: {}",
                                                                        e.getMessage());
                                                }
                                        }
                                        return queryRepository.getTransactionsTrashed(req)
                                                        .compose(res -> redis.setJson(cacheKey, res, CACHE_TTL)
                                                                        .map(v -> res))
                                                        .map(this::mapPaginationDeleteAt);
                                })
                                .onSuccess(r -> metrics.completeSpanSuccess(ctx, "findByTrashedTransaction", "Success"))
                                .onFailure(e -> metrics.completeSpanError(ctx, "findByTrashedTransaction",
                                                e.getMessage()));
        }
}