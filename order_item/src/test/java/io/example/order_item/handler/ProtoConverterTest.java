package io.example.order_item.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import io.example.order_item.domain.response.order_item.OrderItemResponse;
import io.example.order_item.domain.response.order_item.OrderItemResponseDeleteAt;
import io.example.order_item.model.OrderItem;

class ProtoConverterTest {

    @Test
    void fromOrderItemResponse_shouldMapAllFields() {
        OrderItemResponse dto = OrderItemResponse.builder()
                .id(1L)
                .orderId(10)
                .productId(100)
                .quantity(2)
                .price(5000)
                .createdAt("2024-01-01")
                .updatedAt("2024-06-01")
                .build();

        pb.order_item.OrderItem.OrderItemResponse response = ProtoConverter.fromOrderItemResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getOrderId()).isEqualTo(10);
        assertThat(response.getProductId()).isEqualTo(100);
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getPrice()).isEqualTo(5000);
        assertThat(response.getCreatedAt()).isEqualTo("2024-01-01");
        assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01");
    }

    @Test
    void fromOrderItemResponse_shouldHandleNull() {
        pb.order_item.OrderItem.OrderItemResponse response = ProtoConverter.fromOrderItemResponse(null);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(0);
    }

    @Test
    void fromOrderItemResponseDeleteAt_shouldIncludeDeletedAt() {
        OrderItemResponseDeleteAt dto = OrderItemResponseDeleteAt.builder()
                .id(5L)
                .orderId(10)
                .productId(100)
                .quantity(3)
                .price(7500)
                .createdAt("2024-01-01")
                .updatedAt("2024-02-01")
                .deletedAt("2024-03-01")
                .build();

        pb.order_item.OrderItem.OrderItemResponseDeleteAt response = ProtoConverter.fromOrderItemResponseDeleteAt(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5);
        assertThat(response.getOrderId()).isEqualTo(10);
        assertThat(response.getProductId()).isEqualTo(100);
        assertThat(response.getQuantity()).isEqualTo(3);
        assertThat(response.getPrice()).isEqualTo(7500);
        assertThat(response.hasDeletedAt()).isTrue();
        assertThat(response.getDeletedAt().getValue()).isEqualTo("2024-03-01");
    }

    @Test
    void fromOrderItemResponseDeleteAt_shouldHandleNullDeletedAt() {
        OrderItemResponseDeleteAt dto = OrderItemResponseDeleteAt.builder()
                .id(5L)
                .orderId(10)
                .productId(100)
                .quantity(3)
                .price(7500)
                .build();

        pb.order_item.OrderItem.OrderItemResponseDeleteAt response = ProtoConverter.fromOrderItemResponseDeleteAt(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5);
        assertThat(response.hasDeletedAt()).isFalse();
    }

    @Test
    void fromOrderItemResponseDeleteAt_shouldHandleNull() {
        pb.order_item.OrderItem.OrderItemResponseDeleteAt response = ProtoConverter.fromOrderItemResponseDeleteAt(null);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(0);
    }
}
