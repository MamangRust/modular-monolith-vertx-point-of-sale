package io.example.apigateway.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.grpc.StatusRuntimeException;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.RoutingContext;
import io.grpc.Status;
import pb.transaction.*;
import pb.transaction.VertxTransactionCommandServiceGrpcClient;
import pb.transaction.VertxTransactionQueryServiceGrpcClient;
import pb.transaction.stats.*;
import pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcClient;
import pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcClient;

@ExtendWith(MockitoExtension.class)
class TransactionProxyHandlerTest {

    @Mock VertxTransactionQueryServiceGrpcClient queryClient;
    @Mock VertxTransactionCommandServiceGrpcClient commandClient;
    @Mock VertxTransactionStatsMethodServiceGrpcClient statsMethodClient;
    @Mock VertxTransactionStatsStatusServiceGrpcClient statsStatusClient;
    @Mock RoutingContext ctx;
    @Mock HttpServerResponse response;
    @Mock RequestBody body;

    private TransactionProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransactionProxyHandler(queryClient, commandClient,
                statsMethodClient, statsStatusClient);
    }

    // -------------------------------------------------------------------------
    // QUERIES
    // -------------------------------------------------------------------------

    @Test
    void getTransactions_shouldBuildRequestFromQueryParams() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);

        MultiMap params = MultiMap.caseInsensitiveMultiMap()
                .add("search", "tx-001")
                .add("page", "1")
                .add("pageSize", "50");
        when(ctx.queryParams()).thenReturn(params);

        when(queryClient.findAll(any())).thenReturn(Future.succeededFuture(ApiResponsePaginationTransaction.getDefaultInstance()));

        handler.getTransactions(ctx);

        verify(queryClient).findAll(any(FindAllTransactionRequest.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void getTransactionById_shouldBuildRequestFromPathParam() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(ctx.pathParam("transactionId")).thenReturn("99");

        when(queryClient.findById(any())).thenReturn(Future.succeededFuture(ApiResponseTransaction.getDefaultInstance()));

        handler.getTransactionById(ctx);

        verify(queryClient).findById(any(FindByIdTransactionRequest.class));
        verify(response).setStatusCode(200);
    }

    // -------------------------------------------------------------------------
    // STATS
    // -------------------------------------------------------------------------

    @Test
    void getMonthTransactionStatusSuccess_shouldBuildRequestFromQuery() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);

        MultiMap params = MultiMap.caseInsensitiveMultiMap()
                .add("year", "2025")
                .add("month", "3");
        when(ctx.queryParams()).thenReturn(params);

        when(statsStatusClient.findMonthStatusSuccess(any())).thenReturn(Future.succeededFuture(ApiResponseTransactionMonthStatusSuccess.getDefaultInstance()));

        handler.getMonthTransactionStatusSuccess(ctx);

        verify(statsStatusClient).findMonthStatusSuccess(any(FindMonthlyTransactionStatus.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void getMonthlyPaymentMethodsByMerchant_shouldBuildRequest() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(ctx.pathParam("merchantId")).thenReturn("42");

        MultiMap params = MultiMap.caseInsensitiveMultiMap()
                .add("year", "2025")
                .add("month", "7");
        when(ctx.queryParams()).thenReturn(params);

        when(statsMethodClient.findMonthMethodByMerchantSuccess(any())).thenReturn(Future.succeededFuture(ApiResponseTransactionMonthMethod.getDefaultInstance()));

        handler.getMonthlyPaymentMethodsByMerchant(ctx);

        verify(statsMethodClient).findMonthMethodByMerchantSuccess(any(MonthTransactionMethodByMerchant.class));
        verify(response).setStatusCode(200);
    }

    // -------------------------------------------------------------------------
    // COMMANDS
    // -------------------------------------------------------------------------

    @Test
    void createTransaction_shouldBuildRequestFromBody() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(ctx.body()).thenReturn(body);

        JsonObject jsonBody = new JsonObject()
                .put("order_id", 500)
                .put("amount", 25000)
                .put("payment_method", "credit_card")
                .put("merchant_id", 10);
        when(body.asJsonObject()).thenReturn(jsonBody);

        when(commandClient.create(any())).thenReturn(Future.succeededFuture(ApiResponseTransaction.getDefaultInstance()));

        handler.createTransaction(ctx);

        verify(commandClient).create(any(CreateTransactionRequest.class));
        verify(response).setStatusCode(201);
    }

    @Test
    void trashTransaction_shouldCallGrpc() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(ctx.pathParam("transactionId")).thenReturn("33");

        when(commandClient.trashedTransaction(any())).thenReturn(Future.succeededFuture(ApiResponseTransactionDeleteAt.getDefaultInstance()));

        handler.trashTransaction(ctx);

        verify(commandClient).trashedTransaction(any(FindByIdTransactionRequest.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void restoreAllTransactions_shouldCallGrpc() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);

        when(commandClient.restoreAllTransaction(any()))
                .thenReturn(Future.succeededFuture(ApiResponseTransactionAll.getDefaultInstance()));

        handler.restoreAllTransactions(ctx);

        verify(commandClient).restoreAllTransaction(com.google.protobuf.Empty.getDefaultInstance());
        verify(response).setStatusCode(200);
    }

    // -------------------------------------------------------------------------
    // ERROR HANDLING
    // -------------------------------------------------------------------------

    @Test
    void getTransactions_shouldHandleGrpcError() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(), anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);

        MultiMap params = MultiMap.caseInsensitiveMultiMap();
        when(ctx.queryParams()).thenReturn(params);

        when(queryClient.findAll(any())).thenReturn(Future.failedFuture(
                new StatusRuntimeException(Status.UNAVAILABLE.withDescription("service down"))));

        handler.getTransactions(ctx);

        verify(response).setStatusCode(503);
    }
}
