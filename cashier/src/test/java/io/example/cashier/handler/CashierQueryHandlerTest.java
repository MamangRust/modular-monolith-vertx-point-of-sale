package io.example.cashier.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.example.cashier.service.CashierQueryService;
import io.example.cashier.service.CashierStatsByIdService;
import io.example.cashier.service.CashierStatsByMerchant;
import io.example.cashier.service.CashierStatsService;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;

import pb.cashier.Cashier.ApiResponseCashier;
import pb.cashier.Cashier.ApiResponseCashierMonthSales;
import pb.cashier.Cashier.ApiResponseCashierYearSales;
import pb.cashier.Cashier.FindAllCashierRequest;
import pb.cashier.Cashier.FindByIdCashierRequest;
import pb.cashier.Cashier.FindByMerchantCashierRequest;
import pb.cashier.Cashier.FindYearCashier;
import pb.cashier.Cashier.FindYearCashierById;
import pb.cashier.Cashier.FindYearCashierByMerchant;
import pb.cashier.Cashier.FindYearMonthTotalSales;
import pb.cashier.Cashier.FindYearMonthTotalSalesById;
import pb.cashier.Cashier.FindYearMonthTotalSalesByMerchant;
import pb.cashier.Cashier.FindYearTotalSales;
import pb.cashier.Cashier.FindYearTotalSalesById;
import pb.cashier.Cashier.FindYearTotalSalesByMerchant;
import pb.cashier.CashierQuery.ApiResponseCashierMonthlyTotalSales;
import pb.cashier.CashierQuery.ApiResponseCashierYearlyTotalSales;
import pb.cashier.CashierQuery.ApiResponsePaginationCashier;
import pb.cashier.CashierQuery.ApiResponsePaginationCashierDeleteAt;

@ExtendWith(MockitoExtension.class)
class CashierQueryHandlerTest {

    @Mock private CashierStatsService statsService;
    @Mock private CashierStatsByIdService statsByIdService;
    @Mock private CashierStatsByMerchant statsByMerchantService;
    @Mock private CashierQueryService queryService;

    private CashierQueryHandler queryHandler;

    @BeforeEach
    void setUp() {
        queryHandler = new CashierQueryHandler(
                statsService,
                statsByIdService,
                statsByMerchantService,
                queryService
        );
    }

