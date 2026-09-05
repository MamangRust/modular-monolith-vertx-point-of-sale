package io.example.merchant.service;

import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentCommandRepository;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.service.impl.MerchantDocumentCommandServiceImpl;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentCommandServiceImplTest {

    @Mock(lenient = true)
    private MerchantDocumentCommandRepository commandRepository;
    @Mock(lenient = true)
    private MerchantDocumentQueryRepository queryRepository;
    @Mock(lenient = true)
    private MerchantQueryRepository merchantQueryRepository;
    @Mock(lenient = true)
    private RedisService redisService;
    @Mock(lenient = true)
    private TracingMetrics tracingMetrics;
    @Mock(lenient = true)
    private KafkaService kafkaService;

    private MerchantDocumentCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(mock(TracingMetrics.TracingContext.class));
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class)))
                .thenReturn(mock(TracingMetrics.TracingContext.class));
        lenient().when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        service = new MerchantDocumentCommandServiceImpl(
                commandRepository, queryRepository, merchantQueryRepository,
                redisService, tracingMetrics, kafkaService);
    }

    @Test
    void trashDocument_shouldTrashAndReturn() throws Exception {
        Integer documentId = 1;
        MerchantDocument doc = MerchantDocument.builder()
                .documentId(documentId)
                .merchantId(100)
                .documentType("ID_CARD")
                .deletedAt(Timestamp.from(Instant.now()))
                .build();

        when(commandRepository.trashMerchantDocument(documentId.longValue()))
                .thenReturn(Future.succeededFuture(doc));

        MerchantDocumentResponseDeleteAt result = service.trashedMerchantDocument(documentId.longValue())
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(documentId);
        assertThat(result.getMerchantId()).isEqualTo(100);
        assertThat(result.getDeletedAt()).isNotNull();
        verify(commandRepository).trashMerchantDocument(documentId.longValue());
    }

    @Test
    void restoreDocument_shouldRestore() throws Exception {
        Integer documentId = 1;
        MerchantDocument trashed = MerchantDocument.builder()
                .documentId(documentId)
                .merchantId(100)
                .documentType("ID_CARD")
                .deletedAt(Timestamp.from(Instant.now()))
                .build();
        MerchantDocument restored = MerchantDocument.builder()
                .documentId(documentId)
                .merchantId(100)
                .documentType("ID_CARD")
                .deletedAt(null)
                .build();

        when(queryRepository.findByTrashedId(documentId.longValue()))
                .thenReturn(Future.succeededFuture(trashed));
        when(commandRepository.restoreMerchantDocument(documentId.longValue()))
                .thenReturn(Future.succeededFuture(restored));

        MerchantDocumentResponseDeleteAt result = service.restoreMerchantDocument(documentId.longValue())
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(documentId);
        assertThat(result.getDeletedAt()).isNull();
        verify(queryRepository).findByTrashedId(documentId.longValue());
        verify(commandRepository).restoreMerchantDocument(documentId.longValue());
    }

    @Test
    void restoreAll_shouldRestoreAll() throws Exception {
        int count = 3;

        when(commandRepository.restoreAllMerchantDocument()).thenReturn(Future.succeededFuture(count));

        service.restoreAllMerchantDocument()
                .toCompletionStage().toCompletableFuture().get();

        verify(commandRepository).restoreAllMerchantDocument();
    }

    @Test
    void deleteAllPermanent_shouldDeleteAll() throws Exception {
        int count = 3;

        when(commandRepository.deleteAllMerchantDocumentPermanent()).thenReturn(Future.succeededFuture(count));

        service.deleteAllMerchantDocumentPermanent()
                .toCompletionStage().toCompletableFuture().get();

        verify(commandRepository).deleteAllMerchantDocumentPermanent();
    }
}
