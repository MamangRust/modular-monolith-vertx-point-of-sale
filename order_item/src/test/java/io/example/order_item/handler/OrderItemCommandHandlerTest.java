package io.example.order_item.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;

import io.example.order_item.domain.requests.CreateOrderItemRequest;
import io.example.order_item.domain.requests.UpdateOrderItemRequest;
import io.example.order_item.domain.response.order_item.OrderItemResponse;
import io.example.order_item.domain.response.order_item.OrderItemResponseDeleteAt;
import io.example.order_item.service.OrderItemCommandService;
import io.vertx.core.Future;

import pb.order_item.OrderItem.ApiResponseOrderItem;
import pb.order_item.OrderItem.ApiResponseOrderItemAll;
import pb.order_item.OrderItem.ApiResponseOrderItemDelete;
import pb.order_item.OrderItem.ApiResponseOrderItemDeleteAt;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;

@ExtendWith(MockitoExtension.class)
class OrderItemCommandHandlerTest {

    @Mock private OrderItemCommandService commandService;

    private OrderItemCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new OrderItemCommandHandler(commandService);
    }

    @Test
    void createOrderItem_shouldCallServiceAndReturnSuccess() {
        pb.order_item.OrderItemCommand.CreateOrderItemRequest request =
                pb.order_item.OrderItemCommand.CreateOrderItemRequest.newBuilder()
                        .setOrderId(10)
                        .setProductId(100)
                        .setQuantity(2)
                        .setPrice(5000)
                        .build();

        OrderItemResponse responseDto = OrderItemResponse.builder()
                .id(1L)
                .orderId(10)
                .productId(100)
                .quantity(2)
                .price(5000)
                .build();

        when(commandService.create(any(CreateOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseOrderItem> result = commandHandler.createOrderItem(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getOrderId()).isEqualTo(10);
        verify(commandService).create(any(CreateOrderItemRequest.class));
    }

    @Test
    void updateOrderItem_shouldCallServiceAndReturnSuccess() {
        pb.order_item.OrderItemCommand.UpdateOrderItemRequest request =
                pb.order_item.OrderItemCommand.UpdateOrderItemRequest.newBuilder()
                        .setOrderItemId(1)
                        .setOrderId(10)
                        .setProductId(100)
                        .setQuantity(5)
                        .setPrice(7500)
                        .build();

        OrderItemResponse responseDto = OrderItemResponse.builder()
                .id(1L)
                .orderId(10)
                .productId(100)
                .quantity(5)
                .price(7500)
                .build();

        when(commandService.update(any(UpdateOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseOrderItem> result = commandHandler.updateOrderItem(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getQuantity()).isEqualTo(5);
        verify(commandService).update(any(UpdateOrderItemRequest.class));
    }

    @Test
    void trashedOrderItem_shouldCallServiceAndReturnSuccess() {
        FindByIdOrderItemRequest request = FindByIdOrderItemRequest.newBuilder()
                .setOrderItemId(1)
                .build();

        OrderItemResponseDeleteAt responseDto = OrderItemResponseDeleteAt.builder()
                .id(1L)
                .orderId(10)
                .productId(100)
                .deletedAt("2024-01-02")
                .build();

        when(commandService.trash(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseOrderItemDeleteAt> result = commandHandler.trashedOrderItem(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getDeletedAt().getValue()).isEqualTo("2024-01-02");
        verify(commandService).trash(1L);
    }

    @Test
    void restoreOrderItem_shouldCallServiceAndReturnSuccess() {
        FindByIdOrderItemRequest request = FindByIdOrderItemRequest.newBuilder()
                .setOrderItemId(1)
                .build();

        OrderItemResponseDeleteAt responseDto = OrderItemResponseDeleteAt.builder()
                .id(1L)
                .orderId(10)
                .productId(100)
                .deletedAt(null)
                .build();

        when(commandService.restore(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseOrderItemDeleteAt> result = commandHandler.restoreOrderItem(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().hasDeletedAt()).isFalse();
        verify(commandService).restore(1L);
    }

    @Test
    void deleteOrderItemPermanent_shouldCallServiceAndReturnSuccess() {
        FindByIdOrderItemRequest request = FindByIdOrderItemRequest.newBuilder()
                .setOrderItemId(1)
                .build();

        when(commandService.deletePermanent(1L)).thenReturn(Future.succeededFuture());

        Future<ApiResponseOrderItemDelete> result = commandHandler.deleteOrderItemPermanent(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).deletePermanent(1L);
    }

    @Test
    void restoreAllOrderItem_shouldCallServiceAndReturnSuccess() {
        when(commandService.restoreAll()).thenReturn(Future.succeededFuture());

        Future<ApiResponseOrderItemAll> result = commandHandler.restoreAllOrderItem(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).restoreAll();
    }

    @Test
    void deleteAllOrderItemPermanent_shouldCallServiceAndReturnSuccess() {
        when(commandService.deleteAllPermanent()).thenReturn(Future.succeededFuture());

        Future<ApiResponseOrderItemAll> result = commandHandler.deleteAllOrderItemPermanent(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).deleteAllPermanent();
    }
}
