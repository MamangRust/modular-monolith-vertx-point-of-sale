package io.example.category.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.category.domain.requests.FindAllCategory;
import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoryResponse;
import io.example.category.domain.response.category.CategoryResponseDeleteAt;
import io.example.category.service.CategoryQueryService;
import io.example.category.service.CategoryStatsByIdService;
import io.example.category.service.CategoryStatsByMerchantService;
import io.example.category.service.CategoryStatsService;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;

import pb.category.Category.ApiResponseCategory;
import pb.category.Category.ApiResponseCategoryMonthPrice;
import pb.category.Category.ApiResponseCategoryMonthlyTotalPrice;
import pb.category.Category.ApiResponseCategoryYearPrice;
import pb.category.Category.ApiResponseCategoryYearlyTotalPrice;
import pb.category.Category.FindAllCategoryRequest;
import pb.category.Category.FindByIdCategoryRequest;
import pb.category.Category.FindYearCategory;
import pb.category.Category.FindYearCategoryById;
import pb.category.Category.FindYearCategoryByMerchant;
import pb.category.Category.FindYearMonthTotalPriceById;
import pb.category.Category.FindYearMonthTotalPriceByMerchant;
import pb.category.Category.FindYearMonthTotalPrices;
import pb.category.Category.FindYearTotalPriceById;
import pb.category.Category.FindYearTotalPriceByMerchant;
import pb.category.Category.FindYearTotalPrices;
import pb.category.CategoryQuery.ApiResponsePaginationCategory;
import pb.category.CategoryQuery.ApiResponsePaginationCategoryDeleteAt;

@ExtendWith(MockitoExtension.class)
class CategoryQueryHandlerTest {

    @Mock private CategoryStatsService statsService;
    @Mock private CategoryStatsByIdService statsByIdService;
    @Mock private CategoryStatsByMerchantService statsByMerchantService;
    @Mock private CategoryQueryService queryService;

    private CategoryQueryHandler queryHandler;

    @BeforeEach
    void setUp() {
        queryHandler = new CategoryQueryHandler(
                statsService,
                statsByIdService,
                statsByMerchantService,
                queryService
        );
    }

