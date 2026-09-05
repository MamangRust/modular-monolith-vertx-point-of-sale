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

import io.example.cashier.domain.requests.cashier.MonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.CashierStatsRepository;
import io.example.cashier.service.impl.CashierStatsServiceImpl;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class CashierStatsServiceImplTest {

    @Mock private CashierStatsRepository statsRepository;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private CashierStatsServiceImpl statsService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(tracingContext.getContext()).thenReturn(io.opentelemetry.context.Context.root());
        lenient().when(redisService.setJson(anyString(), any(io.vertx.core.json.JsonObject.class), any(java.time.Duration.class))).thenReturn(Future.succeededFuture("OK"));
        lenient().when(redisService.setJson(anyString(), any(Object.class), any(java.time.Duration.class))).thenReturn(Future.succeededFuture("OK"));

        statsService = new CashierStatsServiceImpl(statsRepository, redisService, tracingMetrics);
    }

    @Test
    void getMonthlyTotalSales_shouldFetchFromDbOnCacheMiss() {
        MonthTotalSales req = MonthTotalSales.builder().year(2024).month(6).build();
        CashierMonthTotalSales sales = new CashierMonthTotalSales("2024", "06", 150000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getMonthlyTotalSales(req)).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<List<CashierResponseMonthTotalSales>> result = statsService.getMonthlyTotalSales(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalSales()).isEqualTo(150000L);
        verify(statsRepository).getMonthlyTotalSales(req);
    }

    @Test
    void getMonthlyTotalSales_shouldReturnFromCacheOnCacheHit() {
        MonthTotalSales req = MonthTotalSales.builder().year(2024).month(6).build();
        String cachedJson = "[{\"year\":\"2024\",\"month\":\"06\",\"totalSales\":150000}]";

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(cachedJson));

        Future<List<CashierResponseMonthTotalSales>> result = statsService.getMonthlyTotalSales(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalSales()).isEqualTo(150000L);
        verify(statsRepository, never()).getMonthlyTotalSales(any());
    }

    @Test
    void getYearlyTotalSales_shouldFetchFromDbOnCacheMiss() {
        int year = 2024;
        CashierYearTotalSales sales = new CashierYearTotalSales("2024", 2500000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getYearlyTotalSales(year)).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<List<CashierResponseYearTotalSales>> result = statsService.getYearlyTotalSales(year);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalSales()).isEqualTo(2500000L);
    }

    @Test
    void getMonthlyCashier_shouldFetchFromDbOnCacheMiss() {
        int year = 2024;
        CashierMonthSales sales = new CashierMonthSales("06", 1, "John", 10, 80000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getMonthlyCashier(year)).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<List<CashierResponseMonthSales>> result = statsService.getMonthlyCashier(year);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCashierName()).isEqualTo("John");
    }

    @Test
    void getYearlyCashier_shouldFetchFromDbOnCacheMiss() {
        int year = 2024;
        CashierYearSales sales = new CashierYearSales("2024", 1, "John", 100, 980000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getYearlyCashier(year)).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<List<CashierResponseYearSales>> result = statsService.getYearlyCashier(year);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCashierName()).isEqualTo("John");
    }
}
