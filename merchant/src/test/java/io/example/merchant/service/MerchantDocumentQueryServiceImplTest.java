package io.example.merchant.service;

import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.FindAllMerchantDocuments;
import io.example.merchant.domain.response.MerchantDocumentResponse;
import io.example.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import io.example.merchant.model.MerchantDocument;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.service.impl.MerchantDocumentQueryServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentQueryServiceImplTest {

    @Mock(lenient = true)
    private MerchantDocumentQueryRepository queryRepository;
    @Mock(lenient = true)
    private RedisService redis;
    @Mock(lenient = true)
    private TracingMetrics metrics;

    private MerchantDocumentQueryServiceImpl service;

    private final FindAllMerchantDocuments sampleRequest = FindAllMerchantDocuments.builder()
            .search("")
            .page(1)
            .pageSize(10)
            .build();

    @BeforeEach
    void setUp() {
        lenient().when(metrics.startSpan(anyString())).thenReturn(mock(TracingMetrics.TracingContext.class));
        lenient().when(metrics.startSpan(anyString(), any(Attributes.class)))
                .thenReturn(mock(TracingMetrics.TracingContext.class));
        lenient().when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        lenient().when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
        lenient().when(redis.setJson(anyString(), any(Object.class), any(Duration.class))).thenReturn(Future.succeededFuture("OK"));

        service = new MerchantDocumentQueryServiceImpl(queryRepository, redis, metrics);
    }

    // --- findAll ---

    @Test
    void findAll_shouldFetchFromDb() throws Exception {
        MerchantDocument doc = MerchantDocument.builder()
                .documentId(1).merchantId(100).documentType("ID_CARD")
                .build();
        PagedResult<MerchantDocument> dbResult = new PagedResult<>(List.of(doc), 1);

        when(queryRepository.getDocuments(sampleRequest)).thenReturn(Future.succeededFuture(dbResult));

        PagedResult<MerchantDocumentResponse> result = service.findAll(sampleRequest)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getDocumentType()).isEqualTo("ID_CARD");
        assertThat(result.getTotalRecords()).isEqualTo(1);
        verify(redis).get(anyString());
        verify(queryRepository).getDocuments(sampleRequest);
        verify(redis).setJson(anyString(), eq(dbResult), any(Duration.class));
    }

    // --- findById ---

    @Test
    void findById_shouldFetchFromDb() throws Exception {
        Long documentId = 1L;
        MerchantDocument doc = MerchantDocument.builder()
                .documentId(documentId.intValue()).merchantId(100).documentType("ID_CARD")
                .build();

        when(redis.getJson(anyString(), eq(MerchantDocument.class))).thenReturn(Future.succeededFuture(null));
        when(queryRepository.getDocumentById(documentId)).thenReturn(Future.succeededFuture(doc));

        MerchantDocumentResponse result = service.findById(documentId)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(documentId.intValue());
        assertThat(result.getDocumentType()).isEqualTo("ID_CARD");
        verify(redis).getJson(anyString(), eq(MerchantDocument.class));
        verify(queryRepository).getDocumentById(documentId);
        verify(redis).setJson(anyString(), eq(doc), any(Duration.class));
    }

    @Test
    void findById_shouldReturnFromCache() throws Exception {
        Long documentId = 1L;
        MerchantDocument cached = MerchantDocument.builder()
                .documentId(documentId.intValue()).merchantId(100).documentType("PASSPORT")
                .build();

        when(redis.getJson(anyString(), eq(MerchantDocument.class))).thenReturn(Future.succeededFuture(cached));

        MerchantDocumentResponse result = service.findById(documentId)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(documentId.intValue());
        assertThat(result.getDocumentType()).isEqualTo("PASSPORT");
        verify(redis).getJson(anyString(), eq(MerchantDocument.class));
        verify(queryRepository, never()).getDocumentById(any());
    }

    // --- findAllActive ---

    @Test
    void findAllActive_shouldFetchFromDb() throws Exception {
        MerchantDocument doc = MerchantDocument.builder()
                .documentId(1).merchantId(100).documentType("LICENSE")
                .build();
        PagedResult<MerchantDocument> dbResult = new PagedResult<>(List.of(doc), 1);

        when(queryRepository.getDocumentsActive(sampleRequest)).thenReturn(Future.succeededFuture(dbResult));

        PagedResult<MerchantDocumentResponseDeleteAt> result = service.findByActive(sampleRequest)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getDocumentType()).isEqualTo("LICENSE");
        assertThat(result.getTotalRecords()).isEqualTo(1);
        verify(redis).get(anyString());
        verify(queryRepository).getDocumentsActive(sampleRequest);
        verify(redis).setJson(anyString(), eq(dbResult), any(Duration.class));
    }

    // --- findAllTrashed ---

    @Test
    void findAllTrashed_shouldFetchFromDb() throws Exception {
        MerchantDocument doc = MerchantDocument.builder()
                .documentId(2).merchantId(200).documentType("BANK_STATEMENT")
                .build();
        PagedResult<MerchantDocument> dbResult = new PagedResult<>(List.of(doc), 1);

        when(queryRepository.getDocumentsTrashed(sampleRequest)).thenReturn(Future.succeededFuture(dbResult));

        PagedResult<MerchantDocumentResponseDeleteAt> result = service.findByTrashed(sampleRequest)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getDocumentType()).isEqualTo("BANK_STATEMENT");
        assertThat(result.getTotalRecords()).isEqualTo(1);
        verify(redis).get(anyString());
        verify(queryRepository).getDocumentsTrashed(sampleRequest);
        verify(redis).setJson(anyString(), eq(dbResult), any(Duration.class));
    }
}
