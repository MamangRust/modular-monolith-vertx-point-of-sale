package io.example.transaction.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.transaction.enums.PaymentStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

@ExtendWith(MockitoExtension.class)
class TransactionTest {

  @Mock
  Row row;

  @Test
  void fromRow_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 0, 0);
    LocalDateTime deletedAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(row.getLong("transaction_id")).thenReturn(1L);
    when(row.getLong("order_id")).thenReturn(1L);
    when(row.getLong("merchant_id")).thenReturn(1L);
    when(row.getString("payment_method")).thenReturn("credit_card");
    when(row.getInteger("amount")).thenReturn(50000);
    when(row.getInteger("change_amount")).thenReturn(0);
    when(row.getString("payment_status")).thenReturn("SUCCESS");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(createdAt);
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(updatedAt);
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(deletedAt);

    Transaction t = Transaction.fromRow(row);

    assertThat(t).isNotNull();
    assertThat(t.getTransactionId()).isEqualTo(1L);
    assertThat(t.getOrderId()).isEqualTo(1L);
    assertThat(t.getMerchantId()).isEqualTo(1L);
    assertThat(t.getPaymentMethod()).isEqualTo("credit_card");
    assertThat(t.getAmount()).isEqualTo(50000);
    assertThat(t.getChangeAmount()).isEqualTo(0);
    assertThat(t.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(t.getCreatedAt()).isEqualTo(Timestamp.valueOf(createdAt));
    assertThat(t.getUpdatedAt()).isEqualTo(Timestamp.valueOf(updatedAt));
    assertThat(t.getDeletedAt()).isEqualTo(Timestamp.valueOf(deletedAt));
  }

  @Test
  void fromRow_shouldReturnNullForNullRow() {
    assertThat(Transaction.fromRow(null)).isNull();
  }

  @Test
  void fromRow_shouldHandleNullTimestamps() {
    when(row.getLong("transaction_id")).thenReturn(2L);
    when(row.getLong("order_id")).thenReturn(2L);
    when(row.getLong("merchant_id")).thenReturn(2L);
    when(row.getString("payment_method")).thenReturn("debit_card");
    when(row.getInteger("amount")).thenReturn(30000);
    when(row.getInteger("change_amount")).thenReturn(5000);
    when(row.getString("payment_status")).thenReturn("PENDING");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(null);
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(null);
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(null);

    Transaction t = Transaction.fromRow(row);

    assertThat(t.getCreatedAt()).isNull();
    assertThat(t.getUpdatedAt()).isNull();
    assertThat(t.getDeletedAt()).isNull();
  }

  @Test
  void fromRow_shouldHandleUnknownStatus() {
    when(row.getLong("transaction_id")).thenReturn(3L);
    when(row.getLong("order_id")).thenReturn(3L);
    when(row.getLong("merchant_id")).thenReturn(3L);
    when(row.getString("payment_method")).thenReturn("cash");
    when(row.getInteger("amount")).thenReturn(10000);
    when(row.getInteger("change_amount")).thenReturn(0);
    when(row.getString("payment_status")).thenReturn("UNKNOWN");

    Transaction t = Transaction.fromRow(row);

    assertThat(t.getStatus()).isEqualTo(PaymentStatus.PENDING);
  }

  @Test
  void fromJson_shouldDeserialize() {
    JsonObject json = new JsonObject()
        .put("transactionId", 1L)
        .put("orderId", 1L)
        .put("merchantId", 1L)
        .put("paymentMethod", "credit_card")
        .put("amount", 50000)
        .put("changeAmount", 0)
        .put("status", "SUCCESS")
        .put("createdAt", "2024-01-01T00:00:00Z")
        .put("updatedAt", "2024-06-01T00:00:00Z");

    Transaction t = Transaction.fromJson(json);

    assertThat(t).isNotNull();
    assertThat(t.getTransactionId()).isEqualTo(1L);
    assertThat(t.getOrderId()).isEqualTo(1L);
    assertThat(t.getMerchantId()).isEqualTo(1L);
    assertThat(t.getPaymentMethod()).isEqualTo("credit_card");
    assertThat(t.getAmount()).isEqualTo(50000);
    assertThat(t.getChangeAmount()).isEqualTo(0);
    assertThat(t.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(t.getCreatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-01-01T00:00:00Z")));
    assertThat(t.getUpdatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-06-01T00:00:00Z")));
    assertThat(t.getDeletedAt()).isNull();
  }

  @Test
  void fromJson_shouldReturnNullForNullJson() {
    assertThat(Transaction.fromJson(null)).isNull();
  }

  @Test
  void fromJson_shouldHandleMissingFields() {
    JsonObject json = new JsonObject()
        .put("transactionId", 1L);

    Transaction t = Transaction.fromJson(json);

    assertThat(t).isNotNull();
    assertThat(t.getTransactionId()).isEqualTo(1L);
    assertThat(t.getOrderId()).isNull();
    assertThat(t.getMerchantId()).isNull();
    assertThat(t.getPaymentMethod()).isNull();
    assertThat(t.getAmount()).isNull();
    assertThat(t.getChangeAmount()).isNull();
    assertThat(t.getStatus()).isNull();
    assertThat(t.getCreatedAt()).isNull();
    assertThat(t.getUpdatedAt()).isNull();
    assertThat(t.getDeletedAt()).isNull();
  }

  @Test
  void fromJson_shouldHandleUnknownStatus() {
    JsonObject json = new JsonObject()
        .put("transactionId", 1L)
        .put("status", "INVALID");

    Transaction t = Transaction.fromJson(json);

    assertThat(t.getStatus()).isEqualTo(PaymentStatus.PENDING);
  }

  @Test
  void fromJson_shouldHandleDifferentTimestampFormats() {
    JsonObject json = new JsonObject()
        .put("transactionId", 1L)
        .put("createdAt", "2024-01-01T00:00:00Z");

    Transaction t = Transaction.fromJson(json);

    assertThat(t.getCreatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-01-01T00:00:00Z")));
  }

  @Test
  void toJson_shouldSerializeAllFields() {
    Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
    Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));
    Timestamp deletedAt = Timestamp.valueOf(LocalDateTime.of(2024, 7, 1, 0, 0));

    Transaction t = Transaction.builder()
        .transactionId(1L)
        .orderId(1L)
        .merchantId(1L)
        .paymentMethod("credit_card")
        .amount(50000)
        .changeAmount(0)
        .status(PaymentStatus.SUCCESS)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();

    JsonObject json = t.toJson();

    assertThat(json.getLong("transactionId")).isEqualTo(1L);
    assertThat(json.getLong("orderId")).isEqualTo(1L);
    assertThat(json.getLong("merchantId")).isEqualTo(1L);
    assertThat(json.getString("paymentMethod")).isEqualTo("credit_card");
    assertThat(json.getInteger("amount")).isEqualTo(50000);
    assertThat(json.getInteger("changeAmount")).isEqualTo(0);
    assertThat(json.getString("status")).isEqualTo("SUCCESS");
    assertThat(json.getString("createdAt")).isEqualTo(createdAt.toString());
    assertThat(json.getString("updatedAt")).isEqualTo(updatedAt.toString());
    assertThat(json.getString("deletedAt")).isEqualTo(deletedAt.toString());
  }

  @Test
  void toJson_shouldSkipNullDates() {
    Transaction t = Transaction.builder()
        .transactionId(2L)
        .orderId(2L)
        .merchantId(2L)
        .paymentMethod("debit_card")
        .amount(30000)
        .changeAmount(5000)
        .status(PaymentStatus.PENDING)
        .build();

    JsonObject json = t.toJson();

    assertThat(json.getLong("transactionId")).isEqualTo(2L);
    assertThat(json.getLong("orderId")).isEqualTo(2L);
    assertThat(json.getLong("merchantId")).isEqualTo(2L);
    assertThat(json.getString("paymentMethod")).isEqualTo("debit_card");
    assertThat(json.getInteger("amount")).isEqualTo(30000);
    assertThat(json.getInteger("changeAmount")).isEqualTo(5000);
    assertThat(json.getString("status")).isEqualTo("PENDING");
    assertThat(json.containsKey("createdAt")).isFalse();
    assertThat(json.containsKey("updatedAt")).isFalse();
    assertThat(json.containsKey("deletedAt")).isFalse();
  }

  @Test
  void toJson_shouldHandleNullStatus() {
    Transaction t = Transaction.builder()
        .transactionId(1L)
        .orderId(1L)
        .merchantId(1L)
        .paymentMethod("credit_card")
        .amount(50000)
        .changeAmount(0)
        .build();

    JsonObject json = t.toJson();

    assertThat(json.getString("status")).isNull();
  }

  @Test
  void builder_shouldCreateTransaction() {
    Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
    Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));
    Timestamp deletedAt = Timestamp.valueOf(LocalDateTime.of(2024, 7, 1, 0, 0));

    Transaction t = Transaction.builder()
        .transactionId(1L)
        .orderId(1L)
        .merchantId(1L)
        .paymentMethod("credit_card")
        .amount(50000)
        .changeAmount(0)
        .status(PaymentStatus.SUCCESS)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();

    assertThat(t.getTransactionId()).isEqualTo(1L);
    assertThat(t.getOrderId()).isEqualTo(1L);
    assertThat(t.getMerchantId()).isEqualTo(1L);
    assertThat(t.getPaymentMethod()).isEqualTo("credit_card");
    assertThat(t.getAmount()).isEqualTo(50000);
    assertThat(t.getChangeAmount()).isEqualTo(0);
    assertThat(t.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    assertThat(t.getCreatedAt()).isEqualTo(createdAt);
    assertThat(t.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(t.getDeletedAt()).isEqualTo(deletedAt);
  }
}
