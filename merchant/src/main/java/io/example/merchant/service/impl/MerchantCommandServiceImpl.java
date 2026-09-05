package io.example.merchant.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.vertx.core.json.JsonObject;
import io.example.merchant.domain.requests.CreateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantStatusRequest;
import io.example.merchant.domain.response.MerchantResponse;
import io.example.merchant.domain.response.MerchantResponseDeleteAt;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantCommandRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.repository.UserQueryRepository;
import io.example.merchant.service.MerchantCommandService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MerchantCommandServiceImpl implements MerchantCommandService {
    private final MerchantCommandRepository commandRepository;
    private final MerchantQueryRepository queryRepository;
    private final UserQueryRepository userQueryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;
    private final KafkaService kafkaService;

    private static final String CACHE_PREFIX = "merchant:";
    private static final String CACHE_LIST_PREFIX = "merchant:list:";

    @Override
    public Future<MerchantResponse> createMerchant(CreateMerchantRequest request) {
        var ctx = tracingMetrics.startSpan("MerchantCommandService.createMerchant");

        if (request.getUserId() == null) {
            return Future.failedFuture(new BadRequestException("User ID is required"));
        }

        return userQueryRepository.existsById(request.getUserId().intValue())
                .compose(userExists -> {
                    if (!userExists) {
                        return Future.<Merchant>failedFuture(new NotFoundException("User not found"));
                    }
                    return commandRepository.createMerchant(request);
                })
                .compose(merchant -> invalidateCache(merchant.getMerchantId().intValue(),
                        merchant.getUserId().intValue())
                        .compose(v -> sendMerchantCreateEvent(merchant))
                        .<Merchant>map(v -> merchant))
                .map(MerchantResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "create", "Merchant created successfully"))
                .onFailure(err -> {
                    log.error("Failed to create merchant", err);
                    tracingMetrics.completeSpanError(ctx, "create", err.getMessage());
                });
    }

    @Override
    public Future<MerchantResponse> updateMerchant(UpdateMerchantRequest request) {
        var ctx = tracingMetrics.startSpan(
                "MerchantCommandService.updateMerchant",
                Attributes.builder().put("merchant.id", request.getMerchantId()).build());

        if (request.getMerchantId() == null) {
            return Future.failedFuture(new BadRequestException("Merchant ID is required"));
        }

        return queryRepository.getMerchantById(request.getMerchantId().longValue())
                .compose(existing -> {
                    if (existing == null) {
                        return Future.failedFuture(new NotFoundException("Merchant not found"));
                    }
                    // userId is optional for updates — only validate when provided
                    if (request.getUserId() == null || request.getUserId() <= 0) {
                        return commandRepository.updateMerchant(request);
                    }
                    return userQueryRepository.existsById(request.getUserId().intValue())
                            .compose(userExists -> {
                                if (!userExists) {
                                    return Future.<Merchant>failedFuture(new NotFoundException("User not found"));
                                }
                                return commandRepository.updateMerchant(request);
                            });
                })
                .compose(merchant -> {
                    if (merchant == null) {
                        return Future.failedFuture(new BadRequestException("Failed to update merchant"));
                    }
                    return invalidateCache(merchant.getMerchantId().intValue(), merchant.getUserId().intValue())
                            .<Merchant>map(v -> merchant);
                })
                .map(MerchantResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update", "Merchant updated successfully"))
                .onFailure(err -> {
                    log.error("Failed to update merchant: {}", request.getMerchantId(), err);
                    tracingMetrics.completeSpanError(ctx, "update", err.getMessage());
                });
    }

    @Override
    public Future<MerchantResponse> updateMerchantStatus(UpdateMerchantStatusRequest request) {
        var ctx = tracingMetrics.startSpan(
                "MerchantCommandService.updateMerchantStatus",
                Attributes.builder().put("merchant.id", request.getMerchantId()).build());

        if (request.getMerchantId() == null) {
            return Future.failedFuture(new BadRequestException("Merchant ID is required"));
        }

        return commandRepository.updateMerchantStatus(request)
                .compose(merchant -> {
                    if (merchant == null) {
                        return Future.failedFuture(new NotFoundException("Merchant not found"));
                    }
                    return invalidateCache(merchant.getMerchantId().intValue(), merchant.getUserId().intValue())
                            .compose(v -> sendMerchantStatusUpdateEvent(merchant))
                            .<Merchant>map(v -> merchant);
                })
                .map(MerchantResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update_status",
                        "Merchant status updated successfully"))
                .onFailure(err -> {
                    log.error("Failed to update merchant status: {}", request.getMerchantId(), err);
                    tracingMetrics.completeSpanError(ctx, "update_status", err.getMessage());
                });
    }

    @Override
    public Future<MerchantResponseDeleteAt> trashedMerchant(Long merchantId) {
        var ctx = tracingMetrics.startSpan(
                "MerchantCommandService.trashMerchant",
                Attributes.builder().put("merchant.id", merchantId).build());

        return commandRepository.trashMerchant(merchantId)
                .compose(merchant -> {
                    if (merchant == null) {
                        return Future.failedFuture(new NotFoundException("Merchant not found or already trashed"));
                    }
                    return invalidateCache(merchantId.intValue(), merchant.getUserId().intValue())
                            .<Merchant>map(v -> merchant);
                })
                .map(MerchantResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "trash", "Merchant trashed successfully"))
                .onFailure(err -> {
                    log.error("Failed to trash merchant: {}", merchantId, err);
                    tracingMetrics.completeSpanError(ctx, "trash", err.getMessage());
                });
    }

    @Override
    public Future<MerchantResponseDeleteAt> restoreMerchant(Long merchantId) {
        var ctx = tracingMetrics.startSpan(
                "MerchantCommandService.restoreMerchant",
                Attributes.builder().put("merchant.id", merchantId).build());

        return queryRepository.findByTrashedId(merchantId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.failedFuture(
                                new BadRequestException("Merchant not found or must be trashed first"));
                    }
                    return commandRepository.restoreMerchant(merchantId);
                })
                .compose(r -> {
                    if (r == null) {
                        return Future.<Merchant>failedFuture(new NotFoundException("Merchant not found"));
                    }
                    return invalidateCache(r.getMerchantId().intValue(), r.getUserId().intValue())
                            .<Merchant>map(v -> r);
                })
                .map(MerchantResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restoreMerchant", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreMerchant", e.getMessage()));
    }

    @Override
    public Future<Void> deleteMerchantPermanent(Long merchantId) {
        var ctx = tracingMetrics.startSpan(
                "MerchantCommandService.deleteMerchantPermanent",
                Attributes.builder().put("merchant.id", merchantId).build());

        return queryRepository.findByTrashedId(merchantId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.<Void>failedFuture(
                                new BadRequestException(
                                        "Merchant not found or must be trashed before permanent deletion"));
                    }
                    return commandRepository.deleteMerchantPermanent(merchantId)
                            .compose(res -> invalidateCache(merchantId.intValue(), trashed.getUserId().intValue()));
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deleteMerchantPermanent",
                        "Merchant permanently deleted"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "deleteMerchantPermanent", err.getMessage()));
    }

    @Override
    public Future<Void> restoreAllMerchant() {
        var ctx = tracingMetrics.startSpan("MerchantCommandService.restoreAllMerchant");

        return commandRepository.restoreAllMerchant()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed merchants found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all",
                        "All merchants restored successfully"))
                .onFailure(err -> {
                    log.error("Failed to restore all merchants", err);
                    tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
                });
    }

    @Override
    public Future<Void> deleteAllMerchantPermanent() {
        var ctx = tracingMetrics
                .startSpan("MerchantCommandService.deleteAllMerchantPermanent");

        return commandRepository.deleteAllMerchantPermanent()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed merchants found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "delete_all",
                        "All merchants permanently deleted"))
                .onFailure(err -> {
                    log.error("Failed to permanently delete all merchants", err);
                    tracingMetrics.completeSpanError(ctx, "delete_all", err.getMessage());
                });
    }

    private Future<Void> sendMerchantCreateEvent(Merchant merchant) {
        if (kafkaService == null) {
            log.warn("KafkaService not available, skipping merchant create event");
            return Future.succeededFuture();
        }
        JsonObject payload = new JsonObject()
                .put("email", merchant.getContactEmail())
                .put("subject", "Merchant Created")
                .put("body", "Merchant <b>" + merchant.getName() + "</b> has been created successfully.");
        return kafkaService.sendMessage("email-service-topic-merchant-create",
                merchant.getMerchantId().toString(), payload)
                .recover(err -> { log.warn("Kafka send skipped (merchant create): {}", err.getMessage()); return Future.succeededFuture(); });
    }

    private Future<Void> sendMerchantStatusUpdateEvent(Merchant merchant) {
        if (kafkaService == null) {
            log.warn("KafkaService not available, skipping merchant status update event");
            return Future.succeededFuture();
        }
        JsonObject payload = new JsonObject()
                .put("email", merchant.getContactEmail())
                .put("subject", "Merchant Status Updated")
                .put("body", "Merchant <b>" + merchant.getName() + "</b> status has been updated to <b>"
                        + merchant.getStatus() + "</b>.");
        return kafkaService.sendMessage("email-service-topic-merchant-update-status",
                merchant.getMerchantId().toString(), payload)
                .recover(err -> { log.warn("Kafka send skipped (merchant status update): {}", err.getMessage()); return Future.succeededFuture(); });
    }

    private Future<Void> invalidateListCache() {
        return redisService.deleteByPattern(CACHE_LIST_PREFIX + "*")
                .compose(v -> redisService.deleteByPattern("merchant:active:*"))
                .compose(v -> redisService.deleteByPattern("merchant:trashed:*"))
                .<Void>mapEmpty();
    }

    private Future<Void> invalidateCache(Integer merchantId, Integer userId) {
        Future<Void> deleteDetail = merchantId != null
                ? redisService.delete(CACHE_PREFIX + "detail:" + merchantId).<Void>mapEmpty()
                : Future.succeededFuture();

        Future<Void> deleteUser = userId != null
                ? redisService.delete(CACHE_PREFIX + "user:" + userId).<Void>mapEmpty()
                : Future.succeededFuture();

        return deleteDetail
                .compose(v -> deleteUser)
                .compose(v -> invalidateListCache());
    }
}