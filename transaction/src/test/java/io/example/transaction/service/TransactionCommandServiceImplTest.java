package io.example.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.transaction.domain.response.TransactionResponseDeleteAt;
import io.example.transaction.enums.PaymentStatus;
import io.example.transaction.model.Transaction;
import io.example.transaction.repository.MerchantQueryRepository;
import io.example.transaction.repository.TransactionCommandRepository;
import io.example.transaction.repository.TransactionQueryRepository;
import io.example.transaction.service.impl.TransactionCommandServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class TransactionCommandServiceImplTest {

    @Mock private TransactionCommandRepository commandRepository;
    @Mock private TransactionQueryRepository queryRepository;
    @Mock private RedisService redisService;
    @Mock private KafkaService kafkaService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private MerchantQueryRepository merchantQueryRepository;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private TransactionCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));
        commandService = new TransactionCommandServiceImpl(commandRepository, queryRepository, redisService,
                tracingMetrics, kafkaService, merchantQueryRepository);
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

    // ── trashTransaction ──────────────────────────────────────────────

    @Test
    void trashTransaction_shouldTrashAndReturn() {
        Long id = 1L;
        Transaction trashed = createTransaction();
        when(commandRepository.trashTransaction(id)).thenReturn(Future.succeededFuture(trashed));

        Future<TransactionResponseDeleteAt> result = commandService.trashTransaction(id);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        verify(commandRepository).trashTransaction(id);
    }

    @Test
    void trashTransaction_shouldFailWhenNotFound() {
        Long id = 99L;
        when(commandRepository.trashTransaction(id)).thenReturn(Future.succeededFuture(null));

        Future<TransactionResponseDeleteAt> result = commandService.trashTransaction(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).contains("Transaction not found");
    }

    // ── restoreTransaction ────────────────────────────────────────────

    @Test
    void restoreTransaction_shouldRestore() {
        Long id = 1L;
        Transaction trashed = createTransaction();
        Transaction restored = createTransaction();
        when(queryRepository.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepository.restoreTransaction(id)).thenReturn(Future.succeededFuture(restored));

        Future<TransactionResponseDeleteAt> result = commandService.restoreTransaction(id);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        verify(queryRepository).findByTrashedId(id);
        verify(commandRepository).restoreTransaction(id);
    }

    @Test
    void restoreTransaction_shouldFailWhenNotTrashed() {
        Long id = 99L;
        when(queryRepository.findByTrashedId(id)).thenReturn(Future.succeededFuture(null));

        Future<TransactionResponseDeleteAt> result = commandService.restoreTransaction(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("must be trashed first");
    }

    // ── restoreAllTransactions ────────────────────────────────────────

    @Test
    void restoreAll_shouldRestoreAll() {
        when(commandRepository.restoreAllTransactions()).thenReturn(Future.succeededFuture(5));

        Future<Void> result = commandService.restoreAllTransactions();

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        verify(commandRepository).restoreAllTransactions();
        verify(redisService).deleteByPattern("transaction:list:*");
    }

    @Test
    void restoreAll_shouldFailWhenNoneTrashed() {
        when(commandRepository.restoreAllTransactions()).thenReturn(Future.succeededFuture(0));

        Future<Void> result = commandService.restoreAllTransactions();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).contains("No trashed transactions found");
    }

    // ── deleteAllPermanentTransactions ────────────────────────────────

    @Test
    void deleteAllPermanent_shouldDeleteAll() {
        when(commandRepository.deleteAllPermanentTransactions()).thenReturn(Future.succeededFuture(3));

        Future<Void> result = commandService.deleteAllPermanentTransactions();

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        verify(commandRepository).deleteAllPermanentTransactions();
        verify(redisService).deleteByPattern("transaction:list:*");
    }

    @Test
    void deleteAllPermanent_shouldFailWhenNoneTrashed() {
        when(commandRepository.deleteAllPermanentTransactions()).thenReturn(Future.succeededFuture(0));

        Future<Void> result = commandService.deleteAllPermanentTransactions();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).contains("No trashed transactions found");
    }
}
