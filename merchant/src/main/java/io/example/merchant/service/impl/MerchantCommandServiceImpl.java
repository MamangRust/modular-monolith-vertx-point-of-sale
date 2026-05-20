package io.example.merchant.service.impl;

import io.example.common.exception.BadRequestException;
import io.example.common.exception.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.CreateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantStatusRequest;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantCommandRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.repository.UserQueryRepository;
import io.example.merchant.service.MerchantCommandService;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class MerchantCommandServiceImpl implements MerchantCommandService {
    private final MerchantCommandRepository commandRepository;
    private final MerchantQueryRepository queryRepository;
    private final UserQueryRepository userQueryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    public MerchantCommandServiceImpl(MerchantCommandRepository commandRepository, MerchantQueryRepository queryRepository, UserQueryRepository userQueryRepository, RedisService redisService, TracingMetrics tracingMetrics) {
        this.commandRepository = commandRepository;
        this.queryRepository = queryRepository;
        this.userQueryRepository = userQueryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<Merchant> createMerchant(CreateMerchantRequest request) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.createMerchant");
        if (request.getUserId() == null) {
            return handleError(tracingCtx, "create_merchant", new BadRequestException("User ID is required"));
        }

        return userQueryRepository.existsById(request.getUserId())
                .compose(userExists -> {
                    if (!userExists) {
                        return Future.failedFuture(new NotFoundException("User not found"));
                    }
                    String apiKey = "mk_" + UUID.randomUUID().toString().replace("-", "");
                    return commandRepository.createMerchant(request, apiKey);
                })
                .map(m -> {
                    if (m == null) {
                        throw new BadRequestException("Failed to create merchant");
                    }
                    invalidateCache(m.getMerchantId(), m.getUserId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "create_merchant", "Created");
                    return m;
                })
                .recover(err -> handleError(tracingCtx, "create_merchant", err));
    }

    @Override
    public Future<Merchant> updateMerchant(UpdateMerchantRequest request) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.updateMerchant");
        if (request.getMerchantId() == null) {
            return handleError(tracingCtx, "update_merchant", new BadRequestException("Merchant ID is required"));
        }

        return queryRepository.getMerchantById(request.getMerchantId())
                .compose(existing -> {
                    if (existing == null) {
                        return Future.failedFuture(new NotFoundException("Merchant not found"));
                    }
                    return userQueryRepository.existsById(request.getUserId())
                            .compose(userExists -> {
                                if (!userExists) {
                                    return Future.failedFuture(new NotFoundException("User not found"));
                                }
                                return commandRepository.updateMerchant(request);
                            });
                })
                .map(m -> {
                    if (m == null) {
                        throw new BadRequestException("Failed to update merchant");
                    }
                    invalidateCache(m.getMerchantId(), m.getUserId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "update_merchant", "Updated");
                    return m;
                })
                .recover(err -> handleError(tracingCtx, "update_merchant", err));
    }

    @Override
    public Future<Merchant> updateMerchantStatus(UpdateMerchantStatusRequest request) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.updateMerchantStatus");
        if (request.getMerchantId() == null) {
            return handleError(tracingCtx, "update_merchant_status", new BadRequestException("Merchant ID is required"));
        }

        return commandRepository.updateMerchantStatus(request)
                .map(m -> {
                    if (m == null) {
                        throw new NotFoundException("Merchant not found");
                    }
                    invalidateCache(m.getMerchantId(), m.getUserId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "update_merchant_status", "Status Updated");
                    return m;
                })
                .recover(err -> handleError(tracingCtx, "update_merchant_status", err));
    }

    @Override
    public Future<Merchant> trashMerchant(Integer merchantId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.trashMerchant");

        return commandRepository.trashMerchant(merchantId)
                .map(m -> {
                    if (m == null) {
                        throw new NotFoundException("Merchant not found or already trashed");
                    }
                    invalidateCache(m.getMerchantId(), m.getUserId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "trash_merchant", "Trashed");
                    return m;
                })
                .recover(err -> handleError(tracingCtx, "trash_merchant", err));
    }

    @Override
    public Future<Merchant> restoreMerchant(Integer merchantId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.restoreMerchant");

        return commandRepository.restoreMerchant(merchantId)
                .map(m -> {
                    if (m == null) {
                        throw new NotFoundException("Merchant not found or not in trash");
                    }
                    invalidateCache(m.getMerchantId(), m.getUserId());
                    tracingMetrics.completeSpanSuccess(tracingCtx, "restore_merchant", "Restored");
                    return m;
                })
                .recover(err -> handleError(tracingCtx, "restore_merchant", err));
    }

    @Override
    public Future<Boolean> deleteMerchantPermanent(Integer merchantId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.deleteMerchantPermanent");

        return queryRepository.getMerchantById(merchantId)
                .compose(existing -> commandRepository.deleteMerchantPermanent(merchantId)
                        .map(res -> {
                            if (existing != null) {
                                invalidateCache(merchantId, existing.getUserId());
                            } else {
                                invalidateCache(merchantId, null);
                            }
                            tracingMetrics.completeSpanSuccess(tracingCtx, "delete_merchant_permanent", "Permanently Deleted");
                            return res;
                        }))
                .recover(err -> handleError(tracingCtx, "delete_merchant_permanent", err));
    }

    @Override
    public Future<Boolean> restoreAllMerchant() {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.restoreAllMerchant");

        return commandRepository.restoreAllMerchant()
                .map(res -> {
                    redisService.delete("merchants:list:");
                    redisService.delete("merchants:active:");
                    redisService.delete("merchants:trashed:");
                    tracingMetrics.completeSpanSuccess(tracingCtx, "restore_all_merchant", "All Restored");
                    return res;
                })
                .recover(err -> handleError(tracingCtx, "restore_all_merchant", err));
    }

    @Override
    public Future<Boolean> deleteAllMerchantPermanent() {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("MerchantService.deleteAllMerchantPermanent");

        return commandRepository.deleteAllMerchantPermanent()
                .map(res -> {
                    redisService.delete("merchants:list:");
                    redisService.delete("merchants:active:");
                    redisService.delete("merchants:trashed:");
                    tracingMetrics.completeSpanSuccess(tracingCtx, "delete_all_merchant_permanent", "All Permanently Deleted");
                    return res;
                })
                .recover(err -> handleError(tracingCtx, "delete_all_merchant_permanent", err));
    }

    private void invalidateCache(Integer merchantId, Integer userId) {
        if (merchantId != null) {
            redisService.delete("merchant:detail:" + merchantId);
        }
        if (userId != null) {
            redisService.delete("merchants:user:" + userId);
        }
        redisService.delete("merchants:list:");
        redisService.delete("merchants:active:");
        redisService.delete("merchants:trashed:");
    }

    private <T> Future<T> handleError(TracingMetrics.TracingContext ctx, String operation, Throwable err) {
        log.error("Merchant command service error in {}: {}", operation, err.getMessage());
        tracingMetrics.completeSpanError(ctx, operation, err.getMessage());
        return Future.failedFuture(err);
    }
}
