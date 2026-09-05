package io.example.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.transactions.FindAllTransactionRequest;
import io.example.transaction.domain.response.TransactionResponse;
import io.example.transaction.domain.response.TransactionResponseDeleteAt;
import io.example.transaction.enums.PaymentStatus;
import io.example.transaction.model.Transaction;
import io.example.transaction.repository.TransactionQueryRepository;
import io.example.transaction.service.impl.TransactionQueryServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class TransactionQueryServiceImplTest {

    @Mock private TransactionQueryRepository queryRepository;
    @Mock private RedisService redis;
    @Mock private TracingMetrics metrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private TransactionQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        lenient().when(metrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redis.setJson(anyString(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));
        queryService = new TransactionQueryServiceImpl(queryRepository, redis, metrics);
    }

    private Transaction createTransaction() {
        return Transaction.builder()
                .transactionId(1L)
                .orderId(1L)
                .merchantId(1L)
                .amount(50000)
                .paymentMethod("credit_card")
                .status(PaymentStatus.SUCCESS)
                .build();
    }

    // ── findAllTransaction ────────────────────────────────────────────

    @Test
    void findAll_shouldFetchFromDb() {
        FindAllTransactionRequest req = FindAllTransactionRequest.builder()
                .search("")
                .page(1)
                .pageSize(10)
                .build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepository.getTransactions(any(FindAllTransactionRequest.class)))
                .thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createTransaction()), 1)));

        Future<PagedResult<TransactionResponse>> result = queryService.findAllTransaction(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getId()).isEqualTo(1L);
        assertThat(result.result().getTotalRecords()).isEqualTo(1);
        verify(queryRepository).getTransactions(any(FindAllTransactionRequest.class));
    }

    // ── findByIdTransaction ───────────────────────────────────────────

    @Test
    void findById_shouldFetchFromDb() {
        Long id = 1L;
        when(redis.getJson(anyString(), eq(Transaction.class))).thenReturn(Future.succeededFuture(null));
        when(queryRepository.getTransactionById(id)).thenReturn(Future.succeededFuture(createTransaction()));

        Future<TransactionResponse> result = queryService.findByIdTransaction(id);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        assertThat(result.result().getPaymentStatus()).isEqualTo("SUCCESS");
        verify(queryRepository).getTransactionById(id);
    }

    @Test
    void findById_shouldReturnFromCache() {
        Long id = 1L;
        when(redis.getJson(anyString(), eq(Transaction.class))).thenReturn(Future.succeededFuture(createTransaction()));

        Future<TransactionResponse> result = queryService.findByIdTransaction(id);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        assertThat(result.result().getPaymentStatus()).isEqualTo("SUCCESS");
        verify(queryRepository, never()).getTransactionById(anyLong());
    }

    // ── findByActiveTransaction ───────────────────────────────────────

    @Test
    void findByActive_shouldFetchFromDb() {
        FindAllTransactionRequest req = FindAllTransactionRequest.builder()
                .search("")
                .page(1)
                .pageSize(10)
                .build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepository.getTransactionsActive(any(FindAllTransactionRequest.class)))
                .thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createTransaction()), 1)));

        Future<PagedResult<TransactionResponseDeleteAt>> result = queryService.findByActiveTransaction(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getId()).isEqualTo(1L);
        assertThat(result.result().getTotalRecords()).isEqualTo(1);
        verify(queryRepository).getTransactionsActive(any(FindAllTransactionRequest.class));
    }

    // ── findByTrashedTransaction ──────────────────────────────────────

    @Test
    void findByTrashed_shouldFetchFromDb() {
        FindAllTransactionRequest req = FindAllTransactionRequest.builder()
                .search("old")
                .page(1)
                .pageSize(10)
                .build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepository.getTransactionsTrashed(any(FindAllTransactionRequest.class)))
                .thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createTransaction()), 1)));

        Future<PagedResult<TransactionResponseDeleteAt>> result = queryService.findByTrashedTransaction(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getId()).isEqualTo(1L);
        verify(queryRepository).getTransactionsTrashed(any(FindAllTransactionRequest.class));
    }
}
