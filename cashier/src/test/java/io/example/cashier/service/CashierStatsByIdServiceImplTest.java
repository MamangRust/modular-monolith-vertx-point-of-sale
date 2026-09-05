package io.example.cashier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.cashier.domain.requests.cashier.MonthCashierIdRequest;
import io.example.cashier.domain.requests.cashier.MonthTotalSalesCashier;
import io.example.cashier.domain.requests.cashier.YearCashierIdRequest;
import io.example.cashier.domain.requests.cashier.YearTotalSalesCashier;
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.CashierStatByIdRepository;
import io.example.cashier.service.impl.CashierStatsByIdServiceImpl;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class CashierStatsByIdServiceImplTest {

    @Mock private CashierStatByIdRepository statByIdRepository;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private CashierStatsByIdServiceImpl statsByIdService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(tracingContext.getContext()).thenReturn(io.opentelemetry.context.Context.root());
        lenient().when(redisService.setJson(anyString(), any(io.vertx.core.json.JsonObject.class), any(java.time.Duration.class))).thenReturn(Future.succeededFuture("OK"));
        lenient().when(redisService.setJson(anyString(), any(Object.class), any(java.time.Duration.class))).thenReturn(Future.succeededFuture("OK"));

        statsByIdService = new CashierStatsByIdServiceImpl(statByIdRepository, redisService, tracingMetrics);
    }

    @Test
    void getMonthlyTotalSalesById_shouldFetchFromDbOnCacheMiss() {
        MonthTotalSalesCashier req = MonthTotalSalesCashier.builder().year(2024).month(6).cashierId(1).build();
        CashierMonthTotalSales sales = new CashierMonthTotalSales("2024", "06", 150000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statByIdRepository.getMonthlyTotalSalesById(req)).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<List<CashierResponseMonthTotalSales>> result = statsByIdService.getMonthlyTotalSalesById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalSales()).isEqualTo(150000L);
        verify(statByIdRepository).getMonthlyTotalSalesById(req);
    }

    @Test
    void getMonthlyTotalSalesById_shouldReturnFromCacheOnCacheHit() {
        MonthTotalSalesCashier req = MonthTotalSalesCashier.builder().year(2024).month(6).cashierId(1).build();
        String cachedJson = "[{\"year\":\"2024\",\"month\":\"06\",\"totalSales\":150000}]";

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(cachedJson));

        Future<List<CashierResponseMonthTotalSales>> result = statsByIdService.getMonthlyTotalSalesById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        verify(statByIdRepository, never()).getMonthlyTotalSalesById(any());
    }

    @Test
    void getYearlyTotalSalesById_shouldFetchFromDbOnCacheMiss() {
        YearTotalSalesCashier req = YearTotalSalesCashier.builder().year(2024).cashierId(1).build();
        CashierYearTotalSales sales = new CashierYearTotalSales("2024", 2500000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statByIdRepository.getYearlyTotalSalesById(req)).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<List<CashierResponseYearTotalSales>> result = statsByIdService.getYearlyTotalSalesById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
    }

    @Test
    void getMonthlyCashierById_shouldFetchFromDbOnCacheMiss() {
        MonthCashierIdRequest req = MonthCashierIdRequest.builder().year(2024).cashierId(1).build();
        CashierMonthSales sales = new CashierMonthSales("06", 1, "John", 10, 80000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statByIdRepository.getMonthlyCashierById(req)).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<List<CashierResponseMonthSales>> result = statsByIdService.getMonthlyCashierById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
    }

    @Test
    void getYearlyCashierById_shouldFetchFromDbOnCacheMiss() {
        YearCashierIdRequest req = YearCashierIdRequest.builder().year(2024).cashierId(1).build();
        CashierYearSales sales = new CashierYearSales("2024", 1, "John", 100, 980000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statByIdRepository.getYearlyCashierById(req)).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<List<CashierResponseYearSales>> result = statsByIdService.getYearlyCashierById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
    }
}