    @Test
    void findAll_shouldReturnPagedResponse() {
        FindAllCategoryRequest request = FindAllCategoryRequest.newBuilder()
                .setSearch("books")
                .setPage(1)
                .setPageSize(10)
                .build();

        CategoryResponse responseDto = CategoryResponse.builder()
                .id(1L)
                .name("Books")
                .description("Reading")
                .slugCategory("books")
                .createdAt("2024-01-01")
                .updatedAt("2024-01-01")
                .build();

        PagedResult<CategoryResponse> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getCategories(any(FindAllCategory.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationCategory> result = queryHandler.findAll(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("Books");
    }

    @Test
    void findById_shouldReturnCategoryResponse() {
        FindByIdCategoryRequest request = FindByIdCategoryRequest.newBuilder()
                .setId(1)
                .build();

        CategoryResponse responseDto = CategoryResponse.builder()
                .id(1L)
                .name("Books")
                .build();

        when(queryService.getCategoryById(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseCategory> result = queryHandler.findById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getId()).isEqualTo(1);
    }

    @Test
    void findByActive_shouldReturnPagedDeleteAtResponse() {
        FindAllCategoryRequest request = FindAllCategoryRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        CategoryResponseDeleteAt responseDto = CategoryResponseDeleteAt.builder()
                .id(1L)
                .name("active-category")
                .build();

        PagedResult<CategoryResponseDeleteAt> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getCategoriesActive(any(FindAllCategory.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationCategoryDeleteAt> result = queryHandler.findByActive(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getName()).isEqualTo("active-category");
    }

    @Test
    void findByTrashed_shouldReturnPagedDeleteAtResponse() {
        FindAllCategoryRequest request = FindAllCategoryRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        CategoryResponseDeleteAt responseDto = CategoryResponseDeleteAt.builder()
                .id(1L)
                .name("trashed-category")
                .build();

        PagedResult<CategoryResponseDeleteAt> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getTrashedCategories(any(FindAllCategory.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationCategoryDeleteAt> result = queryHandler.findByTrashed(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getName()).isEqualTo("trashed-category");
    }

    @Test
    void findMonthlyTotalPrices_shouldReturnMonthlyPriceResponse() {
        FindYearMonthTotalPrices request = FindYearMonthTotalPrices.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .build();

        CategoriesMonthlyTotalPriceResponse sales = CategoriesMonthlyTotalPriceResponse.builder()
                .year("2024")
                .month("06")
                .totalRevenue(10000L)
                .build();

        when(statsService.getMonthlyTotalPrice(any())).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCategoryMonthlyTotalPrice> result = queryHandler.findMonthlyTotalPrices(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(10000);
    }

    @Test
    void findYearlyTotalPrices_shouldReturnYearlyPriceResponse() {
        FindYearTotalPrices request = FindYearTotalPrices.newBuilder()
                .setYear(2024)
                .build();

        CategoriesYearlyTotalPriceResponse sales = CategoriesYearlyTotalPriceResponse.builder()
                .year("2024")
                .totalRevenue(120000L)
                .build();

        when(statsService.getYearlyTotalPrice(2024)).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCategoryYearlyTotalPrice> result = queryHandler.findYearlyTotalPrices(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(120000);
    }

    @Test
    void findMonthlyTotalPricesById_shouldReturnMonthlyPriceResponse() {
        FindYearMonthTotalPriceById request = FindYearMonthTotalPriceById.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .setCategoryId(1)
                .build();

        CategoriesMonthlyTotalPriceResponse price = CategoriesMonthlyTotalPriceResponse.builder()
                .year("2024")
                .month("06")
                .totalRevenue(50000L)
                .build();

        when(statsByIdService.getMonthlyTotalPriceById(any()))
                .thenReturn(Future.succeededFuture(List.of(price)));

        Future<ApiResponseCategoryMonthlyTotalPrice> result = queryHandler.findMonthlyTotalPricesById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(50000);
    }

    @Test
    void findYearlyTotalPricesById_shouldReturnYearlyPriceResponse() {
        FindYearTotalPriceById request = FindYearTotalPriceById.newBuilder()
                .setYear(2024)
                .setCategoryId(1)
                .build();

        CategoriesYearlyTotalPriceResponse price = CategoriesYearlyTotalPriceResponse.builder()
                .year("2024")
                .totalRevenue(60000L)
                .build();

        when(statsByIdService.getYearlyTotalPriceById(any()))
                .thenReturn(Future.succeededFuture(List.of(price)));

        Future<ApiResponseCategoryYearlyTotalPrice> result = queryHandler.findYearlyTotalPricesById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(60000);
    }

    @Test
    void findMonthlyTotalPricesByMerchant_shouldReturnMonthlyPriceResponse() {
        FindYearMonthTotalPriceByMerchant request = FindYearMonthTotalPriceByMerchant.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .setMerchantId(5)
                .build();

        CategoriesMonthlyTotalPriceResponse price = CategoriesMonthlyTotalPriceResponse.builder()
                .year("2024")
                .month("06")
                .totalRevenue(70000L)
                .build();

        when(statsByMerchantService.getMonthlyTotalPriceByMerchant(any()))
                .thenReturn(Future.succeededFuture(List.of(price)));

        Future<ApiResponseCategoryMonthlyTotalPrice> result = queryHandler.findMonthlyTotalPricesByMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(70000);
    }

    @Test
    void findYearlyTotalPricesByMerchant_shouldReturnYearlyPriceResponse() {
        FindYearTotalPriceByMerchant request = FindYearTotalPriceByMerchant.newBuilder()
                .setYear(2024)
                .setMerchantId(5)
                .build();

        CategoriesYearlyTotalPriceResponse price = CategoriesYearlyTotalPriceResponse.builder()
                .year("2024")
                .totalRevenue(80000L)
                .build();

        when(statsByMerchantService.getYearlyTotalPriceByMerchant(any()))
                .thenReturn(Future.succeededFuture(List.of(price)));

        Future<ApiResponseCategoryYearlyTotalPrice> result = queryHandler.findYearlyTotalPricesByMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(80000);
    }

    @Test
    void findMonthPrice_shouldReturnMonthPriceResponse() {
        FindYearCategory request = FindYearCategory.newBuilder()
                .setYear(2024)
                .build();

        CategoriesMonthPriceResponse price = CategoriesMonthPriceResponse.builder()
                .month("06")
                .categoryId(1)
                .categoryName("Books")
                .orderCount(10)
                .itemsSold(25)
                .totalRevenue(5000L)
                .build();

        when(statsService.getMonthlyCategory(2024))
                .thenReturn(Future.succeededFuture(List.of(price)));

        Future<ApiResponseCategoryMonthPrice> result = queryHandler.findMonthPrice(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getMonth()).isEqualTo("06");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(5000);
    }

    @Test
    void findYearPrice_shouldReturnYearPriceResponse() {
        FindYearCategory request = FindYearCategory.newBuilder()
                .setYear(2024)
                .build();

        CategoriesYearPriceResponse price = CategoriesYearPriceResponse.builder()
                .year("2024")
                .categoryId(1)
                .categoryName("Books")
                .orderCount(50)
                .itemsSold(120)
                .totalRevenue(60000L)
                .uniqueProductsSold(10)
                .build();

        when(statsService.getYearlyCategory(2024))
                .thenReturn(Future.succeededFuture(List.of(price)));

        Future<ApiResponseCategoryYearPrice> result = queryHandler.findYearPrice(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getYear()).isEqualTo("2024");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(60000);
    }

    @Test
    void findMonthPriceByMerchant_shouldReturnMonthPriceResponse() {
        FindYearCategoryByMerchant request = FindYearCategoryByMerchant.newBuilder()
                .setMerchantId(5)
                .setYear(2024)
                .build();

        CategoriesMonthPriceResponse price = CategoriesMonthPriceResponse.builder()
                .month("06")
                .categoryId(1)
                .categoryName("Electronics")
                .orderCount(15)
                .itemsSold(30)
                .totalRevenue(7000L)
                .build();

        when(statsByMerchantService.getMonthlyCategoryByMerchant(any()))
                .thenReturn(Future.succeededFuture(List.of(price)));

        Future<ApiResponseCategoryMonthPrice> result = queryHandler.findMonthPriceByMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(7000);
    }

    @Test
    void findYearPriceByMerchant_shouldReturnYearPriceResponse() {
        FindYearCategoryByMerchant request = FindYearCategoryByMerchant.newBuilder()
                .setMerchantId(5)
                .setYear(2024)
                .build();

        CategoriesYearPriceResponse price = CategoriesYearPriceResponse.builder()
                .year("2024")
                .categoryId(1)
                .categoryName("Electronics")
                .orderCount(60)
                .itemsSold(140)
                .totalRevenue(90000L)
                .uniqueProductsSold(15)
                .build();

        when(statsByMerchantService.getYearlyCategoryByMerchant(any()))
                .thenReturn(Future.succeededFuture(List.of(price)));

        Future<ApiResponseCategoryYearPrice> result = queryHandler.findYearPriceByMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(90000);
    }

    @Test
    void findMonthPriceById_shouldReturnMonthPriceResponse() {
        FindYearCategoryById request = FindYearCategoryById.newBuilder()
                .setCategoryId(1)
                .setYear(2024)
                .build();

        CategoriesMonthPriceResponse price = CategoriesMonthPriceResponse.builder()
                .month("06")
                .categoryId(1)
                .categoryName("Books")
                .orderCount(12)
                .itemsSold(28)
                .totalRevenue(5500L)
                .build();

        when(statsByIdService.getMonthlyCategoryById(any()))
                .thenReturn(Future.succeededFuture(List.of(price)));

        Future<ApiResponseCategoryMonthPrice> result = queryHandler.findMonthPriceById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(5500);
    }

    @Test
    void findYearPriceById_shouldReturnYearPriceResponse() {
        FindYearCategoryById request = FindYearCategoryById.newBuilder()
                .setCategoryId(1)
                .setYear(2024)
                .build();

        CategoriesYearPriceResponse price = CategoriesYearPriceResponse.builder()
                .year("2024")
                .categoryId(1)
                .categoryName("Books")
                .orderCount(45)
                .itemsSold(100)
                .totalRevenue(65000L)
                .uniqueProductsSold(8)
                .build();

        when(statsByIdService.getYearlyCategoryById(any()))
                .thenReturn(Future.succeededFuture(List.of(price)));

        Future<ApiResponseCategoryYearPrice> result = queryHandler.findYearPriceById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalRevenue()).isEqualTo(65000);
    }
}
