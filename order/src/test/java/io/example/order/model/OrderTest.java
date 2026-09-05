package io.example.order.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

@ExtendWith(MockitoExtension.class)
class OrderTest {

  @Mock
  Row row;

  @Test
  void fromRow_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 0, 0);
    LocalDateTime deletedAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(row.getLong("order_id")).thenReturn(1L);
    when(row.getLong("merchant_id")).thenReturn(1L);
    when(row.getLong("cashier_id")).thenReturn(1L);
    when(row.getLong("total_price")).thenReturn(5000L);
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(createdAt);
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(updatedAt);
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(deletedAt);

    Order order = Order.fromRow(row);

    assertThat(order).isNotNull();
    assertThat(order.getOrderId()).isEqualTo(1L);
    assertThat(order.getMerchantId()).isEqualTo(1L);
    assertThat(order.getCashierId()).isEqualTo(1L);
    assertThat(order.getTotalPrice()).isEqualTo(5000L);
    assertThat(order.getCreatedAt()).isEqualTo(Timestamp.valueOf(createdAt));
    assertThat(order.getUpdatedAt()).isEqualTo(Timestamp.valueOf(updatedAt));
    assertThat(order.getDeletedAt()).isEqualTo(Timestamp.valueOf(deletedAt));
  }

  @Test
  void fromRow_shouldReturnNullForNullRow() {
    assertThat(Order.fromRow(null)).isNull();
  }

  @Test
  void fromRow_shouldHandleNullTimestamps() {
    when(row.getLong("order_id")).thenReturn(2L);
    when(row.getLong("merchant_id")).thenReturn(2L);
    when(row.getLong("cashier_id")).thenReturn(2L);
    when(row.getLong("total_price")).thenReturn(3000L);
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(null);
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(null);
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);

    Order order = Order.fromRow(row);

    assertThat(order.getCreatedAt()).isNull();
    assertThat(order.getUpdatedAt()).isNull();
    assertThat(order.getDeletedAt()).isNull();
  }

  @Test
  void fromJson_shouldDeserialize() {
    JsonObject json = new JsonObject()
        .put("orderId", 1)
        .put("merchantId", 1)
        .put("cashierId", 1)
        .put("totalPrice", 5000)
        .put("createdAt", "2024-01-01T00:00:00Z")
        .put("updatedAt", "2024-06-01T00:00:00Z");

    Order order = Order.fromJson(json);

    assertThat(order).isNotNull();
    assertThat(order.getOrderId()).isEqualTo(1L);
    assertThat(order.getMerchantId()).isEqualTo(1L);
    assertThat(order.getCashierId()).isEqualTo(1L);
    assertThat(order.getTotalPrice()).isEqualTo(5000L);
    assertThat(order.getCreatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-01-01T00:00:00Z")));
    assertThat(order.getUpdatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-06-01T00:00:00Z")));
    assertThat(order.getDeletedAt()).isNull();
  }

  @Test
  void fromJson_shouldReturnNullForNullJson() {
    assertThat(Order.fromJson(null)).isNull();
  }

  @Test
  void fromJson_shouldHandleMissingFields() {
    JsonObject json = new JsonObject()
        .put("orderId", 1);

    Order order = Order.fromJson(json);

    assertThat(order).isNotNull();
    assertThat(order.getOrderId()).isEqualTo(1L);
    assertThat(order.getMerchantId()).isNull();
    assertThat(order.getCashierId()).isNull();
    assertThat(order.getTotalPrice()).isNull();
    assertThat(order.getCreatedAt()).isNull();
    assertThat(order.getUpdatedAt()).isNull();
    assertThat(order.getDeletedAt()).isNull();
  }

  @Test
  void toJson_shouldSerializeAllFields() {
    Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
    Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));

    Order order = Order.builder()
        .orderId(1L)
        .merchantId(1L)
        .cashierId(1L)
        .totalPrice(5000L)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();

    JsonObject json = order.toJson();

    assertThat(json.getLong("orderId")).isEqualTo(1L);
    assertThat(json.getLong("merchantId")).isEqualTo(1L);
    assertThat(json.getLong("cashierId")).isEqualTo(1L);
    assertThat(json.getLong("totalPrice")).isEqualTo(5000L);
    assertThat(json.getString("createdAt")).isEqualTo(createdAt.toString());
    assertThat(json.getString("updatedAt")).isEqualTo(updatedAt.toString());
  }

  @Test
  void toJson_shouldSkipNullDates() {
    Order order = Order.builder()
        .orderId(2L)
        .merchantId(2L)
        .cashierId(2L)
        .totalPrice(3000L)
        .build();

    JsonObject json = order.toJson();

    assertThat(json.getLong("orderId")).isEqualTo(2L);
    assertThat(json.getLong("merchantId")).isEqualTo(2L);
    assertThat(json.getLong("cashierId")).isEqualTo(2L);
    assertThat(json.getLong("totalPrice")).isEqualTo(3000L);
    assertThat(json.containsKey("createdAt")).isFalse();
    assertThat(json.containsKey("updatedAt")).isFalse();
    assertThat(json.containsKey("deletedAt")).isFalse();
  }

  @Test
  void builder_shouldCreateOrder() {
    Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
    Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));
    Timestamp deletedAt = Timestamp.valueOf(LocalDateTime.of(2024, 7, 1, 0, 0));

    Order order = Order.builder()
        .orderId(1L)
        .merchantId(1L)
        .cashierId(1L)
        .totalPrice(5000L)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();

    assertThat(order.getOrderId()).isEqualTo(1L);
    assertThat(order.getMerchantId()).isEqualTo(1L);
    assertThat(order.getCashierId()).isEqualTo(1L);
    assertThat(order.getTotalPrice()).isEqualTo(5000L);
    assertThat(order.getCreatedAt()).isEqualTo(createdAt);
    assertThat(order.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(order.getDeletedAt()).isEqualTo(deletedAt);
  }
}
