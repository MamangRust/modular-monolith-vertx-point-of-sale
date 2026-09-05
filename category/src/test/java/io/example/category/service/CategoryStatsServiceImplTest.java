package io.example.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

import io.example.category.domain.requests.MonthTotalPrice;
import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.CategoryStatsRepository;
import io.example.category.service.impl.CategoryStatsServiceImpl;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class CategoryStatsServiceImplTest {

    @Mock private CategoryStatsRepository statsRepository;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private CategoryStatsServiceImpl statsService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(tracingContext.getContext()).thenReturn(io.opentelemetry.context.Context.root());
        lenient().when(redisService.setJson(anyString(), any(io.vertx.core.json.JsonObject.class), any(java.time.Duration.class))).thenReturn(Future.succeededFuture("OK"));
        lenient().when(redisService.setJson(anyString(), any(Object.class), any(java.time.Duration.class))).thenReturn(Future.succeededFuture("OK"));

        statsService = new CategoryStatsServiceImpl(statsRepository, redisService, tracingMetrics);
    }

    @Test
    void getMonthlyTotalPrice_shouldFetchFromDbOnCacheMiss() {
        MonthTotalPrice req = MonthTotalPrice.builder().year(2024).month(6).build();
        CategoryMonthTotalPrice price = new CategoryMonthTotalPrice("2024", "06", 150000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getMonthlyTotalPrice(req)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesMonthlyTotalPriceResponse>> result = statsService.getMonthlyTotalPrice(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(150000L);
        verify(statsRepository).getMonthlyTotalPrice(req);
    }

    @Test
    void getMonthlyTotalPrice_shouldReturnFromCacheOnCacheHit() {
        MonthTotalPrice req = MonthTotalPrice.builder().year(2024).month(6).build();
        String cachedJson = "[{\"year\":\"2024\",\"month\":\"06\",\"totalRevenue\":150000}]";

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(cachedJson));

        Future<List<CategoriesMonthlyTotalPriceResponse>> result = statsService.getMonthlyTotalPrice(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        verify(statsRepository, never()).getMonthlyTotalPrice(any());
    }

    @Test
    void getYearlyTotalPrice_shouldFetchFromDbOnCacheMiss() {
        int year = 2024;
        CategoryYearTotalPrice price = new CategoryYearTotalPrice("2024", 2500000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getYearlyTotalPrice(year)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesYearlyTotalPriceResponse>> result = statsService.getYearlyTotalPrice(year);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(2500000L);
    }

    @Test
    void getMonthlyCategory_shouldFetchFromDbOnCacheMiss() {
        int year = 2024;
        CategoryMonthPrice price = new CategoryMonthPrice("06", 1, "Books", 100, 200, 50000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getMonthlyCategory(year)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesMonthPriceResponse>> result = statsService.getMonthlyCategory(year);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCategoryName()).isEqualTo("Books");
    }

    @Test
    void getYearlyCategory_shouldFetchFromDbOnCacheMiss() {
        int year = 2024;
        CategoryYearPrice price = new CategoryYearPrice("2024", 1, "Books", 1000, 2000, 500000L, 50);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getYearlyCategory(year)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesYearPriceResponse>> result = statsService.getYearlyCategory(year);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCategoryName()).isEqualTo("Books");
    }
}
