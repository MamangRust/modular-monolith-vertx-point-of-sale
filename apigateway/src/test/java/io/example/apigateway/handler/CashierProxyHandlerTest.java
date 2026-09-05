package io.example.apigateway.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.cashier.Cashier;
import pb.cashier.CashierCommand;
import pb.cashier.CashierQuery;
import pb.cashier.VertxCashierCommandServiceGrpcClient;
import pb.cashier.VertxCashierServiceGrpcClient;

@ExtendWith(MockitoExtension.class)
class CashierProxyHandlerTest {

    @Mock
    VertxCashierServiceGrpcClient queryClient;
    @Mock
    VertxCashierCommandServiceGrpcClient commandClient;
    @Mock
    RoutingContext ctx;
    @Mock
    HttpServerResponse response;
    @Mock
    RequestBody body;

    private CashierProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CashierProxyHandler(queryClient, commandClient);
    }

    @Captor
    ArgumentCaptor<Cashier.FindAllCashierRequest> findAllCaptor;
    @Captor
    ArgumentCaptor<Cashier.FindByIdCashierRequest> findByIdCaptor;
    @Captor
    ArgumentCaptor<Cashier.CreateCashierRequest> createCaptor;
    @Captor
    ArgumentCaptor<Cashier.FindYearMonthTotalSales> findYearMonthCaptor;
    @Captor
    ArgumentCaptor<Cashier.FindYearTotalSalesById> findYearSalesByIdCaptor;

    // =========================================================================
    // QUERY: findAll
    // =========================================================================

    @Test
    void findAll_shouldBuildRequestFromQueryParams() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);

        var params = MultiMap.caseInsensitiveMultiMap()
                .add("search", "test")
                .add("page", "2")
                .add("pageSize", "20");
        when(ctx.queryParams()).thenReturn(params);

        when(queryClient.findAll(any()))
                .thenReturn(Future.succeededFuture(CashierQuery.ApiResponsePaginationCashier.getDefaultInstance()));

        handler.findAll(ctx);

        verify(queryClient).findAll(findAllCaptor.capture());
        var actual = findAllCaptor.getValue();
        assertThat(actual.getSearch()).isEqualTo("test");
        assertThat(actual.getPage()).isEqualTo(2);
        assertThat(actual.getPageSize()).isEqualTo(20);
        verify(response).setStatusCode(200);
    }

    // =========================================================================
    // QUERY: findById
    // =========================================================================

    @Test
    void findById_shouldBuildRequestFromPathParam() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(ctx.pathParam("id")).thenReturn("42");

        when(queryClient.findById(any()))
                .thenReturn(Future.succeededFuture(Cashier.ApiResponseCashier.getDefaultInstance()));

        handler.findById(ctx);

        verify(queryClient).findById(findByIdCaptor.capture());
        assertThat(findByIdCaptor.getValue().getId()).isEqualTo(42);
        verify(response).setStatusCode(200);
    }

    // =========================================================================
    // COMMAND: create
    // =========================================================================

    @Test
    void create_shouldBuildRequestFromBody() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(new JsonObject()
                .put("merchant_id", 10)
                .put("user_id", 20)
                .put("name", "New Cashier"));

        when(commandClient.createCashier(any()))
                .thenReturn(Future.succeededFuture(Cashier.ApiResponseCashier.getDefaultInstance()));

        handler.create(ctx);

        verify(commandClient).createCashier(createCaptor.capture());
        var actual = createCaptor.getValue();
        assertThat(actual.getMerchantId()).isEqualTo(10);
        assertThat(actual.getUserId()).isEqualTo(20);
        assertThat(actual.getName()).isEqualTo("New Cashier");
        verify(response).setStatusCode(201);
    }

    // =========================================================================
    // COMMAND: trashed
    // =========================================================================

    @Test
    void trashed_shouldCallGrpcWithId() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(ctx.pathParam("id")).thenReturn("7");

        when(commandClient.trashedCashier(any()))
                .thenReturn(Future.succeededFuture(Cashier.ApiResponseCashierDeleteAt.getDefaultInstance()));

        handler.trashed(ctx);

        verify(commandClient).trashedCashier(findByIdCaptor.capture());
        assertThat(findByIdCaptor.getValue().getId()).isEqualTo(7);
        verify(response).setStatusCode(200);
    }

    // =========================================================================
    // COMMAND: restoreAll
    // =========================================================================

    @Test
    void restoreAll_shouldCallGrpcWithEmpty() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);

        when(commandClient.restoreAllCashier(any(com.google.protobuf.Empty.class)))
                .thenReturn(Future.succeededFuture(CashierCommand.ApiResponseCashierAll.getDefaultInstance()));

        handler.restoreAll(ctx);

        verify(commandClient).restoreAllCashier(any(com.google.protobuf.Empty.class));
        verify(response).setStatusCode(200);
    }

    // =========================================================================
    // STATS: findMonthlyTotalSales
    // =========================================================================

    @Test
    void findMonthlyTotalSales_shouldBuildRequestWithQueryParams() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);

        var params = MultiMap.caseInsensitiveMultiMap()
                .add("year", "2025")
                .add("month", "6");
        when(ctx.queryParams()).thenReturn(params);

        when(queryClient.findMonthlyTotalSales(any()))
                .thenReturn(Future.succeededFuture(CashierQuery.ApiResponseCashierMonthlyTotalSales.getDefaultInstance()));

        handler.findMonthlyTotalSales(ctx);

        verify(queryClient).findMonthlyTotalSales(findYearMonthCaptor.capture());
        var actual = findYearMonthCaptor.getValue();
        assertThat(actual.getYear()).isEqualTo(2025);
        assertThat(actual.getMonth()).isEqualTo(6);
        verify(response).setStatusCode(200);
    }

    // =========================================================================
    // STATS: findYearlyTotalSalesById
    // =========================================================================

    @Test
    void findYearlyTotalSalesById_shouldBuildRequestWithPathAndQuery() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);

        var params = MultiMap.caseInsensitiveMultiMap().add("year", "2025");
        when(ctx.queryParams()).thenReturn(params);
        when(ctx.pathParam("cashierId")).thenReturn("15");

        when(queryClient.findYearlyTotalSalesById(any()))
                .thenReturn(Future.succeededFuture(CashierQuery.ApiResponseCashierYearlyTotalSales.getDefaultInstance()));

        handler.findYearlyTotalSalesById(ctx);

        verify(queryClient).findYearlyTotalSalesById(findYearSalesByIdCaptor.capture());
        var actual = findYearSalesByIdCaptor.getValue();
        assertThat(actual.getYear()).isEqualTo(2025);
        assertThat(actual.getCashierId()).isEqualTo(15);
        verify(response).setStatusCode(200);
    }

    // =========================================================================
    // ERROR HANDLING
    // =========================================================================

    @Test
    void findAll_shouldHandleGrpcError() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);

        var params = MultiMap.caseInsensitiveMultiMap()
                .add("search", "test")
                .add("page", "1")
                .add("pageSize", "10");
        when(ctx.queryParams()).thenReturn(params);

        when(queryClient.findAll(any()))
                .thenReturn(Future.failedFuture(
                        io.grpc.Status.NOT_FOUND.withDescription("cashier not found").asRuntimeException()));

        handler.findAll(ctx);

        verify(response).setStatusCode(404);
    }
}
