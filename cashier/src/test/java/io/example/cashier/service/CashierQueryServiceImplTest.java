package io.example.cashier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.cashier.domain.requests.cashier.FindAllCashierMerchant;
import io.example.cashier.domain.requests.cashier.FindAllCashiers;
import io.example.cashier.domain.response.cashier.CashierResponse;
import io.example.cashier.domain.response.cashier.CashierResponseDeleteAt;
import io.example.cashier.model.Cashier;
import io.example.cashier.repository.CashierQueryRepository;
import io.example.cashier.service.impl.CashierQueryServiceImpl;
import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class CashierQueryServiceImplTest {

    @Mock
    private CashierQueryRepository queryRepo;

    @Mock
    private RedisService redis;

    @Mock
    private TracingMetrics metrics;

    @Mock
    private TracingMetrics.TracingContext tracingContext;

    private CashierQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        lenient().when(metrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redis.setJson(anyString(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));
        queryService = new CashierQueryServiceImpl(queryRepo, redis, metrics);
    }

    private Cashier createCashier() {
        return Cashier.builder()
                .cashierId(1L)
                .merchantId(2L)
                .userId(3L)
                .name("Cashier Test")
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .build();
    }

    // --- getCashiers ---

    @Test
    void getCashiers_shouldFetchFromDbWhenCacheMiss() {
        FindAllCashiers req = FindAllCashiers.builder().search("Test").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.findAllCashiers(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createCashier()), 1)));

        Future<PagedResult<CashierResponse>> result = queryService.getCashiers(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Cashier Test");
        verify(queryRepo).findAllCashiers(req);
    }

    @Test
    void getCashiers_shouldReturnFromCacheWhenCacheHit() {
        FindAllCashiers req = FindAllCashiers.builder().search("Test").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"cashierId\":1,\"merchantId\":2,\"userId\":3,\"name\":\"Cashier Test\",\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<CashierResponse>> result = queryService.getCashiers(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Cashier Test");
        verify(queryRepo, never()).findAllCashiers(any());
    }

    @Test
    void getCashiers_shouldFetchFromDbIfCacheIsCorrupted() {
        FindAllCashiers req = FindAllCashiers.builder().search("Test").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture("corrupted-json"));
        when(queryRepo.findAllCashiers(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createCashier()), 1)));

        Future<PagedResult<CashierResponse>> result = queryService.getCashiers(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).findAllCashiers(req);
    }

    // --- getCashierById ---

    @Test
    void getCashierById_shouldFetchFromDbWhenCacheMiss() {
        Long id = 1L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.findById(id)).thenReturn(Future.succeededFuture(createCashier()));

        Future<CashierResponse> result = queryService.getCashierById(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Cashier Test");
        verify(queryRepo).findById(id);
    }

    @Test
    void getCashierById_shouldReturnFromCacheWhenCacheHit() {
        Long id = 1L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(createCashier()));

        Future<CashierResponse> result = queryService.getCashierById(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Cashier Test");
        verify(queryRepo, never()).findById(anyLong());
    }

    @Test
    void getCashierById_shouldFailWhenNotFound() {
        Long id = 99L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.findById(id)).thenReturn(Future.succeededFuture(null));

        Future<CashierResponse> result = queryService.getCashierById(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- getCashiersActive ---

    @Test
    void getCashiersActive_shouldFetchFromDbWhenCacheMiss() {
        FindAllCashiers req = FindAllCashiers.builder().search("").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.findByActive(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createCashier()), 1)));

        Future<PagedResult<CashierResponseDeleteAt>> result = queryService.getCashiersActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).findByActive(req);
    }

    @Test
    void getCashiersActive_shouldReturnFromCacheWhenCacheHit() {
        FindAllCashiers req = FindAllCashiers.builder().search("").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"cashierId\":1,\"merchantId\":2,\"userId\":3,\"name\":\"Cashier Test\",\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<CashierResponseDeleteAt>> result = queryService.getCashiersActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo, never()).findByActive(any());
    }

    // --- getCashiersTrashed ---

    @Test
    void getCashiersTrashed_shouldFetchFromDbWhenCacheMiss() {
        FindAllCashiers req = FindAllCashiers.builder().search("").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.findByTrashed(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createCashier()), 1)));

        Future<PagedResult<CashierResponseDeleteAt>> result = queryService.getCashiersTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).findByTrashed(req);
    }

    @Test
    void getCashiersTrashed_shouldReturnFromCacheWhenCacheHit() {
        FindAllCashiers req = FindAllCashiers.builder().search("").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"cashierId\":1,\"merchantId\":2,\"userId\":3,\"name\":\"Cashier Test\",\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<CashierResponseDeleteAt>> result = queryService.getCashiersTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo, never()).findByTrashed(any());
    }

    // --- getCashiersByMerchant ---

    @Test
    void getCashiersByMerchant_shouldFetchFromDbWhenCacheMiss() {
        FindAllCashierMerchant req = FindAllCashierMerchant.builder().merchantId(2).page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.findByMerchant(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createCashier()), 1)));

        Future<PagedResult<CashierResponse>> result = queryService.getCashiersByMerchant(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).findByMerchant(req);
    }

    @Test
    void getCashiersByMerchant_shouldReturnFromCacheWhenCacheHit() {
        FindAllCashierMerchant req = FindAllCashierMerchant.builder().merchantId(2).page(1).pageSize(10).build();
        String json = "{\"data\":[{\"cashierId\":1,\"merchantId\":2,\"userId\":3,\"name\":\"Cashier Test\",\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<CashierResponse>> result = queryService.getCashiersByMerchant(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo, never()).findByMerchant(any());
    }
}
