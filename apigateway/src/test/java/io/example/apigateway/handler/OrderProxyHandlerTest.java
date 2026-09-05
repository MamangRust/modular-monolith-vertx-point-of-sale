package io.example.apigateway.handler;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.order.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProxyHandlerTest {
    @Mock VertxOrderQueryServiceGrpcClient queryClient;
    @Mock VertxOrderCommandServiceGrpcClient commandClient;
    @Mock RoutingContext ctx;
    @Mock io.vertx.core.http.HttpServerResponse response;
    @Mock io.vertx.ext.web.RequestBody body;
    private OrderProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderProxyHandler(queryClient, commandClient);
    }

    @Test
    void findAll_shouldBuildRequestFromQueryParams() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("search","test").add("page","2").add("pageSize","20"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findAll(any())).thenReturn(Future.succeededFuture(OrderQuery.ApiResponsePaginationOrder.getDefaultInstance()));
        handler.findAll(ctx);
        var captor = ArgumentCaptor.forClass(Order.FindAllOrderRequest.class);
        verify(queryClient).findAll(captor.capture());
        assertThat(captor.getValue().getSearch()).isEqualTo("test");
        assertThat(captor.getValue().getPage()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        verify(response).setStatusCode(200);
    }

    @Test
    void findById_shouldBuildRequestFromPathParam() {
        when(ctx.pathParam("id")).thenReturn("42");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findById(any())).thenReturn(Future.succeededFuture(Order.ApiResponseOrder.getDefaultInstance()));
        handler.findById(ctx);
        var captor = ArgumentCaptor.forClass(Order.FindByIdOrderRequest.class);
        verify(queryClient).findById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(42);
        verify(response).setStatusCode(200);
    }

    @Test
    void create_shouldBuildRequestFromBody() {
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(new JsonObject().put("order_code","ORD-001").put("total_price",5000));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(commandClient.create(any())).thenReturn(Future.succeededFuture(Order.ApiResponseOrder.getDefaultInstance()));
        handler.create(ctx);
        verify(commandClient).create(any(Order.CreateOrderRequest.class));
        verify(response).setStatusCode(201);
    }

    @Test
    void findMonthlyTotalRevenue_shouldBuildRequestFromQuery() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("year","2024").add("month","6"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findMonthlyTotalRevenue(any())).thenReturn(Future.succeededFuture(Order.ApiResponseOrderMonthlyTotalRevenue.getDefaultInstance()));
        handler.findMonthlyTotalRevenue(ctx);
        verify(queryClient).findMonthlyTotalRevenue(any(Order.FindYearMonthTotalRevenue.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void restoreAll_shouldCallGrpc() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(commandClient.restoreAllOrder(any())).thenReturn(Future.succeededFuture(Order.ApiResponseOrderAll.getDefaultInstance()));
        handler.restoreAll(ctx);
        verify(commandClient).restoreAllOrder(any(com.google.protobuf.Empty.class));
        verify(response).setStatusCode(200);
    }
}
