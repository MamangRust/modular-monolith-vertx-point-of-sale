package io.example.category.service;

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

import io.example.category.domain.requests.MonthPriceMerchant;
import io.example.category.domain.requests.MonthTotalPriceMerchant;
import io.example.category.domain.requests.YearPriceMerchant;
import io.example.category.domain.requests.YearTotalPriceMerchant;
import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.CategoryStatsByMerchantRepository;
import io.example.category.service.impl.CategoryStatsByMerchantServiceImpl;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class CategoryStatsByMerchantServiceImplTest {

    @Mock private CategoryStatsByMerchantRepository statsRepository;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private CategoryStatsByMerchantServiceImpl statsByMerchantService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(tracingContext.getContext()).thenReturn(io.opentelemetry.context.Context.root());
        lenient().when(redisService.setJson(anyString(), any(io.vertx.core.json.JsonObject.class), any(java.time.Duration.class))).thenReturn(Future.succeededFuture("OK"));
        lenient().when(redisService.setJson(anyString(), any(Object.class), any(java.time.Duration.class))).thenReturn(Future.succeededFuture("OK"));

        statsByMerchantService = new CategoryStatsByMerchantServiceImpl(statsRepository, redisService, tracingMetrics);
    }

    @Test
    void getMonthlyTotalPriceByMerchant_shouldFetchFromDbOnCacheMiss() {
        MonthTotalPriceMerchant req = MonthTotalPriceMerchant.builder().year(2024).month(6).merchantId(1).build();
        CategoryMonthTotalPrice price = new CategoryMonthTotalPrice("2024", "06", 150000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getMonthlyTotalPriceByMerchant(req)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesMonthlyTotalPriceResponse>> result = statsByMerchantService.getMonthlyTotalPriceByMerchant(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(150000L);
        verify(statsRepository).getMonthlyTotalPriceByMerchant(req);
    }

    @Test
    void getMonthlyTotalPriceByMerchant_shouldReturnFromCacheOnCacheHit() {
        MonthTotalPriceMerchant req = MonthTotalPriceMerchant.builder().year(2024).month(6).merchantId(1).build();
        String cachedJson = "[{\"year\":\"2024\",\"month\":\"06\",\"totalRevenue\":150000}]";

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(cachedJson));

        Future<List<CategoriesMonthlyTotalPriceResponse>> result = statsByMerchantService.getMonthlyTotalPriceByMerchant(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        verify(statsRepository, never()).getMonthlyTotalPriceByMerchant(any());
    }

    @Test
    void getYearlyTotalPriceByMerchant_shouldFetchFromDbOnCacheMiss() {
        YearTotalPriceMerchant req = YearTotalPriceMerchant.builder().year(2024).merchantId(1).build();
        CategoryYearTotalPrice price = new CategoryYearTotalPrice("2024", 2500000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getYearlyTotalPriceByMerchant(req)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesYearlyTotalPriceResponse>> result = statsByMerchantService.getYearlyTotalPriceByMerchant(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(2500000L);
    }

    @Test
    void getMonthlyCategoryByMerchant_shouldFetchFromDbOnCacheMiss() {
        MonthPriceMerchant req = MonthPriceMerchant.builder().year(2024).merchantId(1).build();
        CategoryMonthPrice price = new CategoryMonthPrice("06", 1, "Books", 100, 200, 50000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getMonthlyCategoryByMerchant(req)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesMonthPriceResponse>> result = statsByMerchantService.getMonthlyCategoryByMerchant(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCategoryName()).isEqualTo("Books");
    }

    @Test
    void getYearlyCategoryByMerchant_shouldFetchFromDbOnCacheMiss() {
        YearPriceMerchant req = YearPriceMerchant.builder().year(2024).merchantId(1).build();
        CategoryYearPrice price = new CategoryYearPrice("2024", 1, "Books", 1000, 2000, 500000L, 50);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getYearlyCategoryByMerchant(req)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesYearPriceResponse>> result = statsByMerchantService.getYearlyCategoryByMerchant(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCategoryName()).isEqualTo("Books");
    }
}
