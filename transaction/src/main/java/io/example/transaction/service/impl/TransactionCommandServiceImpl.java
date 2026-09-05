package io.example.transaction.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.transactions.CreateTransactionRequest;
import io.example.transaction.domain.requests.transactions.UpdateTransactionRequest;
import io.example.transaction.domain.response.TransactionResponse;
import io.example.transaction.domain.response.TransactionResponseDeleteAt;
import io.example.transaction.model.Transaction;
import io.example.transaction.repository.MerchantQueryRepository;
import io.example.transaction.repository.TransactionCommandRepository;
import io.example.transaction.repository.TransactionQueryRepository;
import io.example.transaction.service.TransactionCommandService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TransactionCommandServiceImpl implements TransactionCommandService {
    private final TransactionCommandRepository commandRepository;
    private final TransactionQueryRepository queryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final KafkaService kafkaService;
    private final MerchantQueryRepository merchantQueryRepository;

    private static final String CACHE_PREFIX = "transaction:";
    private static final String CACHE_LIST_PREFIX = "transaction:list:";

    @Override
    public Future<TransactionResponse> createTransaction(CreateTransactionRequest req) {
        if (req.getOrderID() == null || req.getOrderID() <= 0) {
            return Future.failedFuture(new BadRequestException("order_id is required and must be a positive integer"));
        }
        if (req.getPaymentMethod() == null || req.getPaymentMethod().isBlank()) {
            return Future.failedFuture(new BadRequestException("payment_method is required"));
        }
        if (req.getAmount() == null || req.getAmount() <= 0) {
            return Future.failedFuture(new BadRequestException("amount must be a positive integer"));
        }
        var ctx = tracingMetrics.startSpan("TransactionCommandService.createTransaction");

        return commandRepository.createTransaction(req)
                .compose(created -> invalidateListCache()
                        .compose(v -> sendTransactionCreateEvent(created))
                        .<Transaction>map(v -> created))
                .map(TransactionResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "create", "Transaction created successfully"))
                .onFailure(err -> {
                    log.error("Failed to create transaction", err);
                    tracingMetrics.completeSpanError(ctx, "create", err.getMessage());
                });
    }

    @Override
    public Future<TransactionResponse> updateTransaction(UpdateTransactionRequest req) {
        if (req.getTransactionID() == null || req.getTransactionID() <= 0) {
            return Future.failedFuture(new BadRequestException("transaction_id is required and must be a positive integer"));
        }
        var ctx = tracingMetrics.startSpan("TransactionCommandService.updateTransaction",
                Attributes.builder().put("transaction.id", req.getTransactionID()).build());

        return commandRepository.updateTransaction(req)
                .compose(updated -> {
                    if (updated == null) {
                        return Future.failedFuture(new NotFoundException("Transaction not found"));
                    }
                    return invalidateCache(req.getTransactionID().longValue()).<Transaction>map(v -> updated);
                })
                .map(TransactionResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update", "Transaction updated successfully"))
                .onFailure(err -> {
                    log.error("Failed to update transaction: {}", req.getTransactionID(), err);
                    tracingMetrics.completeSpanError(ctx, "update", err.getMessage());
                });
    }

    @Override
    public Future<TransactionResponseDeleteAt> trashTransaction(Long req) {
        var ctx = tracingMetrics.startSpan("TransactionCommandService.trashTransaction",
                Attributes.builder().put("transaction.id", req).build());

        return commandRepository.trashTransaction(req)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.failedFuture(new NotFoundException("Transaction not found"));
                    }
                    return invalidateCache(req).<Transaction>map(v -> trashed);
                })
                .map(TransactionResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "trash", "Transaction trashed successfully"))
                .onFailure(err -> {
                    log.error("Failed to trash transaction: {}", req, err);
                    tracingMetrics.completeSpanError(ctx, "trash", err.getMessage());
                });
    }

    @Override
    public Future<TransactionResponseDeleteAt> restoreTransaction(Long req) {
        var ctx = tracingMetrics.startSpan("TransactionCommandService.restoreTransaction",
                Attributes.builder().put("transaction.id", req).build());

        return queryRepository.findByTrashedId(req)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.failedFuture(
                                new BadRequestException("Transaction not found or must be trashed first"));
                    }
                    return commandRepository.restoreTransaction(req);
                })
                .compose(r -> {
                    if (r == null) {
                        return Future.<Transaction>failedFuture(new NotFoundException("Transaction not found"));
                    }
                    return invalidateCache(req).<Transaction>map(v -> r);
                })
                .map(TransactionResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restoreTransaction", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreTransaction", e.getMessage()));
    }

    @Override
    public Future<Void> deletePermanent(Long req) {
        var ctx = tracingMetrics.startSpan("TransactionCommandService.deletePermanent",
                Attributes.builder().put("transaction.id", req).build());

        return queryRepository.findByTrashedId(req)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.<Void>failedFuture(
                                new BadRequestException(
                                        "Transaction not found or must be trashed before permanent deletion"));
                    }
                    return commandRepository.deleteTransactionPermanently(req)
                            .compose(deleted -> {
                                if (!deleted) {
                                    return Future.<Void>failedFuture(
                                            new BadRequestException("Failed to delete transaction permanently"));
                                }
                                return invalidateCache(req);
                            });
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deletePermanent",
                        "Transaction permanently deleted"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "deletePermanent", err.getMessage()));
    }

    @Override
    public Future<Void> restoreAllTransactions() {
        var ctx = tracingMetrics.startSpan("TransactionCommandService.restoreAllTransactions");

        return commandRepository.restoreAllTransactions()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed transactions found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all",
                        "All transactions restored successfully"))
                .onFailure(err -> {
                    log.error("Failed to restore all transactions", err);
                    tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
                });
    }

    @Override
    public Future<Void> deleteAllPermanentTransactions() {
        var ctx = tracingMetrics.startSpan("TransactionCommandService.deleteAllPermanentTransactions");

        return commandRepository.deleteAllPermanentTransactions()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed transactions found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "delete_all",
                        "All transactions permanently deleted"))
                .onFailure(err -> {
                    log.error("Failed to permanently delete all transactions", err);
                    tracingMetrics.completeSpanError(ctx, "delete_all", err.getMessage());
                });
    }

    private Future<Void> sendTransactionCreateEvent(Transaction transaction) {
        if (kafkaService == null) {
            log.warn("KafkaService not available, skipping transaction create event");
            return Future.succeededFuture();
        }

        Integer merchantId = transaction.getMerchantId().intValue();
        return merchantQueryRepository.findContactEmailByMerchantId(merchantId)
                .compose(contactEmail -> {
                    if (contactEmail == null || contactEmail.isBlank()) {
                        log.warn("Contact email not found for merchant {}, skipping event", merchantId);
                        return Future.succeededFuture();
                    }

                    JsonObject payload = new JsonObject()
                            .put("email", contactEmail)
                            .put("subject", "New Transaction Created")
                            .put("body", "A new transaction of <b>"
                                    + (transaction.getAmount() != null ? transaction.getAmount() : "N/A")
                                    + "</b> using <b>" + transaction.getPaymentMethod()
                                    + "</b> has been created. Status: <b>" + transaction.getStatus() + "</b>.");
                    return kafkaService.sendMessage("email-service-topic-transaction-create",
                            transaction.getTransactionId().toString(), payload);
                });
    }

    private Future<Void> invalidateCache(Long transactionId) {
        return redisService.delete(CACHE_PREFIX + "id:" + transactionId)
                .compose(v -> redisService.deleteByPattern(CACHE_LIST_PREFIX + "*"))
                .<Void>mapEmpty();
    }

    private Future<Void> invalidateListCache() {
        return redisService.deleteByPattern(CACHE_LIST_PREFIX + "*").<Void>mapEmpty();
    }
}