    @Test
    void findAll_shouldReturnPagedResponse() {
        FindAllCashierRequest request = FindAllCashierRequest.newBuilder()
                .setSearch("cashier")
                .setPage(1)
                .setPageSize(10)
                .build();

        CashierResponse responseDto = CashierResponse.builder()
                .id(1)
                .merchantId(2)
                .name("cashier")
                .createdAt("2024-01-01")
                .updatedAt("2024-01-01")
                .build();

        PagedResult<CashierResponse> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getCashiers(any(FindAllCashiers.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationCashier> result = queryHandler.findAll(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("cashier");
    }

    @Test
    void findById_shouldReturnCashierResponse() {
        FindByIdCashierRequest request = FindByIdCashierRequest.newBuilder()
                .setId(1)
                .build();

        CashierResponse responseDto = CashierResponse.builder()
                .id(1)
                .merchantId(2)
                .name("cashier")
                .build();

        when(queryService.getCashierById(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseCashier> result = queryHandler.findById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getName()).isEqualTo("cashier");
    }

    @Test
    void findByActive_shouldReturnPagedDeleteAtResponse() {
        FindAllCashierRequest request = FindAllCashierRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        CashierResponseDeleteAt responseDto = CashierResponseDeleteAt.builder()
                .id(1)
                .name("active-cashier")
                .build();

        PagedResult<CashierResponseDeleteAt> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getCashiersActive(any(FindAllCashiers.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationCashierDeleteAt> result = queryHandler.findByActive(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("active-cashier");
    }

    @Test
    void findByTrashed_shouldReturnPagedDeleteAtResponse() {
        FindAllCashierRequest request = FindAllCashierRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        CashierResponseDeleteAt responseDto = CashierResponseDeleteAt.builder()
                .id(1)
                .name("trashed-cashier")
                .build();

        PagedResult<CashierResponseDeleteAt> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getCashiersTrashed(any(FindAllCashiers.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationCashierDeleteAt> result = queryHandler.findByTrashed(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("trashed-cashier");
    }

    @Test
    void findByMerchant_shouldReturnPagedResponse() {
        FindByMerchantCashierRequest request = FindByMerchantCashierRequest.newBuilder()
                .setMerchantId(5)
                .setPage(1)
                .setPageSize(10)
                .build();

        CashierResponse responseDto = CashierResponse.builder()
                .id(1)
                .merchantId(5)
                .name("merchant-cashier")
                .build();

        PagedResult<CashierResponse> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getCashiersByMerchant(any(FindAllCashierMerchant.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationCashier> result = queryHandler.findByMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getMerchantId()).isEqualTo(5);
    }

    @Test
    void findMonthlyTotalSales_shouldReturnMonthlySalesResponse() {
        FindYearMonthTotalSales request = FindYearMonthTotalSales.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .build();

        CashierResponseMonthTotalSales sales = CashierResponseMonthTotalSales.builder()
                .year("2024")
                .month("06")
                .totalSales(10000L)
                .build();

        when(statsService.getMonthlyTotalSales(any())).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierMonthlyTotalSales> result = queryHandler.findMonthlyTotalSales(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(10000);
    }

    @Test
    void findYearlyTotalSales_shouldReturnYearlySalesResponse() {
        FindYearTotalSales request = FindYearTotalSales.newBuilder()
                .setYear(2024)
                .build();

        CashierResponseYearTotalSales sales = CashierResponseYearTotalSales.builder()
                .year("2024")
                .totalSales(120000L)
                .build();

        when(statsService.getYearlyTotalSales(2024)).thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierYearlyTotalSales> result = queryHandler.findYearlyTotalSales(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(120000);
    }

    @Test
    void findMonthlyTotalSalesById_shouldReturnMonthlySalesResponse() {
        FindYearMonthTotalSalesById request = FindYearMonthTotalSalesById.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .setCashierId(1)
                .build();

        CashierResponseMonthTotalSales sales = CashierResponseMonthTotalSales.builder()
                .year("2024")
                .month("06")
                .totalSales(50000L)
                .build();

        when(statsByIdService.getMonthlyTotalSalesById(any()))
                .thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierMonthlyTotalSales> result = queryHandler.findMonthlyTotalSalesById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(50000);
    }

    @Test
    void findYearlyTotalSalesById_shouldReturnYearlySalesResponse() {
        FindYearTotalSalesById request = FindYearTotalSalesById.newBuilder()
                .setYear(2024)
                .setCashierId(1)
                .build();

        CashierResponseYearTotalSales sales = CashierResponseYearTotalSales.builder()
                .year("2024")
                .totalSales(60000L)
                .build();

        when(statsByIdService.getYearlyTotalSalesById(any()))
                .thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierYearlyTotalSales> result = queryHandler.findYearlyTotalSalesById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(60000);
    }

    @Test
    void findMonthlyTotalSalesByMerchant_shouldReturnMonthlySalesResponse() {
        FindYearMonthTotalSalesByMerchant request = FindYearMonthTotalSalesByMerchant.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .setMerchantId(5)
                .build();

        CashierResponseMonthTotalSales sales = CashierResponseMonthTotalSales.builder()
                .year("2024")
                .month("06")
                .totalSales(70000L)
                .build();

        when(statsByMerchantService.getMonthlyTotalSalesByMerchant(any()))
                .thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierMonthlyTotalSales> result = queryHandler.findMonthlyTotalSalesByMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(70000);
    }

    @Test
    void findYearlyTotalSalesByMerchant_shouldReturnYearlySalesResponse() {
        FindYearTotalSalesByMerchant request = FindYearTotalSalesByMerchant.newBuilder()
                .setYear(2024)
                .setMerchantId(5)
                .build();

        CashierResponseYearTotalSales sales = CashierResponseYearTotalSales.builder()
                .year("2024")
                .totalSales(80000L)
                .build();

        when(statsByMerchantService.getYearlyTotalSalesByMerchant(any()))
                .thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierYearlyTotalSales> result = queryHandler.findYearlyTotalSalesByMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(80000);
    }

    @Test
    void findMonthSales_shouldReturnMonthSalesResponse() {
        FindYearCashier request = FindYearCashier.newBuilder()
                .setYear(2024)
                .build();

        CashierResponseMonthSales sales = CashierResponseMonthSales.builder()
                .month("06")
                .cashierId(1)
                .cashierName("Cashier A")
                .orderCount(10)
                .totalSales(5000L)
                .build();

        when(statsService.getMonthlyCashier(2024))
                .thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierMonthSales> result = queryHandler.findMonthSales(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getMonth()).isEqualTo("06");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(5000);
    }

    @Test
    void findYearSales_shouldReturnYearSalesResponse() {
        FindYearCashier request = FindYearCashier.newBuilder()
                .setYear(2024)
                .build();

        CashierResponseYearSales sales = CashierResponseYearSales.builder()
                .year("2024")
                .cashierId(1)
                .cashierName("Cashier A")
                .orderCount(50)
                .totalSales(60000L)
                .build();

        when(statsService.getYearlyCashier(2024))
                .thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierYearSales> result = queryHandler.findYearSales(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getYear()).isEqualTo("2024");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(60000);
    }

    @Test
    void findMonthSalesByMerchant_shouldReturnMonthSalesResponse() {
        FindYearCashierByMerchant request = FindYearCashierByMerchant.newBuilder()
                .setMerchantId(5)
                .setYear(2024)
                .build();

        CashierResponseMonthSales sales = CashierResponseMonthSales.builder()
                .month("06")
                .cashierId(1)
                .cashierName("Cashier B")
                .orderCount(15)
                .totalSales(7000L)
                .build();

        when(statsByMerchantService.getMonthlyCashierByMerchant(any()))
                .thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierMonthSales> result = queryHandler.findMonthSalesByMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(7000);
    }

    @Test
    void findYearSalesByMerchant_shouldReturnYearSalesResponse() {
        FindYearCashierByMerchant request = FindYearCashierByMerchant.newBuilder()
                .setMerchantId(5)
                .setYear(2024)
                .build();

        CashierResponseYearSales sales = CashierResponseYearSales.builder()
                .year("2024")
                .cashierId(1)
                .cashierName("Cashier B")
                .orderCount(60)
                .totalSales(90000L)
                .build();

        when(statsByMerchantService.getYearlyCashierByMerchant(any()))
                .thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierYearSales> result = queryHandler.findYearSalesByMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(90000);
    }

    @Test
    void findMonthSalesById_shouldReturnMonthSalesResponse() {
        FindYearCashierById request = FindYearCashierById.newBuilder()
                .setCashierId(1)
                .setYear(2024)
                .build();

        CashierResponseMonthSales sales = CashierResponseMonthSales.builder()
                .month("06")
                .cashierId(1)
                .cashierName("Cashier A")
                .orderCount(12)
                .totalSales(5500L)
                .build();

        when(statsByIdService.getMonthlyCashierById(any()))
                .thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierMonthSales> result = queryHandler.findMonthSalesById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(5500);
    }

    @Test
    void findYearSalesById_shouldReturnYearSalesResponse() {
        FindYearCashierById request = FindYearCashierById.newBuilder()
                .setCashierId(1)
                .setYear(2024)
                .build();

        CashierResponseYearSales sales = CashierResponseYearSales.builder()
                .year("2024")
                .cashierId(1)
                .cashierName("Cashier A")
                .orderCount(45)
                .totalSales(65000L)
                .build();

        when(statsByIdService.getYearlyCashierById(any()))
                .thenReturn(Future.succeededFuture(List.of(sales)));

        Future<ApiResponseCashierYearSales> result = queryHandler.findYearSalesById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getTotalSales()).isEqualTo(65000);
    }
}
