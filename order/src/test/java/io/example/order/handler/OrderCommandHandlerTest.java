package io.example.order.handler;

import com.google.protobuf.Empty;

import io.example.order.domain.response.order.OrderResponse;
import io.example.order.domain.response.order.OrderResponseDeleteAt;
import io.example.order.service.OrderCommandService;
import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.order.Order.ApiResponseOrder;
import pb.order.Order.ApiResponseOrderDeleteAt;
import pb.order.Order.CreateOrderRequest;
import pb.order.Order.FindByIdOrderRequest;
import pb.order.Order.UpdateOrderRequest;
import pb.order.Order.ApiResponseOrderAll;
import pb.order.Order.ApiResponseOrderDelete;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCommandHandlerTest {

    @Mock
    private OrderCommandService commandService;

    private OrderCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderCommandHandler(commandService);
    }

    @Test
    void create_shouldReturnApiResponse() {
        OrderResponse resp = new OrderResponse(1L, 1, 1, 5000L, "2024-01-01", "2024-06-01");
        doReturn(Future.succeededFuture(resp)).when(commandService).createOrder(any());

        CreateOrderRequest req = CreateOrderRequest.newBuilder()
                .setMerchantId(1)
                .setCashierId(1)
                .build();

        Future<ApiResponseOrder> future = handler.create(req);

        assertTrue(future.succeeded());
        ApiResponseOrder result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Order created successfully", result.getMessage());
        assertNotNull(result.getData());

        verify(commandService).createOrder(any());
    }

    @Test
    void update_shouldReturnApiResponse() {
        OrderResponse resp = new OrderResponse(1L, 1, 1, 5000L, "2024-01-01", "2024-06-01");
        doReturn(Future.succeededFuture(resp)).when(commandService).updateOrder(any());

        UpdateOrderRequest req = UpdateOrderRequest.newBuilder()
                .setOrderId(1)
                .setCashierId(1)
                .build();

        Future<ApiResponseOrder> future = handler.update(req);

        assertTrue(future.succeeded());
        ApiResponseOrder result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Order updated successfully", result.getMessage());
        assertNotNull(result.getData());

        verify(commandService).updateOrder(any());
    }

    @Test
    void trashedOrder_shouldReturnApiResponse() {
        OrderResponseDeleteAt resp = new OrderResponseDeleteAt(1L, 1, 1, 5000L,
                "2024-01-01", "2024-06-01", "2024-07-01");
        doReturn(Future.succeededFuture(resp)).when(commandService).trashedOrder(any());

        FindByIdOrderRequest req = FindByIdOrderRequest.newBuilder()
                .setId(1)
                .build();

        Future<ApiResponseOrderDeleteAt> future = handler.trashedOrder(req);

        assertTrue(future.succeeded());
        ApiResponseOrderDeleteAt result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Order trashed successfully", result.getMessage());
        assertNotNull(result.getData());

        verify(commandService).trashedOrder(any());
    }

    @Test
    void restoreOrder_shouldReturnApiResponse() {
        OrderResponseDeleteAt resp = new OrderResponseDeleteAt(1L, 1, 1, 5000L,
                "2024-01-01", "2024-06-01", "2024-07-01");
        doReturn(Future.succeededFuture(resp)).when(commandService).restoreOrder(any());

        FindByIdOrderRequest req = FindByIdOrderRequest.newBuilder()
                .setId(1)
                .build();

        Future<ApiResponseOrderDeleteAt> future = handler.restoreOrder(req);

        assertTrue(future.succeeded());
        ApiResponseOrderDeleteAt result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Order restored successfully", result.getMessage());
        assertNotNull(result.getData());

        verify(commandService).restoreOrder(any());
    }

    @Test
    void deleteOrderPermanent_shouldReturnApiResponse() {
        doReturn(Future.succeededFuture(true)).when(commandService).deleteOrderPermanent(any());

        FindByIdOrderRequest req = FindByIdOrderRequest.newBuilder()
                .setId(1)
                .build();

        Future<ApiResponseOrderDelete> future = handler.deleteOrderPermanent(req);

        assertTrue(future.succeeded());
        ApiResponseOrderDelete result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Order deleted permanently", result.getMessage());

        verify(commandService).deleteOrderPermanent(any());
    }

    @Test
    void restoreAllOrder_shouldReturnApiResponse() {
        doReturn(Future.succeededFuture(true)).when(commandService).restoreAllOrder();

        Future<ApiResponseOrderAll> future = handler.restoreAllOrder(Empty.getDefaultInstance());

        assertTrue(future.succeeded());
        ApiResponseOrderAll result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("All orders restored", result.getMessage());

        verify(commandService).restoreAllOrder();
    }

    @Test
    void deleteAllOrderPermanent_shouldReturnApiResponse() {
        doReturn(Future.succeededFuture(true)).when(commandService).deleteAllOrderPermanent();

        Future<ApiResponseOrderAll> future = handler.deleteAllOrderPermanent(Empty.getDefaultInstance());

        assertTrue(future.succeeded());
        ApiResponseOrderAll result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("All orders permanently deleted", result.getMessage());

        verify(commandService).deleteAllOrderPermanent();
    }
}
