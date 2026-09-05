package io.example.transaction.service.impl;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.transactions.MonthAmountTransactionMerchant;
import io.example.transaction.domain.requests.transactions.MonthAmountTransactionRequest;
import io.example.transaction.domain.requests.transactions.MonthMethodTransactionMerchantRequest;
import io.example.transaction.domain.requests.transactions.MonthMethodTransactionRequest;
import io.example.transaction.domain.requests.transactions.YearAmountTransactionMerchant;
import io.example.transaction.domain.requests.transactions.YearMethodTransactionMerchantRequest;
import io.example.transaction.domain.response.TransactionMonthlyAmountFailedResponse;
import io.example.transaction.domain.response.TransactionMonthlyAmountSuccessResponse;
import io.example.transaction.domain.response.TransactionMonthlyMethodResponse;
import io.example.transaction.domain.response.TransactionYearlyAmountFailedResponse;
import io.example.transaction.domain.response.TransactionYearlyAmountSuccessResponse;
import io.example.transaction.domain.response.TransactionYearlyMethodResponse;
import io.example.transaction.model.TransactionMonthlyAmountFailed;
import io.example.transaction.model.TransactionMonthlyAmountSuccess;
import io.example.transaction.model.TransactionMonthlyMethod;
import io.example.transaction.model.TransactionYearMethod;
import io.example.transaction.model.TransactionYearlyAmountFailed;
import io.example.transaction.model.TransactionYearlyAmountSuccess;
import io.example.transaction.repository.TransactionStatsRepository;
import io.example.transaction.service.TransactionStatsService;
import io.opentelemetry.api.trace.Span;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TransactionStatsServiceImpl implements TransactionStatsService {
    private final TransactionStatsRepository statsRepository;
    private final RedisService redisService;
    private final TracingMetrics tracingMetrics;

    private static final String CACHE_PREFIX = "transaction:stats:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    @Override
    public Future<List<TransactionMonthlyAmountSuccessResponse>> findMonthlyTransactionStatusSuccess(
            MonthAmountTransactionRequest req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findMonthlyTransactionStatusSuccess");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_status_success:" + req.getYear() + ":" + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionMonthlyAmountSuccess> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionMonthlyAmountSuccess.class))
                                .toList();
                        return Future.succeededFuture(
                                data.stream().map(TransactionMonthlyAmountSuccessResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getMonthlyAmountTransactionSuccess(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionMonthlyAmountSuccessResponse::from)
                                            .toList()));
                })
                .onSuccess(
                        r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyTransactionStatusSuccess", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findMonthlyTransactionStatusSuccess",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionYearlyAmountSuccessResponse>> findYearlyTransactionStatusSuccess(int year) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findYearlyTransactionStatusSuccess");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_status_success:" + year;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionYearlyAmountSuccess> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionYearlyAmountSuccess.class))
                                .toList();
                        return Future.succeededFuture(
                                data.stream().map(TransactionYearlyAmountSuccessResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getYearlyAmountTransactionSuccess(year)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionYearlyAmountSuccessResponse::from)
                                            .toList()));
                })
                .onSuccess(
                        r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyTransactionStatusSuccess", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findYearlyTransactionStatusSuccess",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionMonthlyAmountFailedResponse>> findMonthlyTransactionStatusFailed(
            MonthAmountTransactionRequest req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findMonthlyTransactionStatusFailed");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_status_failed:" + req.getYear() + ":" + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionMonthlyAmountFailed> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionMonthlyAmountFailed.class))
                                .toList();
                        return Future.succeededFuture(
                                data.stream().map(TransactionMonthlyAmountFailedResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getMonthlyAmountTransactionFailed(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionMonthlyAmountFailedResponse::from)
                                            .toList()));
                })
                .onSuccess(
                        r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyTransactionStatusFailed", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findMonthlyTransactionStatusFailed",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionYearlyAmountFailedResponse>> findYearlyTransactionStatusFailed(int year) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findYearlyTransactionStatusFailed");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_status_failed:" + year;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionYearlyAmountFailed> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionYearlyAmountFailed.class))
                                .toList();
                        return Future.succeededFuture(
                                data.stream().map(TransactionYearlyAmountFailedResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getYearlyAmountTransactionFailed(year)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionYearlyAmountFailedResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyTransactionStatusFailed", "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findYearlyTransactionStatusFailed",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionMonthlyAmountSuccessResponse>> findMonthlyTransactionStatusSuccessByMerchant(
            MonthAmountTransactionMerchant req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findMonthlyTransactionStatusSuccessByMerchant");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_status_success_merchant:" + req.getMerchantId() + ":" + req.getYear()
                + ":" + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionMonthlyAmountSuccess> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionMonthlyAmountSuccess.class))
                                .toList();
                        return Future.succeededFuture(
                                data.stream().map(TransactionMonthlyAmountSuccessResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository
                            .getMonthlyAmountTransactionSuccessByMerchant(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionMonthlyAmountSuccessResponse::from)
                                            .toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyTransactionStatusSuccessByMerchant",
                        "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findMonthlyTransactionStatusSuccessByMerchant",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionYearlyAmountSuccessResponse>> findYearlyTransactionStatusSuccessByMerchant(
            YearAmountTransactionMerchant req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findYearlyTransactionStatusSuccessByMerchant");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_status_success_merchant:" + req.getMerchantId() + ":" + req.getYear();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionYearlyAmountSuccess> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionYearlyAmountSuccess.class))
                                .toList();
                        return Future.succeededFuture(
                                data.stream().map(TransactionYearlyAmountSuccessResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository
                            .getYearlyAmountTransactionSuccessByMerchant(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionYearlyAmountSuccessResponse::from)
                                            .toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyTransactionStatusSuccessByMerchant",
                        "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findYearlyTransactionStatusSuccessByMerchant",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionMonthlyAmountFailedResponse>> findMonthlyTransactionStatusFailedByMerchant(
            MonthAmountTransactionMerchant req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findMonthlyTransactionStatusFailedByMerchant");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_status_failed_merchant:" + req.getMerchantId() + ":" + req.getYear()
                + ":" + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionMonthlyAmountFailed> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionMonthlyAmountFailed.class))
                                .toList();
                        return Future.succeededFuture(
                                data.stream().map(TransactionMonthlyAmountFailedResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository
                            .getMonthlyAmountTransactionFailedByMerchant(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionMonthlyAmountFailedResponse::from)
                                            .toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyTransactionStatusFailedByMerchant",
                        "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findMonthlyTransactionStatusFailedByMerchant",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionYearlyAmountFailedResponse>> findYearlyTransactionStatusFailedByMerchant(
            YearAmountTransactionMerchant req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findYearlyTransactionStatusFailedByMerchant");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_status_failed_merchant:" + req.getMerchantId() + ":" + req.getYear();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionYearlyAmountFailed> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionYearlyAmountFailed.class))
                                .toList();
                        return Future.succeededFuture(
                                data.stream().map(TransactionYearlyAmountFailedResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository
                            .getYearlyAmountTransactionFailedByMerchant(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionYearlyAmountFailedResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyTransactionStatusFailedByMerchant",
                        "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findYearlyTransactionStatusFailedByMerchant",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionMonthlyMethodResponse>> findMonthlyPaymentMethodsSuccess(
            MonthMethodTransactionRequest req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findMonthlyPaymentMethodsSuccess");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_method_success:" + req.getYear() + ":" + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionMonthlyMethod> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionMonthlyMethod.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(TransactionMonthlyMethodResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getMonthlyTransactionMethodsSuccess(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionMonthlyMethodResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyPaymentMethodsSuccess", "Success"))
                .onFailure(
                        e -> tracingMetrics.completeSpanError(ctx, "findMonthlyPaymentMethodsSuccess", e.getMessage()));
    }

    @Override
    public Future<List<TransactionYearlyMethodResponse>> findYearlyPaymentMethodsSuccess(int year) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findYearlyPaymentMethodsSuccess");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_method_success:" + year;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionYearMethod> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionYearMethod.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(TransactionYearlyMethodResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getYearlyTransactionMethodsSuccess(year)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionYearlyMethodResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyPaymentMethodsSuccess", "Success"))
                .onFailure(
                        e -> tracingMetrics.completeSpanError(ctx, "findYearlyPaymentMethodsSuccess", e.getMessage()));
    }

    @Override
    public Future<List<TransactionMonthlyMethodResponse>> findMonthlyPaymentMethodsFailed(
            MonthMethodTransactionRequest req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findMonthlyPaymentMethodsFailed");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_method_failed:" + req.getYear() + ":" + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionMonthlyMethod> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionMonthlyMethod.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(TransactionMonthlyMethodResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getMonthlyTransactionMethodsFailed(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionMonthlyMethodResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyPaymentMethodsFailed", "Success"))
                .onFailure(
                        e -> tracingMetrics.completeSpanError(ctx, "findMonthlyPaymentMethodsFailed", e.getMessage()));
    }

    @Override
    public Future<List<TransactionYearlyMethodResponse>> findYearlyPaymentMethodsFailed(int year) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findYearlyPaymentMethodsFailed");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_method_failed:" + year;

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionYearMethod> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionYearMethod.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(TransactionYearlyMethodResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository.getYearlyTransactionMethodsFailed(year)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionYearlyMethodResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyPaymentMethodsFailed", "Success"))
                .onFailure(
                        e -> tracingMetrics.completeSpanError(ctx, "findYearlyPaymentMethodsFailed", e.getMessage()));
    }

    @Override
    public Future<List<TransactionMonthlyMethodResponse>> findMonthlyPaymentMethodsByMerchantSuccess(
            MonthMethodTransactionMerchantRequest req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findMonthlyPaymentMethodsByMerchantSuccess");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_method_success_merchant:" + req.getMerchantId() + ":" + req.getYear()
                + ":" + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionMonthlyMethod> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionMonthlyMethod.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(TransactionMonthlyMethodResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository
                            .getMonthlyTransactionMethodsByMerchantSuccess(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionMonthlyMethodResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyPaymentMethodsByMerchantSuccess",
                        "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findMonthlyPaymentMethodsByMerchantSuccess",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionYearlyMethodResponse>> findYearlyPaymentMethodsByMerchantSuccess(
            YearMethodTransactionMerchantRequest req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findYearlyPaymentMethodsByMerchantSuccess");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_method_success_merchant:" + req.getMerchantId() + ":" + req.getYear();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionYearMethod> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionYearMethod.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(TransactionYearlyMethodResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository
                            .getYearlyTransactionMethodsByMerchantSuccess(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionYearlyMethodResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyPaymentMethodsByMerchantSuccess",
                        "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findYearlyPaymentMethodsByMerchantSuccess",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionMonthlyMethodResponse>> findMonthlyPaymentMethodsByMerchantFailed(
            MonthMethodTransactionMerchantRequest req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findMonthlyPaymentMethodsByMerchantFailed");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "monthly_method_failed_merchant:" + req.getMerchantId() + ":" + req.getYear()
                + ":" + req.getMonth();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionMonthlyMethod> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionMonthlyMethod.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(TransactionMonthlyMethodResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository
                            .getMonthlyTransactionMethodsByMerchantFailed(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionMonthlyMethodResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findMonthlyPaymentMethodsByMerchantFailed",
                        "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findMonthlyPaymentMethodsByMerchantFailed",
                        e.getMessage()));
    }

    @Override
    public Future<List<TransactionYearlyMethodResponse>> findYearlyPaymentMethodsByMerchantFailed(
            YearMethodTransactionMerchantRequest req) {
        var ctx = tracingMetrics.startSpan("TransactionStatsService.findYearlyPaymentMethodsByMerchantFailed");
        Span span = Span.fromContext(Objects.requireNonNull(ctx.getContext()));
        String cacheKey = CACHE_PREFIX + "yearly_method_failed_merchant:" + req.getMerchantId() + ":" + req.getYear();

        return redisService.get(cacheKey)
                .compose(cached -> {
                    if (cached != null) {
                        span.setAttribute("cache.hit", true);
                        JsonArray arr = new JsonArray(cached);
                        List<TransactionYearMethod> data = arr.stream()
                                .map(o -> ((JsonObject) o).mapTo(TransactionYearMethod.class))
                                .toList();
                        return Future
                                .succeededFuture(data.stream().map(TransactionYearlyMethodResponse::from).toList());
                    }
                    span.setAttribute("cache.hit", false);
                    return statsRepository
                            .getYearlyTransactionMethodsByMerchantFailed(req)
                            .compose(list -> redisService
                                    .set(cacheKey,
                                            new JsonArray(list.stream().map(JsonObject::mapFrom).toList()).encode(),
                                            CACHE_TTL)
                                    .map(v -> list.stream().map(TransactionYearlyMethodResponse::from).toList()));
                })
                .onSuccess(r -> tracingMetrics.completeSpanSuccess(ctx, "findYearlyPaymentMethodsByMerchantFailed",
                        "Success"))
                .onFailure(e -> tracingMetrics.completeSpanError(ctx, "findYearlyPaymentMethodsByMerchantFailed",
                        e.getMessage()));
    }
}