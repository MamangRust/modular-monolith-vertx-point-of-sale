package io.example.merchant.service;

import io.example.common.domain.PagedResult;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.merchant.domain.requests.FindAllMerchants;
import io.example.merchant.domain.response.MerchantResponse;
import io.example.merchant.domain.response.MerchantResponseDeleteAt;
import io.example.merchant.model.Merchant;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.service.impl.MerchantQueryServiceImpl;
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
class MerchantQueryServiceImplTest {

    @Mock(lenient = true)
    private MerchantQueryRepository queryRepository;
    @Mock(lenient = true)
    private RedisService redis;
    @Mock(lenient = true)
    private TracingMetrics metrics;

    private MerchantQueryServiceImpl service;

    private final FindAllMerchants sampleRequest = FindAllMerchants.builder()
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

        service = new MerchantQueryServiceImpl(queryRepository, redis, metrics);
    }

    // --- findAll ---

    @Test
    void findAll_shouldFetchFromDb() throws Exception {
        Merchant merchant = Merchant.builder()
                .merchantId(1L).userId(1L).name("Merchant A")
                .build();
        PagedResult<Merchant> dbResult = new PagedResult<>(List.of(merchant), 1);

        when(queryRepository.getMerchants(sampleRequest)).thenReturn(Future.succeededFuture(dbResult));

        PagedResult<MerchantResponse> result = service.findAll(sampleRequest)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getName()).isEqualTo("Merchant A");
        assertThat(result.getTotalRecords()).isEqualTo(1);
        verify(redis).get(anyString());
        verify(queryRepository).getMerchants(sampleRequest);
        verify(redis).setJson(anyString(), eq(dbResult), any(Duration.class));
    }

    // --- findById ---

    @Test
    void findById_shouldFetchFromDb() throws Exception {
        Long merchantId = 1L;
        Merchant merchant = Merchant.builder()
                .merchantId(merchantId).userId(1L).name("Merchant A")
                .build();

        when(redis.getJson(anyString(), eq(Merchant.class))).thenReturn(Future.succeededFuture(null));
        when(queryRepository.getMerchantById(merchantId)).thenReturn(Future.succeededFuture(merchant));

        MerchantResponse result = service.findById(merchantId)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(merchantId);
        assertThat(result.getName()).isEqualTo("Merchant A");
        verify(redis).getJson(anyString(), eq(Merchant.class));
        verify(queryRepository).getMerchantById(merchantId);
        verify(redis).setJson(anyString(), eq(merchant), any(Duration.class));
    }

    @Test
    void findById_shouldReturnFromCache() throws Exception {
        Long merchantId = 1L;
        Merchant cached = Merchant.builder()
                .merchantId(merchantId).userId(1L).name("Cached Merchant")
                .build();

        when(redis.getJson(anyString(), eq(Merchant.class))).thenReturn(Future.succeededFuture(cached));

        MerchantResponse result = service.findById(merchantId)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(merchantId);
        assertThat(result.getName()).isEqualTo("Cached Merchant");
        verify(redis).getJson(anyString(), eq(Merchant.class));
        verify(queryRepository, never()).getMerchantById(any());
    }

    // --- findByActive ---

    @Test
    void findByActive_shouldFetchFromDb() throws Exception {
        Merchant merchant = Merchant.builder()
                .merchantId(1L).userId(1L).name("Active Merchant")
                .build();
        PagedResult<Merchant> dbResult = new PagedResult<>(List.of(merchant), 1);

        when(queryRepository.getMerchantsActive(sampleRequest)).thenReturn(Future.succeededFuture(dbResult));

        PagedResult<MerchantResponseDeleteAt> result = service.findByActive(sampleRequest)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getName()).isEqualTo("Active Merchant");
        assertThat(result.getTotalRecords()).isEqualTo(1);
        verify(redis).get(anyString());
        verify(queryRepository).getMerchantsActive(sampleRequest);
        verify(redis).setJson(anyString(), eq(dbResult), any(Duration.class));
    }

    // --- findByTrashed ---

    @Test
    void findByTrashed_shouldFetchFromDb() throws Exception {
        Merchant merchant = Merchant.builder()
                .merchantId(1L).userId(1L).name("Trashed Merchant")
                .build();
        PagedResult<Merchant> dbResult = new PagedResult<>(List.of(merchant), 1);

        when(queryRepository.getMerchantsTrashed(sampleRequest)).thenReturn(Future.succeededFuture(dbResult));

        PagedResult<MerchantResponseDeleteAt> result = service.findByTrashed(sampleRequest)
                .toCompletionStage().toCompletableFuture().get();

        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getName()).isEqualTo("Trashed Merchant");
        assertThat(result.getTotalRecords()).isEqualTo(1);
        verify(redis).get(anyString());
        verify(queryRepository).getMerchantsTrashed(sampleRequest);
        verify(redis).setJson(anyString(), eq(dbResult), any(Duration.class));
    }
}
