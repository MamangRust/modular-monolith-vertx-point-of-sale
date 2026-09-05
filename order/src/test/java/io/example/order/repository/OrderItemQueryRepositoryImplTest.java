package io.example.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.order.model.OrderItem;
import io.example.order.repository.impl.OrderItemQueryRepositoryImpl;
import io.vertx.core.Future;
import pb.order_item.VertxOrderItemServiceGrpcClient;
import pb.order_item.OrderItem.ApiResponsesOrderItem;
import pb.order_item.OrderItem.FindByIdOrderItemRequest;
import pb.order_item.OrderItem.OrderItemResponse;

@ExtendWith(MockitoExtension.class)
class OrderItemQueryRepositoryImplTest {

    @Mock
    private VertxOrderItemServiceGrpcClient client;

    private OrderItemQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new OrderItemQueryRepositoryImpl(client);
    }

    @Test
    void findOrderItemByOrder_shouldReturnList() {
        OrderItemResponse protoItem = OrderItemResponse.newBuilder()
                .setId(1)
                .setOrderId(10)
                .setProductId(100)
                .setQuantity(2)
                .setPrice(5000)
                .build();
        ApiResponsesOrderItem response = ApiResponsesOrderItem.newBuilder()
                .addData(protoItem)
                .build();

        when(client.findOrderItemByOrder(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<List<OrderItem>> result = repository.findOrderItemByOrder(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getOrderItemId()).isEqualTo(1L);
        assertThat(result.result().get(0).getOrderId()).isEqualTo(10L);
        assertThat(result.result().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.result().get(0).getPrice()).isEqualTo(5000);
    }

    @Test
    void findOrderItemByOrder_shouldReturnEmptyListWhenNoData() {
        ApiResponsesOrderItem response = ApiResponsesOrderItem.newBuilder().build();

        when(client.findOrderItemByOrder(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<List<OrderItem>> result = repository.findOrderItemByOrder(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEmpty();
    }

    @Test
    void findOrderItemByOrder_shouldReturnEmptyListWhenNullOrderId() {
        Future<List<OrderItem>> result = repository.findOrderItemByOrder(null);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEmpty();
    }

    @Test
    void findOrderItemByOrder_shouldReturnEmptyListOnGrpcError() {
        when(client.findOrderItemByOrder(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.failedFuture(new RuntimeException("gRPC error")));

        Future<List<OrderItem>> result = repository.findOrderItemByOrder(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEmpty();
    }

    @Test
    void calculateTotalPrice_shouldSumQuantityTimesPrice() {
        OrderItemResponse protoItem1 = OrderItemResponse.newBuilder()
                .setId(1)
                .setOrderId(10)
                .setProductId(100)
                .setQuantity(2)
                .setPrice(5000)
                .build();
        OrderItemResponse protoItem2 = OrderItemResponse.newBuilder()
                .setId(2)
                .setOrderId(10)
                .setProductId(101)
                .setQuantity(3)
                .setPrice(3000)
                .build();
        ApiResponsesOrderItem response = ApiResponsesOrderItem.newBuilder()
                .addData(protoItem1)
                .addData(protoItem2)
                .build();

        when(client.findOrderItemByOrder(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Integer> result = repository.calculateTotalPrice(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(19000); // (2*5000) + (3*3000)
    }

    @Test
    void calculateTotalPrice_shouldReturnZeroForEmptyItems() {
        ApiResponsesOrderItem response = ApiResponsesOrderItem.newBuilder().build();

        when(client.findOrderItemByOrder(any(FindByIdOrderItemRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Integer> result = repository.calculateTotalPrice(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isZero();
    }
}
