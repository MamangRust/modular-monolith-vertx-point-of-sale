package io.example.merchant.service.impl;

import io.example.common.exception.BadRequestException;
import io.example.common.exception.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.CreateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentCommandRepository;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.service.MerchantDocumentCommandService;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MerchantDocumentCommandServiceImpl implements MerchantDocumentCommandService {
    private final MerchantDocumentCommandRepository commandRepository;
    private final MerchantDocumentQueryRepository queryRepository;
    private final MerchantQueryRepository merchantQueryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    public MerchantDocumentCommandServiceImpl(MerchantDocumentCommandRepository commandRepository, MerchantDocumentQueryRepository queryRepository, MerchantQueryRepository merchantQueryRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.commandRepository = commandRepository;
        this.queryRepository = queryRepository;
        this.merchantQueryRepository = merchantQueryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<MerchantDocument> createMerchantDocument(CreateMerchantDocumentRequest request) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.createMerchantDocument");
        if (request.getMerchantId() == null) {
            return handleError(tracingCtx, "create_document", new BadRequestException("Merchant ID is required"));
        }

        return merchantQueryRepository.getMerchantById(request.getMerchantId())
                .compose(merchant -> {
                    if (merchant == null) {
                        return Future.failedFuture(new NotFoundException("Merchant not found"));
                    }
                    return commandRepository.createMerchantDocument(request);
                })
                .map(doc -> {
                    if (doc == null) {
                        throw new BadRequestException("Failed to create document");
                    }
                    invalidateCache(doc.getDocumentId(), doc.getMerchantId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "create_document", "Created");
                    return doc;
                })
                .recover(err -> handleError(tracingCtx, "create_document", err));
    }

    @Override
    public Future<MerchantDocument> updateMerchantDocument(UpdateMerchantDocumentRequest request) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.updateMerchantDocument");
        if (request.getDocumentId() == null) {
            return handleError(tracingCtx, "update_document", new BadRequestException("Document ID is required"));
        }

        return queryRepository.getDocumentById(request.getDocumentId())
                .compose(existing -> {
                    if (existing == null) {
                        return Future.failedFuture(new NotFoundException("Merchant document not found"));
                    }
                    return merchantQueryRepository.getMerchantById(request.getMerchantId())
                            .compose(merchant -> {
                                if (merchant == null) {
                                    return Future.failedFuture(new NotFoundException("Merchant not found"));
                                }
                                return commandRepository.updateMerchantDocument(request);
                            });
                })
                .map(doc -> {
                    if (doc == null) {
                        throw new BadRequestException("Failed to update document");
                    }
                    invalidateCache(doc.getDocumentId(), doc.getMerchantId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "update_document", "Updated");
                    return doc;
                })
                .recover(err -> handleError(tracingCtx, "update_document", err));
    }

    @Override
    public Future<MerchantDocument> updateMerchantDocumentStatus(UpdateMerchantDocumentStatusRequest request) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.updateMerchantDocumentStatus");
        if (request.getDocumentId() == null) {
            return handleError(tracingCtx, "update_document_status", new BadRequestException("Document ID is required"));
        }

        return commandRepository.updateMerchantDocumentStatus(request)
                .map(doc -> {
                    if (doc == null) {
                        throw new NotFoundException("Merchant document not found");
                    }
                    invalidateCache(doc.getDocumentId(), doc.getMerchantId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "update_document_status", "Status Updated");
                    return doc;
                })
                .recover(err -> handleError(tracingCtx, "update_document_status", err));
    }

    @Override
    public Future<MerchantDocument> trashMerchantDocument(Integer documentId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.trashMerchantDocument");

        return commandRepository.trashMerchantDocument(documentId)
                .map(doc -> {
                    if (doc == null) {
                        throw new NotFoundException("Merchant document not found or already trashed");
                    }
                    invalidateCache(doc.getDocumentId(), doc.getMerchantId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "trash_document", "Trashed");
                    return doc;
                })
                .recover(err -> handleError(tracingCtx, "trash_document", err));
    }

    @Override
    public Future<MerchantDocument> restoreMerchantDocument(Integer documentId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.restoreMerchantDocument");

        return commandRepository.restoreMerchantDocument(documentId)
                .map(doc -> {
                    if (doc == null) {
                        throw new NotFoundException("Merchant document not found or not in trash");
                    }
                    invalidateCache(doc.getDocumentId(), doc.getMerchantId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "restore_document", "Restored");
                    return doc;
                })
                .recover(err -> handleError(tracingCtx, "restore_document", err));
    }

    @Override
    public Future<Boolean> deleteMerchantDocumentPermanent(Integer documentId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.deleteMerchantDocumentPermanent");

        return queryRepository.getDocumentById(documentId)
                .compose(existing -> commandRepository.deleteMerchantDocumentPermanent(documentId)
                        .map(res -> {
                            if (existing != null) {
                                invalidateCache(documentId, existing.getMerchantId());
                            } else {
                                invalidateCache(documentId, null);
                            }
                            tracingMetrics.completeSpanSuccess(tracingCtx, "delete_document_permanent", "Permanently Deleted");
                            return res;
                        }))
                .recover(err -> handleError(tracingCtx, "delete_document_permanent", err));
    }

    @Override
    public Future<Boolean> restoreAllMerchantDocument() {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.restoreAllMerchantDocument");

        return commandRepository.restoreAllMerchantDocument()
                .map(res -> {
                    redisService.delete("documents:list:");
                    redisService.delete("documents:active:");
                    redisService.delete("documents:trashed:");
                    tracingMetrics.completeSpanSuccess(tracingCtx, "restore_all_documents", "All Restored");
                    return res;
                })
                .recover(err -> handleError(tracingCtx, "restore_all_documents", err));
    }

    @Override
    public Future<Boolean> deleteAllMerchantDocumentPermanent() {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantDocumentService.deleteAllMerchantDocumentPermanent");

        return commandRepository.deleteAllMerchantDocumentPermanent()
                .map(res -> {
                    redisService.delete("documents:list:");
                    redisService.delete("documents:active:");
                    redisService.delete("documents:trashed:");
                    tracingMetrics.completeSpanSuccess(tracingCtx, "delete_all_documents_permanent", "All Permanently Deleted");
                    return res;
                })
                .recover(err -> handleError(tracingCtx, "delete_all_documents_permanent", err));
    }

    private void invalidateCache(Integer documentId, Integer merchantId) {
        if (documentId != null) {
            redisService.delete("document:detail:" + documentId);
        }
        redisService.delete("documents:list:");
        redisService.delete("documents:active:");
        redisService.delete("documents:trashed:");
    }

    private <T> Future<T> handleError(TracingMetrics.TracingContext ctx, String operation, Throwable err) {
        log.error("MerchantDocument command service error in {}: {}", operation, err.getMessage());
        tracingMetrics.completeSpanError(ctx, operation, err.getMessage());
        return Future.failedFuture(err);
    }
}
