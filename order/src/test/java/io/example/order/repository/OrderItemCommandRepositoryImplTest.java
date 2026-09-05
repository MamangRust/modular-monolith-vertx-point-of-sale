package io.example.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.order.domain.requests.CreateOrderItemRecordRequest;
import io.example.order.domain.requests.UpdateOrderItemRecordRequest;
import io.example.order.model.OrderItem;
import io.example.order.repository.impl.OrderItemCommandRepositoryImpl;
import io.vertx.core.Future;
import pb.order_item.VertxOrderItemCommandServiceGrpcClient;
import pb.order_item.OrderItem.ApiResponseOrderItem;
import pb.order_item.OrderItem.ApiResponseOrderItemAll;
import pb.order_item.OrderItem.ApiResponseOrderItemDelete;
import pb.order_item.OrderItem.ApiResponseOrderItemDeleteAt;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;
import pb.order_item.OrderItem.OrderItemResponse;
import pb.order_item.OrderItem.OrderItemResponseDeleteAt;
import pb.order_item.OrderItemCommand.CreateOrderItemRequest;
import pb.order_item.OrderItemCommand.UpdateOrderItemRequest;

@ExtendWith(MockitoExtension.class)
class OrderItemCommandRepositoryImplTest {

    @Mock
    private VertxOrderItemCommandServiceGrpcClient client;

