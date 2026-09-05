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

import io.example.category.domain.requests.MonthTotalPriceCategory;
import io.example.category.domain.requests.YearPriceId;
import io.example.category.domain.requests.YearTotalPriceCategory;
import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.CategoryStatsByIdRepository;
import io.example.category.service.impl.CategoryStatsByIdServiceImpl;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class CategoryStatsByIdServiceImplTest {

    @Mock private CategoryStatsByIdRepository statsRepository;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private CategoryStatsByIdServiceImpl statsByIdService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(tracingContext.getContext()).thenReturn(io.opentelemetry.context.Context.root());
        lenient().when(redisService.setJson(anyString(), any(io.vertx.core.json.JsonObject.class), any(java.time.Duration.class))).thenReturn(Future.succeededFuture("OK"));
        lenient().when(redisService.setJson(anyString(), any(Object.class), any(java.time.Duration.class))).thenReturn(Future.succeededFuture("OK"));

        statsByIdService = new CategoryStatsByIdServiceImpl(statsRepository, redisService, tracingMetrics);
    }

    @Test
    void getMonthlyTotalPriceById_shouldFetchFromDbOnCacheMiss() {
        MonthTotalPriceCategory req = MonthTotalPriceCategory.builder().year(2024).month(6).categoryId(1).build();
        CategoryMonthTotalPrice price = new CategoryMonthTotalPrice("2024", "06", 150000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getMonthlyTotalPriceById(req)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesMonthlyTotalPriceResponse>> result = statsByIdService.getMonthlyTotalPriceById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(150000L);
        verify(statsRepository).getMonthlyTotalPriceById(req);
    }

    @Test
    void getMonthlyTotalPriceById_shouldReturnFromCacheOnCacheHit() {
        MonthTotalPriceCategory req = MonthTotalPriceCategory.builder().year(2024).month(6).categoryId(1).build();
        String cachedJson = "[{\"year\":\"2024\",\"month\":\"06\",\"totalRevenue\":150000}]";

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(cachedJson));

        Future<List<CategoriesMonthlyTotalPriceResponse>> result = statsByIdService.getMonthlyTotalPriceById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        verify(statsRepository, never()).getMonthlyTotalPriceById(any());
    }

    @Test
    void getYearlyTotalPriceById_shouldFetchFromDbOnCacheMiss() {
        YearTotalPriceCategory req = YearTotalPriceCategory.builder().year(2024).categoryId(1).build();
        CategoryYearTotalPrice price = new CategoryYearTotalPrice("2024", 2500000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getYearlyTotalPriceById(req)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesYearlyTotalPriceResponse>> result = statsByIdService.getYearlyTotalPriceById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(2500000L);
    }

    @Test
    void getMonthlyCategoryById_shouldFetchFromDbOnCacheMiss() {
        YearPriceId req = YearPriceId.builder().year(2024).categoryId(1).build();
        CategoryMonthPrice price = new CategoryMonthPrice("06", 1, "Books", 100, 200, 50000L);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getMonthlyCategoryById(req)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesMonthPriceResponse>> result = statsByIdService.getMonthlyCategoryById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCategoryName()).isEqualTo("Books");
    }

    @Test
    void getYearlyCategoryById_shouldFetchFromDbOnCacheMiss() {
        YearPriceId req = YearPriceId.builder().year(2024).categoryId(1).build();
        CategoryYearPrice price = new CategoryYearPrice("2024", 1, "Books", 1000, 2000, 500000L, 50);

        when(redisService.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(statsRepository.getYearlyCategoryById(req)).thenReturn(Future.succeededFuture(List.of(price)));

        Future<List<CategoriesYearPriceResponse>> result = statsByIdService.getYearlyCategoryById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCategoryName()).isEqualTo("Books");
    }
}
