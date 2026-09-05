package io.example.apigateway.handler;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.order_item.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemProxyHandlerTest {
    @Mock VertxOrderItemServiceGrpcClient queryClient;
    @Mock RoutingContext ctx;
    @Mock io.vertx.core.http.HttpServerResponse response;
    private OrderItemProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderItemProxyHandler(queryClient);
    }

    @Test
    void findAll_shouldCallGrpc() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("search","test").add("page","1").add("pageSize","10"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findAll(any())).thenReturn(Future.succeededFuture(OrderItemQuery.ApiResponsePaginationOrderItem.getDefaultInstance()));
        handler.findAll(ctx);
        verify(queryClient).findAll(any(OrderItem.FindAllOrderItemRequest.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void findByOrder_shouldCallGrpc() {
        when(ctx.pathParam("orderId")).thenReturn("15");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findOrderItemByOrder(any())).thenReturn(Future.succeededFuture(OrderItem.ApiResponsesOrderItem.getDefaultInstance()));
        handler.findByOrder(ctx);
        var captor = ArgumentCaptor.forClass(OrderItem.FindByIdOrderItemRequest.class);
        verify(queryClient).findOrderItemByOrder(captor.capture());
        assertThat(captor.getValue().getOrderItemId()).isEqualTo(15);
        verify(response).setStatusCode(200);
    }

    @Test
    void findActive_shouldCallGrpc() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findByActive(any())).thenReturn(Future.succeededFuture(OrderItemQuery.ApiResponsePaginationOrderItemDeleteAt.getDefaultInstance()));
        handler.findActive(ctx);
        verify(queryClient).findByActive(any(OrderItem.FindAllOrderItemRequest.class));
        verify(response).setStatusCode(200);
    }
}
