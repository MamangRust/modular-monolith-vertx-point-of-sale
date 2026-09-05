package io.example.merchant.service;

import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.merchant.domain.response.MerchantResponseDeleteAt;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantCommandRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.repository.UserQueryRepository;
import io.example.merchant.service.impl.MerchantCommandServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantCommandServiceImplTest {

    @Mock(lenient = true)
    private MerchantCommandRepository commandRepository;
    @Mock(lenient = true)
    private MerchantQueryRepository queryRepository;
    @Mock(lenient = true)
    private UserQueryRepository userQueryRepository;
    @Mock(lenient = true)
    private RedisService redisService;
    @Mock(lenient = true)
    private TracingMetrics tracingMetrics;
    @Mock(lenient = true)
    private KafkaService kafkaService;

    private MerchantCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(mock(TracingMetrics.TracingContext.class));
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class)))
                .thenReturn(mock(TracingMetrics.TracingContext.class));
        lenient().when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        service = new MerchantCommandServiceImpl(
                commandRepository, queryRepository, userQueryRepository,
                redisService, tracingMetrics, kafkaService);
    }

    @Test
    void trashMerchant_shouldTrashAndReturn() throws Exception {
        Long merchantId = 1L;
        Merchant merchant = Merchant.builder()
                .merchantId(merchantId)
                .userId(1L)
                .name("Test Merchant")
                .deletedAt(Timestamp.from(Instant.now()))
                .build();

        when(commandRepository.trashMerchant(merchantId)).thenReturn(Future.succeededFuture(merchant));

        MerchantResponseDeleteAt result = service.trashedMerchant(merchantId)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(merchantId);
        assertThat(result.getDeletedAt()).isNotNull();
        verify(commandRepository).trashMerchant(merchantId);
    }

    @Test
    void trashMerchant_shouldFailWhenNotFound() {
        Long merchantId = 99L;

        when(commandRepository.trashMerchant(merchantId)).thenReturn(Future.succeededFuture(null));

        assertThatThrownBy(() -> service.trashedMerchant(merchantId)
                .toCompletionStage().toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(NotFoundException.class);
    }

    @Test
    void restoreMerchant_shouldRestore() throws Exception {
        Long merchantId = 1L;
        Merchant trashed = Merchant.builder()
                .merchantId(merchantId)
                .userId(1L)
                .name("Trashed Merchant")
                .deletedAt(Timestamp.from(Instant.now()))
                .build();
        Merchant restored = Merchant.builder()
                .merchantId(merchantId)
                .userId(1L)
                .name("Restored Merchant")
                .deletedAt(null)
                .build();

        when(queryRepository.findByTrashedId(merchantId)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepository.restoreMerchant(merchantId)).thenReturn(Future.succeededFuture(restored));

        MerchantResponseDeleteAt result = service.restoreMerchant(merchantId)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(merchantId);
        assertThat(result.getDeletedAt()).isNull();
        verify(queryRepository).findByTrashedId(merchantId);
        verify(commandRepository).restoreMerchant(merchantId);
    }

    @Test
    void restoreMerchant_shouldFailWhenNotTrashed() {
        Long merchantId = 99L;

        when(queryRepository.findByTrashedId(merchantId)).thenReturn(Future.succeededFuture(null));

        assertThatThrownBy(() -> service.restoreMerchant(merchantId)
                .toCompletionStage().toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(io.example.common.exception.grpc.BadRequestException.class);
    }

    @Test
    void restoreAll_shouldRestoreAll() throws Exception {
        int count = 5;

        when(commandRepository.restoreAllMerchant()).thenReturn(Future.succeededFuture(count));

        service.restoreAllMerchant()
                .toCompletionStage().toCompletableFuture().get();

        verify(commandRepository).restoreAllMerchant();
    }

    @Test
    void restoreAll_shouldFailWhenNoneTrashed() {
        when(commandRepository.restoreAllMerchant()).thenReturn(Future.succeededFuture(0));

        assertThatThrownBy(() -> service.restoreAllMerchant()
                .toCompletionStage().toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteAllPermanent_shouldDeleteAll() throws Exception {
        int count = 5;

        when(commandRepository.deleteAllMerchantPermanent()).thenReturn(Future.succeededFuture(count));

        service.deleteAllMerchantPermanent()
                .toCompletionStage().toCompletableFuture().get();

        verify(commandRepository).deleteAllMerchantPermanent();
    }

    @Test
    void deleteAllPermanent_shouldFailWhenNoneTrashed() {
        when(commandRepository.deleteAllMerchantPermanent()).thenReturn(Future.succeededFuture(0));

        assertThatThrownBy(() -> service.deleteAllMerchantPermanent()
                .toCompletionStage().toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(NotFoundException.class);
    }
}
