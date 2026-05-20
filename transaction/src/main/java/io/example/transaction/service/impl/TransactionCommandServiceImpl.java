package io.example.transaction.service.impl;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.example.common.exception.NotFoundException;
import io.example.common.model.ApiResponse;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.model.Transaction;
import io.example.transaction.model.TransactionResponse;
import io.example.transaction.model.TransactionResponseDeleteAt;
import io.example.transaction.repository.TransactionCommandRepository;
import io.example.transaction.service.TransactionCommandService;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import pb.transaction.TransactionCommand.CreateTransactionRequest;
import pb.transaction.TransactionCommand.UpdateTransactionRequest;
import pb.transaction.Transaction.FindByIdTransactionRequest;

public class TransactionCommandServiceImpl implements TransactionCommandService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionCommandServiceImpl.class);

    private final TransactionCommandRepository repo;
    private final RedisService redis;
    private final TracingMetrics metrics;

    private static final String CACHE_PREFIX = "transaction:";

    public TransactionCommandServiceImpl(
            TransactionCommandRepository repo,
            RedisService redis,
            TracingMetrics metrics) {
        this.repo = repo;
        this.redis = redis;
        this.metrics = metrics;
    }

    @Override
    public Future<ApiResponse<TransactionResponse>> createTransaction(CreateTransactionRequest req) {
        String card = req.getCardNumber();
        int amount = req.getAmount();
        String paymentMethod = req.getPaymentMethod();
        int merchantId = req.getMerchantId();

        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "TransactionCommandService.createTransaction",
                Attributes.builder()
                        .put("card.number", Objects.requireNonNull(card))
                        .put("merchant.id", (long) merchantId)
                        .build());
        Span span = Span.fromContext(Objects.requireNonNull(tracingContext.getContext()));

        logger.info("Creating transaction for card: {}, amount: {}", card, amount);

        return repo.createTransaction(card, amount, paymentMethod, merchantId, "success")
                .map(created -> {
                    span.setAttribute("transaction.id", created.getTransactionId());
                    metrics.completeSpanSuccess(tracingContext, "create", "Transaction created successfully");
                    return ApiResponse.success("Transaction created successfully", TransactionResponse.from(created));
                })
                .recover(err -> {
                    logger.error("Failed to create transaction: card={}", card, err);
                    metrics.completeSpanError(tracingContext, "create", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to create transaction: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<TransactionResponse>> updateTransaction(UpdateTransactionRequest req) {
        long id = req.getTransactionId();
        String card = req.getCardNumber();
        int amount = req.getAmount();
        String paymentMethod = req.getPaymentMethod();
        int merchantId = req.getMerchantId();

        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "TransactionCommandService.updateTransaction",
                Attributes.builder()
                        .put("transaction.id", id)
                        .put("card.number", card)
                        .put("merchant.id", (long) merchantId)
                        .build());

        logger.info("Updating transaction: {}, card: {}, amount: {}", id, card, amount);

        return repo.updateTransaction(id, card, amount, paymentMethod, merchantId, "success")
                .compose(updated -> {
                    if (updated == null) {
                        return Future.failedFuture(new NotFoundException("Transaction not found"));
                    }
                    String cacheKey = CACHE_PREFIX + "id:" + id;
                    return redis.delete(cacheKey).map(updated);
                })
                .map(updated -> {
                    metrics.completeSpanSuccess(tracingContext, "update", "Transaction updated successfully");
                    return ApiResponse.success("Transaction updated successfully", TransactionResponse.from(updated));
                })
                .recover(err -> {
                    logger.error("Failed to update transaction: {}", id, err);
                    metrics.completeSpanError(tracingContext, "update", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to update transaction: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<TransactionResponseDeleteAt>> trashTransaction(FindByIdTransactionRequest req) {
        long id = req.getTransactionId();
        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "TransactionCommandService.trashTransaction",
                Attributes.builder().put("transaction.id", id).build());

        logger.info("Trashing transaction: {}", id);

        return repo.trashTransaction(id)
                .compose(tx -> {
                    if (tx == null) {
                        return Future.failedFuture(new NotFoundException("Transaction not found"));
                    }
                    String cacheKey = CACHE_PREFIX + "id:" + id;
                    return redis.delete(cacheKey).map(tx);
                })
                .map(tx -> {
                    metrics.completeSpanSuccess(tracingContext, "trash", "Transaction trashed successfully");
                    return ApiResponse.success("Transaction trashed successfully", TransactionResponseDeleteAt.from(tx));
                })
                .recover(err -> {
                    logger.error("Failed to trash transaction: {}", id, err);
                    metrics.completeSpanError(tracingContext, "trash", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to trash transaction: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<TransactionResponseDeleteAt>> restoreTransaction(FindByIdTransactionRequest req) {
        long id = req.getTransactionId();
        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "TransactionCommandService.restoreTransaction",
                Attributes.builder().put("transaction.id", id).build());

        logger.info("Restoring transaction: {}", id);

        return repo.restoreTransaction(id)
                .compose(tx -> {
                    if (tx == null) {
                        return Future.failedFuture(new NotFoundException("Transaction not found"));
                    }
                    String cacheKey = CACHE_PREFIX + "id:" + id;
                    return redis.delete(cacheKey).map(tx);
                })
                .map(tx -> {
                    metrics.completeSpanSuccess(tracingContext, "restore", "Transaction restored successfully");
                    return ApiResponse.success("Transaction restored successfully", TransactionResponseDeleteAt.from(tx));
                })
                .recover(err -> {
                    logger.error("Failed to restore transaction: {}", id, err);
                    metrics.completeSpanError(tracingContext, "restore", err.getMessage());
                    return Future.succeededFuture(ApiResponse.error("Failed to restore transaction: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<Void>> deletePermanent(FindByIdTransactionRequest req) {
        long id = req.getTransactionId();
        TracingMetrics.TracingContext tracingContext = metrics.startSpan(
                "TransactionCommandService.deletePermanent",
                Attributes.builder().put("transaction.id", id).build());

        logger.info("Permanently deleting transaction: {}", id);

        return repo.deleteTransactionPermanently(id)
                .compose(v -> {
                    String cacheKey = CACHE_PREFIX + "id:" + id;
                    return redis.delete(cacheKey).mapEmpty();
                })
                .map(v -> {
                    metrics.completeSpanSuccess(tracingContext, "delete_permanent", "Transaction permanently deleted");
                    return ApiResponse.<Void>success("Transaction permanently deleted successfully");
                })
                .recover(err -> {
                    logger.error("Failed to permanently delete transaction: {}", id, err);
                    metrics.completeSpanError(tracingContext, "delete_permanent", err.getMessage());
                    return Future.succeededFuture(ApiResponse.<Void>error("Failed to permanently delete transaction: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<Void>> restoreAllTransactions() {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan("TransactionCommandService.restoreAllTransactions");
        logger.info("Restoring all transactions");

        return repo.restoreAllTransactions()
                .map(count -> {
                    metrics.completeSpanSuccess(tracingContext, "restore_all", "All transactions restored");
                    return ApiResponse.<Void>success("All transactions restored successfully");
                })
                .recover(err -> {
                    logger.error("Failed to restore all transactions", err);
                    metrics.completeSpanError(tracingContext, "restore_all", err.getMessage());
                    return Future.succeededFuture(ApiResponse.<Void>error("Failed to restore all transactions: " + err.getMessage()));
                });
    }

    @Override
    public Future<ApiResponse<Void>> deleteAllPermanentTransactions() {
        TracingMetrics.TracingContext tracingContext = metrics.startSpan("TransactionCommandService.deleteAllPermanentTransactions");
        logger.info("Permanently deleting all transactions");

        return repo.deleteAllPermanentTransactions()
                .map(count -> {
                    metrics.completeSpanSuccess(tracingContext, "delete_all_permanent", "All permanent transactions deleted");
                    return ApiResponse.<Void>success("All permanent transactions deleted successfully");
                })
                .recover(err -> {
                    logger.error("Failed to permanently delete all transactions", err);
                    metrics.completeSpanError(tracingContext, "delete_all_permanent", err.getMessage());
                    return Future.succeededFuture(ApiResponse.<Void>error("Failed to permanently delete all transactions: " + err.getMessage()));
                });
    }
}
