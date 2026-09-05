package io.example.cashier.service.impl;

import io.example.cashier.domain.requests.cashier.CreateCashierRequest;
import io.example.cashier.domain.requests.cashier.UpdateCashierRequest;
import io.example.cashier.domain.response.cashier.CashierResponse;
import io.example.cashier.domain.response.cashier.CashierResponseDeleteAt;
import io.example.cashier.model.Cashier;
import io.example.cashier.repository.CashierCommandRepository;
import io.example.cashier.repository.CashierQueryRepository;
import io.example.cashier.repository.MerchantQueryRepository;
import io.example.cashier.repository.UserQueryRepository;
import io.example.cashier.service.CashierCommandService;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CashierCommandServiceImpl implements CashierCommandService {
    private final CashierCommandRepository commandRepository;
    private final CashierQueryRepository queryRepository;
    private final MerchantQueryRepository merchantQueryRepository;
    private final UserQueryRepository userQueryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "cashier:";
    private static final String CACHE_LIST_PREFIX = "cashier:list:";

    @Override
    public Future<CashierResponse> createCashier(CreateCashierRequest req) {
        var ctx = tracingMetrics.startSpan("CashierCommandService.createCashier");

        return queryRepository.findByName(req.getName())
                .compose(existingCashier -> {
                    if (existingCashier != null) {
                        return Future.<Cashier>failedFuture(new BadRequestException("Cashier name already exists"));
                    }

                    return merchantQueryRepository.existsById(req.getMerchantId().intValue())
                            .compose(merchantExists -> {
                                if (!merchantExists) {
                                    return Future.<Cashier>failedFuture(new NotFoundException("Merchant not found"));
                                }

                                return userQueryRepository.existsById(req.getUserId().intValue())
                                        .compose(userExists -> {
                                            if (!userExists) {
                                                return Future.failedFuture(new NotFoundException("User not found"));
                                            }

                                            return commandRepository.createCashier(req);
                                        });
                            });
                })
                .compose(cashier -> invalidateListCache().<Cashier>map(v -> cashier))
                .map(CashierResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "create", "Cashier created successfully"))
                .onFailure(err -> {
                    log.error("Failed to create cashier", err);
                    tracingMetrics.completeSpanError(ctx, "create", err.getMessage());
                });
    }

    @Override
    public Future<CashierResponse> updateCashier(UpdateCashierRequest req) {
        var ctx = tracingMetrics.startSpan(
                "CashierCommandService.updateCashier",
                Attributes.builder().put("cashier.id", req.getCashierId()).build());

        return queryRepository.findById(req.getCashierId().longValue())
                .compose(existing -> {
                    if (existing == null) {
                        return Future.failedFuture(new NotFoundException("Cashier not found"));
                    }
                    // Only check name uniqueness when name is being changed
                    if (req.getName() == null || req.getName().isBlank()) {
                        return commandRepository.updateCashier(req);
                    }
                    return queryRepository.findByName(req.getName())
                            .compose(checkName -> {
                                if (checkName != null
                                        && !checkName.getCashierId().equals(req.getCashierId().longValue())) {
                                    return Future.failedFuture(
                                            new BadRequestException("Cashier name already used by another cashier"));
                                }
                                return commandRepository.updateCashier(req);
                            });
                })
                .compose(cashier -> invalidateCache(req.getCashierId().longValue()).<Cashier>map(v -> cashier))
                .map(CashierResponse::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update", "Cashier updated successfully"))
                .onFailure(err -> {
                    log.error("Failed to update cashier: {}", req.getCashierId(), err);
                    tracingMetrics.completeSpanError(ctx, "update", err.getMessage());
                });
    }

    @Override
    public Future<CashierResponseDeleteAt> trashCashier(Long cashierId) {
        var ctx = tracingMetrics.startSpan(
                "CashierCommandService.trashCashier",
                Attributes.builder().put("cashier.id", cashierId).build());

        return commandRepository.trashCashier(cashierId)
                .compose(cashier -> {
                    if (cashier == null) {
                        return Future.failedFuture(new NotFoundException("Cashier not found or already trashed"));
                    }
                    return invalidateCache(cashierId).<Cashier>map(v -> cashier);
                })
                .map(CashierResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "trash", "Cashier trashed successfully"))
                .onFailure(err -> {
                    log.error("Failed to trash cashier: {}", cashierId, err);
                    tracingMetrics.completeSpanError(ctx, "trash", err.getMessage());
                });
    }

    @Override
    public Future<CashierResponseDeleteAt> restoreCashier(Long cashierId) {
        var ctx = tracingMetrics.startSpan("CashierCommandService.restoreCashier",
                Attributes.builder().put("cashier.id", cashierId).build());

        return queryRepository.findByTrashedId(cashierId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future
                                .failedFuture(new BadRequestException("Cashier not found or must be trashed first"));
                    }
                    return commandRepository.restoreCashier(cashierId);
                })
                .compose(r -> {
                    if (r == null) {
                        return Future.<Cashier>failedFuture(new NotFoundException("Cashier not found"));
                    }
                    return invalidateCache(cashierId).<Cashier>map(v -> r);
                })
                .map(CashierResponseDeleteAt::from)
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restoreCashier", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreCashier", e.getMessage()));
    }

    @Override
    public Future<Void> deleteCashierPermanent(Long cashierId) {
        var ctx = tracingMetrics.startSpan("CashierCommandService.deleteCashierPermanent",
                Attributes.builder().put("cashier.id", cashierId).build());

        return queryRepository.findByTrashedId(cashierId)
                .compose(trashed -> {
                    if (trashed == null) {
                        return Future.<Void>failedFuture(
                                new BadRequestException(
                                        "Cashier not found or must be trashed before permanent deletion"));
                    }
                    return commandRepository.deleteCashierPermanent(cashierId)
                            .compose(deleted -> {
                                if (!deleted) {
                                    return Future.<Void>failedFuture(
                                            new BadRequestException("Failed to delete cashier permanently"));
                                }
                                return invalidateCache(cashierId);
                            });
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deleteCashierPermanent",
                        "Cashier deleted permanently"))
                .onFailure(err -> tracingMetrics.completeSpanError(ctx, "deleteCashierPermanent", err.getMessage()));
    }

    @Override
    public Future<Void> restoreAllCashier() {
        var ctx = tracingMetrics.startSpan("CashierCommandService.restoreAllCashier");

        return commandRepository.restoreAllCashier()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed cashiers found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all",
                        "All cashiers restored successfully"))
                .onFailure(err -> {
                    log.error("Failed to restore all cashiers", err);
                    tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
                });
    }

    @Override
    public Future<Void> deleteAllCashierPermanent() {
        var ctx = tracingMetrics.startSpan("CashierCommandService.deleteAllCashierPermanent");

        return commandRepository.deleteAllCashierPermanent()
                .compose(count -> {
                    if (count == 0) {
                        return Future.failedFuture(new NotFoundException("No trashed cashiers found"));
                    }
                    return invalidateListCache();
                })
                .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "delete_all",
                        "All cashiers permanently deleted"))
                .onFailure(err -> {
                    log.error("Failed to permanently delete all cashiers", err);
                    tracingMetrics.completeSpanError(ctx, "delete_all", err.getMessage());
                });
    }

    private Future<Void> invalidateCache(Long cashierId) {
        return redisService.delete(CACHE_PREFIX + cashierId)
                .compose(v -> redisService.deleteByPattern(CACHE_LIST_PREFIX + "*"))
                .<Void>mapEmpty();
    }

    private Future<Void> invalidateListCache() {
        return redisService.deleteByPattern(CACHE_LIST_PREFIX + "*").<Void>mapEmpty();
    }
}