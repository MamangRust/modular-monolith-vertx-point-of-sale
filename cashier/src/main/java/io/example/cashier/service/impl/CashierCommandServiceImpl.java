package io.example.cashier.service.impl;

import io.example.cashier.model.Cashier;
import io.example.cashier.repository.CashierCommandRepository;
import io.example.cashier.repository.MerchantQueryRepository;
import io.example.cashier.repository.UserQueryRepository;
import io.example.cashier.service.CashierCommandService;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CashierCommandServiceImpl implements CashierCommandService {
    private final CashierCommandRepository commandRepository;
    private final MerchantQueryRepository merchantQueryRepository;
    private final UserQueryRepository userQueryRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    public CashierCommandServiceImpl(CashierCommandRepository commandRepository,
                                     MerchantQueryRepository merchantQueryRepository,
                                     UserQueryRepository userQueryRepository,
                                     RedisService redisService,
                                     TracingMetrics tracingMetrics) {
        this.commandRepository = commandRepository;
        this.merchantQueryRepository = merchantQueryRepository;
        this.userQueryRepository = userQueryRepository;
        this.redisService = redisService;
        this.tracingMetrics = tracingMetrics;
    }

    @Override
    public Future<Cashier> createCashier(Long merchantId, Long userId, String name) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.createCashier");

        return merchantQueryRepository.existsById(merchantId.intValue())
                .compose(exists -> {
                    if (!exists) {
                        return Future.failedFuture(new io.example.common.exception.NotFoundException("Merchant not found"));
                    }
                    return userQueryRepository.existsById(userId.intValue());
                })
                .compose(exists -> {
                    if (!exists) {
                        return Future.failedFuture(new io.example.common.exception.NotFoundException("User not found"));
                    }
                    return commandRepository.createCashier(merchantId, userId, name);
                })
                .map(cashier -> {
                    tracingMetrics.completeSpanSuccess(tracingCtx, "create", "Cashier created");
                    return cashier;
                })
                .recover(err -> handleError(tracingCtx, "create", err));
    }

    @Override
    public Future<Cashier> updateCashier(Long cashierId, String name) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.updateCashier");

        return commandRepository.updateCashier(cashierId, name)
                .map(cashier -> {
                    if (cashier == null) {
                        throw new io.example.common.exception.NotFoundException("Failed to update cashier or cashier not found");
                    }
                    invalidateCache(cashierId);
                    tracingMetrics.completeSpanSuccess(tracingCtx, "update", "Cashier updated");
                    return cashier;
                })
                .recover(err -> handleError(tracingCtx, "update", err));
    }

    @Override
    public Future<Cashier> trashCashier(Long cashierId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.trashCashier");

        return commandRepository.trashCashier(cashierId)
                .map(cashier -> {
                    if (cashier == null) {
                        throw new io.example.common.exception.NotFoundException("Failed to trash cashier or cashier not found");
                    }
                    invalidateCache(cashierId);
                    tracingMetrics.completeSpanSuccess(tracingCtx, "trash", "Cashier trashed");
                    return cashier;
                })
                .recover(err -> handleError(tracingCtx, "trash", err));
    }

    @Override
    public Future<Cashier> restoreCashier(Long cashierId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.restoreCashier");

        return commandRepository.restoreCashier(cashierId)
                .map(cashier -> {
                    if (cashier == null) {
                        throw new io.example.common.exception.NotFoundException("Failed to restore cashier or cashier not found");
                    }
                    invalidateCache(cashierId);
                    tracingMetrics.completeSpanSuccess(tracingCtx, "restore", "Cashier restored");
                    return cashier;
                })
                .recover(err -> handleError(tracingCtx, "restore", err));
    }

    @Override
    public Future<Boolean> deleteCashierPermanent(Long cashierId) {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.deleteCashierPermanent");

        return commandRepository.deleteCashierPermanent(cashierId)
                .map(deleted -> {
                    if (deleted) {
                        invalidateCache(cashierId);
                    }
                    tracingMetrics.completeSpanSuccess(tracingCtx, "delete_permanent", "Cashier permanently deleted");
                    return deleted;
                })
                .recover(err -> handleError(tracingCtx, "delete_permanent", err));
    }

    @Override
    public Future<Boolean> restoreAllCashier() {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.restoreAllCashiers");

        return commandRepository.restoreAllCashier()
                .map(restored -> {
                    tracingMetrics.completeSpanSuccess(tracingCtx, "restore_all", "All cashiers restored");
                    return restored;
                })
                .recover(err -> handleError(tracingCtx, "restore_all", err));
    }

    @Override
    public Future<Boolean> deleteAllCashierPermanent() {
        TracingMetrics.TracingContext tracingCtx = tracingMetrics.startSpan("CashierService.deleteAllPermanentCashiers");

        return commandRepository.deleteAllCashierPermanent()
                .map(deleted -> {
                    tracingMetrics.completeSpanSuccess(tracingCtx, "delete_all", "All cashiers permanently deleted");
                    return deleted;
                })
                .recover(err -> handleError(tracingCtx, "delete_all", err));
    }

    private void invalidateCache(Long cashierId) {
        redisService.delete("cashier:id:" + cashierId);
        log.debug("Invalidated cache for cashier ID: {}", cashierId);
    }

    private <T> Future<T> handleError(TracingMetrics.TracingContext ctx, String methodName, Throwable err) {
        log.error("Cashier command service error in {}: {}", methodName, err.getMessage());
        tracingMetrics.completeSpanError(ctx, methodName, err.getMessage());
        return Future.failedFuture(err);
    }
}
