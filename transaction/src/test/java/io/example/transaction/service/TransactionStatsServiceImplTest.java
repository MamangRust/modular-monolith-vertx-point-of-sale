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

import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.domain.requests.transactions.MonthAmountTransactionRequest;
import io.example.transaction.domain.response.TransactionMonthlyAmountSuccessResponse;
import io.example.transaction.domain.response.TransactionYearlyAmountFailedResponse;
import io.example.transaction.model.TransactionMonthlyAmountSuccess;
import io.example.transaction.model.TransactionYearlyAmountFailed;
import io.example.transaction.repository.TransactionStatsRepository;
import io.example.transaction.service.impl.TransactionStatsServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class TransactionStatsServiceImplTest {

    @Mock private TransactionStatsRepository statsRepository;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private TransactionStatsServiceImpl statsService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(tracingContext.getContext()).thenReturn(io.opentelemetry.context.Context.root());
        lenient().when(redisService.set(anyString(), anyString(), any())).thenReturn(Future.succeededFuture("OK"));
        statsService = new TransactionStatsServiceImpl(statsRepository, redisService, tracingMetrics);
    }

    // ── findMonthlyTransactionStatusSuccess ───────────────────────────

    @Test
    void findMonthAmountSuccess_shouldReturnFromDb() {
        MonthAmountTransactionRequest req = MonthAmountTransactionRequest.builder()
                .year(2024)
                .month(6)
                .build();
        TransactionMonthlyAmountSuccess success = new TransactionMonthlyAmountSuccess("2024", "06", 100, 5000000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getMonthlyAmountTransactionSuccess(req))
                .thenReturn(Future.succeededFuture(List.of(success)));

        Future<List<TransactionMonthlyAmountSuccessResponse>> result = statsService
                .findMonthlyTransactionStatusSuccess(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getYear()).isEqualTo("2024");
        assertThat(result.result().get(0).getMonth()).isEqualTo("06");
        assertThat(result.result().get(0).getTotalSuccess()).isEqualTo(100);
        assertThat(result.result().get(0).getTotalAmount()).isEqualTo(5000000L);
        verify(statsRepository).getMonthlyAmountTransactionSuccess(req);
    }

    // ── findYearlyTransactionStatusFailed ─────────────────────────────

    @Test
    void findYearAmountFailed_shouldReturnFromDb() {
        int year = 2024;
        TransactionYearlyAmountFailed failed = new TransactionYearlyAmountFailed("2024", 50, 25000000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getYearlyAmountTransactionFailed(year))
                .thenReturn(Future.succeededFuture(List.of(failed)));

        Future<List<TransactionYearlyAmountFailedResponse>> result = statsService
                .findYearlyTransactionStatusFailed(year);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getYear()).isEqualTo("2024");
        assertThat(result.result().get(0).getTotalFailed()).isEqualTo(50);
        assertThat(result.result().get(0).getTotalAmount()).isEqualTo(25000000L);
        verify(statsRepository).getYearlyAmountTransactionFailed(year);
    }
}
