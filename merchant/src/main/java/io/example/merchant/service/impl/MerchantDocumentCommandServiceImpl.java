package io.example.merchant.service.impl;

import io.example.merchant.domain.requests.CreateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest;
import io.example.merchant.domain.response.MerchantDocumentResponse;
import io.example.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentCommandRepository;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.service.MerchantDocumentCommandService;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MerchantDocumentCommandServiceImpl implements MerchantDocumentCommandService {
    private final MerchantDocumentCommandRepository commandRepository;
    private final MerchantDocumentQueryRepository queryRepository;
    private final MerchantQueryRepository merchantQueryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final KafkaService kafkaService;

    private static final String CACHE_PREFIX = "document:";

    @Override
    public Future<MerchantDocumentResponse> createMerchantDocument(CreateMerchantDocumentRequest request) {
        var ctx = tracingMetrics.startSpan("MerchantDocumentCommandService.createMerchantDocument");

        if (request.getMerchantId() == null) {
            return Future.failedFuture(new BadRequestException("Merchant ID is required"));
        }

        return merchantQueryRepository.getMerchantById(request.getMerchantId().longValue())
                .compose(merchant -> {
                    if (merchant == null) {
                        return Future.failedFuture(new NotFoundException("Merchant not found"));
                    }
                    return commandRepository.createMerchantDocument(request)
                            .compose(doc -> invalidateListCache()
                                    .compose(v -> sendMerchantDocumentCreateEvent(merchant, doc))
                                    .<MerchantDocument>map(v -> doc));
                })
                .map(MerchantDocumentResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "create", "Document created successfully"))
                .onFailure(err -> {
                    log.error("Failed to create merchant document", err);
                    tracingMetrics.completeSpanError(ctx, "create", err.getMessage());
                });
    }

    @Override
    public Future<MerchantDocumentResponse> updateMerchantDocument(UpdateMerchantDocumentRequest request) {
        var ctx = tracingMetrics.startSpan(
                "MerchantDocumentCommandService.updateMerchantDocument",
                Attributes.builder().put("document.id", request.getDocumentId()).build());

        if (request.getDocumentId() == null) {
            return Future.failedFuture(new BadRequestException("Document ID is required"));
        }

        return queryRepository.getDocumentById(request.getDocumentId().longValue())
                .compose(existing -> {
                    if (existing == null) {
                        return Future.failedFuture(new NotFoundException("Merchant document not found"));
                    }
                    // merchantId is optional for updates — only validate when provided
                    if (request.getMerchantId() == null || request.getMerchantId() <= 0) {
                        return commandRepository.updateMerchantDocument(request);
                    }
                    return merchantQueryRepository.getMerchantById(request.getMerchantId().longValue())
                            .compose(merchant -> {
                                if (merchant == null) {
                                    return Future.failedFuture(new NotFoundException("Merchant not found"));
                                }
                                return commandRepository.updateMerchantDocument(request);
                            });
                })
                .compose(doc -> invalidateCache(doc.getDocumentId(), doc.getMerchantId())
                        .<MerchantDocument>map(v -> doc))
                .map(MerchantDocumentResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update", "Document updated successfully"))
                .onFailure(err -> {
                    log.error("Failed to update merchant document: {}", request.getDocumentId(), err);
                    tracingMetrics.completeSpanError(ctx, "update", err.getMessage());
                });
    }

    @Override
    public Future<MerchantDocumentResponse> updateMerchantDocumentStatus(UpdateMerchantDocumentStatusRequest request) {
        var ctx = tracingMetrics.startSpan(
                "MerchantDocumentCommandService.updateMerchantDocumentStatus",
                Attributes.builder().put("document.id", request.getDocumentId()).build());

        if (request.getDocumentId() == null) {
            return Future.failedFuture(new BadRequestException("Document ID is required"));
        }

        return commandRepository.updateMerchantDocumentStatus(request)
                .compose(doc -> {
                    if (doc == null) {
                        return Future.failedFuture(new NotFoundException("Merchant document not found"));
                    }
                    return invalidateCache(doc.getDocumentId(), doc.getMerchantId())
                            .compose(v -> merchantQueryRepository.getMerchantById(doc.getMerchantId().longValue()))
                            .compose(merchant -> {
                                if (merchant == null) {
                                    log.warn("Merchant not found for document status update, skipping event");
                                    return Future.succeededFuture();
                                }
                                return sendMerchantDocumentStatusUpdateEvent(merchant, doc);
                            })
                            .<MerchantDocument>map(v -> doc);
                })
                .map(MerchantDocumentResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update_status",
                        "Document status updated successfully"))
                .onFailure(err -> {
                    log.error("Failed to update merchant document status: {}", request.getDocumentId(), err);
                    tracingMetrics.completeSpanError(ctx, "update_status", err.getMessage());
                });
    }

    @Override
    public Future<MerchantDocumentResponseDeleteAt> trashedMerchantDocument(Long documentId) {
        var ctx = tracingMetrics.startSpan(
                "MerchantDocumentCommandService.trashMerchantDocument",
                Attributes.builder().put("document.id", documentId).build());

        return commandRepository.trashMerchantDocument(documentId)
                .compose(doc -> {
                    if (doc == null) {
                        return Future
                                .failedFuture(new NotFoundException("Merchant document not found or already trashed"));
                    }
                    return invalidateCache(documentId.intValue(), doc.getMerchantId()).<MerchantDocument>map(v -> doc);
                })
                .map(MerchantDocumentResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "trash", "Document trashed successfully"))
                .onFailure(err -> {
                    log.error("Failed to trash merchant document: {}", documentId, err);
                    tracingMetrics.completeSpanError(ctx, "trash", err.getMessage());
                });
    }

    @Override
    public Future<MerchantDocumentResponseDeleteAt> restoreMerchantDocument(Long documentId) {
        var ctx = tracingMetrics.startSpan(
                "MerchantDocumentCommandService.restoreMerchantDocument",
                Attributes.builder().put("document.id", documentId).build());

        return queryRepository.findByTrashedId(documentId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.failedFuture(
                                new BadRequestException("Merchant document not found or must be trashed first"));
                    }
                    return commandRepository.restoreMerchantDocument(documentId);
                })
                .compose(r -> {
                    if (r == null) {
                        return Future.<MerchantDocument>failedFuture(
                                new NotFoundException("Merchant document not found"));
                    }
                    return invalidateCache(r.getDocumentId().intValue(), r.getMerchantId().intValue())
                            .<MerchantDocument>map(v -> r);
                })
                .map(MerchantDocumentResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restoreMerchantDocument", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreMerchantDocument", e.getMessage()));
    }

    @Override
    public Future<Boolean> deleteMerchantDocumentPermanent(Long documentId) {
        var ctx = tracingMetrics.startSpan(
                "MerchantDocumentCommandService.deleteMerchantDocumentPermanent",
                Attributes.builder().put("document.id", documentId).build());

        return queryRepository.findByTrashedId(documentId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.<Boolean>failedFuture(
                                new BadRequestException(
                                        "Merchant document not found or must be trashed before permanent deletion"));
                    }
                    return commandRepository.deleteMerchantDocumentPermanent(documentId)
                            .compose(res -> invalidateCache(documentId.intValue(), trashed.getMerchantId())
                                    .<Boolean>map(v -> res));
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deleteMerchantDocumentPermanent",
                        "Document permanently deleted"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "deleteMerchantDocumentPermanent",
                        err.getMessage()));
    }

    @Override
    public Future<Void> restoreAllMerchantDocument() {
        var ctx = tracingMetrics
                .startSpan("MerchantDocumentCommandService.restoreAllMerchantDocument");

        return commandRepository.restoreAllMerchantDocument()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed documents found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all",
                        "All documents restored successfully"))
                .onFailure(err -> {
                    log.error("Failed to restore all merchant documents", err);
                    tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
                });
    }

    @Override
    public Future<Void> deleteAllMerchantDocumentPermanent() {
        var ctx = tracingMetrics
                .startSpan("MerchantDocumentCommandService.deleteAllMerchantDocumentPermanent");

        return commandRepository.deleteAllMerchantDocumentPermanent()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed documents found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(
                        v -> tracingMetrics.completeSpanSuccess(ctx, "delete_all", "All documents permanently deleted"))
                .onFailure(err -> {
                    log.error("Failed to permanently delete all merchant documents", err);
                    tracingMetrics.completeSpanError(ctx, "delete_all", err.getMessage());
                });
    }

    private Future<Void> sendMerchantDocumentCreateEvent(io.example.merchant.model.Merchant merchant, MerchantDocument doc) {
        if (kafkaService == null) {
            log.warn("KafkaService not available, skipping merchant document create event");
            return Future.succeededFuture();
        }
        JsonObject payload = new JsonObject()
                .put("email", merchant.getContactEmail())
                .put("subject", "Merchant Document Created")
                .put("body", "Document <b>" + doc.getDocumentType() + "</b> has been created for merchant <b>"
                        + merchant.getName() + "</b>.");
        return kafkaService.sendMessage("email-service-topic-merchant-document-create",
                doc.getDocumentId().toString(), payload)
                .recover(err -> { log.warn("Kafka send skipped (merchant doc create): {}", err.getMessage()); return Future.succeededFuture(); });
    }

    private Future<Void> sendMerchantDocumentStatusUpdateEvent(io.example.merchant.model.Merchant merchant, MerchantDocument doc) {
        if (kafkaService == null) {
            log.warn("KafkaService not available, skipping merchant document status update event");
            return Future.succeededFuture();
        }
        JsonObject payload = new JsonObject()
                .put("email", merchant.getContactEmail())
                .put("subject", "Merchant Document Status Updated")
                .put("body", "Document <b>" + doc.getDocumentType() + "</b> for merchant <b>"
                        + merchant.getName() + "</b> status has been updated to <b>" + doc.getStatus() + "</b>.");
        return kafkaService.sendMessage("email-service-topic-merchant-document-update-status",
                doc.getDocumentId().toString(), payload)
                .recover(err -> { log.warn("Kafka send skipped (merchant doc status): {}", err.getMessage()); return Future.succeededFuture(); });
    }

    private Future<Void> invalidateCache(Integer documentId, Integer merchantId) {
        Future<Void> deleteDetail = documentId != null
                ? redisService.delete(CACHE_PREFIX + "detail:" + documentId).<Void>mapEmpty()
                : Future.succeededFuture();

        return deleteDetail
                .compose(v -> invalidateListCache());
    }

    private Future<Void> invalidateListCache() {
        return redisService.deleteByPattern(CACHE_PREFIX + "list:*")
                .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "active:*"))
                .compose(v -> redisService.deleteByPattern(CACHE_PREFIX + "trashed:*"))
                .<Void>mapEmpty();
    }
}