    private OrderItemCommandRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new OrderItemCommandRepositoryImpl(client);
    }

    @Test
    void createOrderItem_shouldReturnOrderItem() {
        CreateOrderItemRecordRequest req = CreateOrderItemRecordRequest.builder()
                .orderId(1L)
                .productId(2L)
                .quantity(3)
                .price(5000)
                .build();

        OrderItemResponse protoData = OrderItemResponse.newBuilder()
                .setId(10)
                .setOrderId(1)
                .setProductId(2)
                .setQuantity(3)
                .setPrice(5000)
                .build();
        ApiResponseOrderItem response = ApiResponseOrderItem.newBuilder()
                .setData(protoData)
                .build();

        when(client.createOrderItem(any(CreateOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<OrderItem> result = repository.createOrderItem(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getOrderItemId()).isEqualTo(10L);
        assertThat(result.result().getOrderId()).isEqualTo(1L);
        assertThat(result.result().getQuantity()).isEqualTo(3);
        assertThat(result.result().getPrice()).isEqualTo(5000);
    }

    @Test
    void createOrderItem_shouldReturnNullWhenNoData() {
        CreateOrderItemRecordRequest req = CreateOrderItemRecordRequest.builder()
                .orderId(1L)
                .productId(2L)
                .quantity(3)
                .price(5000)
                .build();

        ApiResponseOrderItem response = ApiResponseOrderItem.newBuilder().build();

        when(client.createOrderItem(any(CreateOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<OrderItem> result = repository.createOrderItem(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void updateOrderItem_shouldReturnOrderItem() {
        UpdateOrderItemRecordRequest req = UpdateOrderItemRecordRequest.builder()
                .orderItemId(10L)
                .quantity(5)
                .price(7500)
                .build();

        OrderItemResponse protoData = OrderItemResponse.newBuilder()
                .setId(10)
                .setOrderId(1)
                .setProductId(2)
                .setQuantity(5)
                .setPrice(7500)
                .build();
        ApiResponseOrderItem response = ApiResponseOrderItem.newBuilder()
                .setData(protoData)
                .build();

        when(client.updateOrderItem(any(UpdateOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<OrderItem> result = repository.updateOrderItem(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getOrderItemId()).isEqualTo(10L);
        assertThat(result.result().getQuantity()).isEqualTo(5);
        assertThat(result.result().getPrice()).isEqualTo(7500);
    }

    @Test
    void updateOrderItem_shouldReturnNullWhenNoData() {
        UpdateOrderItemRecordRequest req = UpdateOrderItemRecordRequest.builder()
                .orderItemId(999L)
                .quantity(5)
                .price(7500)
                .build();

        ApiResponseOrderItem response = ApiResponseOrderItem.newBuilder().build();

        when(client.updateOrderItem(any(UpdateOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<OrderItem> result = repository.updateOrderItem(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void trashedOrderItem_shouldReturnOrderItem() {
        OrderItemResponseDeleteAt protoData = OrderItemResponseDeleteAt.newBuilder()
                .setId(10)
                .setOrderId(1)
                .setProductId(2)
                .setQuantity(3)
                .setPrice(5000)
                .build();
        ApiResponseOrderItemDeleteAt response = ApiResponseOrderItemDeleteAt.newBuilder()
                .setData(protoData)
                .build();

        when(client.trashedOrderItem(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<OrderItem> result = repository.trashedOrderItem(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getOrderItemId()).isEqualTo(10L);
    }

    @Test
    void trashedOrderItem_shouldReturnNullWhenNoData() {
        ApiResponseOrderItemDeleteAt response = ApiResponseOrderItemDeleteAt.newBuilder().build();

        when(client.trashedOrderItem(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<OrderItem> result = repository.trashedOrderItem(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void restoreOrderItem_shouldReturnOrderItem() {
        OrderItemResponseDeleteAt protoData = OrderItemResponseDeleteAt.newBuilder()
                .setId(10)
                .setOrderId(1)
                .setProductId(2)
                .setQuantity(3)
                .setPrice(5000)
                .build();
        ApiResponseOrderItemDeleteAt response = ApiResponseOrderItemDeleteAt.newBuilder()
                .setData(protoData)
                .build();

        when(client.restoreOrderItem(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<OrderItem> result = repository.restoreOrderItem(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getOrderItemId()).isEqualTo(10L);
    }

    @Test
    void restoreOrderItem_shouldReturnNullWhenNoData() {
        ApiResponseOrderItemDeleteAt response = ApiResponseOrderItemDeleteAt.newBuilder().build();

        when(client.restoreOrderItem(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<OrderItem> result = repository.restoreOrderItem(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void deleteOrderItemPermanent_shouldReturnTrueOnSuccess() {
        ApiResponseOrderItemDelete response = ApiResponseOrderItemDelete.newBuilder()
                .setStatus("success")
                .build();

        when(client.deleteOrderItemPermanent(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.deleteOrderItemPermanent(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void deleteOrderItemPermanent_shouldReturnFalseOnFailure() {
        ApiResponseOrderItemDelete response = ApiResponseOrderItemDelete.newBuilder()
                .setStatus("failed")
                .build();

        when(client.deleteOrderItemPermanent(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.deleteOrderItemPermanent(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void restoreAllOrderItem_shouldReturnTrueOnSuccess() {
        ApiResponseOrderItemAll response = ApiResponseOrderItemAll.newBuilder()
                .setStatus("success")
                .build();

        when(client.restoreAllOrderItem(any(com.google.protobuf.Empty.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.restoreAllOrderItem();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void restoreAllOrderItem_shouldReturnFalseOnFailure() {
        ApiResponseOrderItemAll response = ApiResponseOrderItemAll.newBuilder()
                .setStatus("failed")
                .build();

        when(client.restoreAllOrderItem(any(com.google.protobuf.Empty.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.restoreAllOrderItem();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void deleteAllOrderPermanent_shouldReturnTrueOnSuccess() {
        ApiResponseOrderItemAll response = ApiResponseOrderItemAll.newBuilder()
                .setStatus("success")
                .build();

        when(client.deleteAllOrderItemPermanent(any(com.google.protobuf.Empty.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.deleteAllOrderPermanent();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void deleteAllOrderPermanent_shouldReturnFalseOnFailure() {
        ApiResponseOrderItemAll response = ApiResponseOrderItemAll.newBuilder()
                .setStatus("failed")
                .build();

        when(client.deleteAllOrderItemPermanent(any(com.google.protobuf.Empty.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.deleteAllOrderPermanent();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }
}
