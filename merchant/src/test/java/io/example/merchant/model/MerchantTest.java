package io.example.merchant.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.merchant.model.MerchantDocument;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

@ExtendWith(MockitoExtension.class)
class MerchantTest {

  @Mock
  Row row;

  @Test
  void fromRow_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 0, 0);
    LocalDateTime deletedAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(row.getLong("merchant_id")).thenReturn(1L);
    when(row.getLong("user_id")).thenReturn(1L);
    when(row.getString("name")).thenReturn("Merchant A");
    when(row.getString("description")).thenReturn("Description");
    when(row.getString("address")).thenReturn("Address");
    when(row.getString("contact_email")).thenReturn("email@test.com");
    when(row.getString("contact_phone")).thenReturn("081234");
    when(row.getString("status")).thenReturn("ACTIVE");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(createdAt);
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(updatedAt);
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(deletedAt);

    Merchant merchant = Merchant.fromRow(row);

    assertThat(merchant).isNotNull();
    assertThat(merchant.getMerchantId()).isEqualTo(1L);
    assertThat(merchant.getUserId()).isEqualTo(1L);
    assertThat(merchant.getName()).isEqualTo("Merchant A");
    assertThat(merchant.getDescription()).isEqualTo("Description");
    assertThat(merchant.getAddress()).isEqualTo("Address");
    assertThat(merchant.getContactEmail()).isEqualTo("email@test.com");
    assertThat(merchant.getContactPhone()).isEqualTo("081234");
    assertThat(merchant.getStatus()).isEqualTo(io.example.merchant.enums.Status.ACTIVE);
    assertThat(merchant.getCreatedAt()).isEqualTo(Timestamp.valueOf(createdAt));
    assertThat(merchant.getUpdatedAt()).isEqualTo(Timestamp.valueOf(updatedAt));
    assertThat(merchant.getDeletedAt()).isEqualTo(Timestamp.valueOf(deletedAt));
  }

  @Test
  void fromRow_shouldReturnNullForNullRow() {
    assertThat(Merchant.fromRow(null)).isNull();
  }

  @Test
  void fromRow_shouldHandleNullStatus() {
    when(row.getLong("merchant_id")).thenReturn(2L);
    when(row.getLong("user_id")).thenReturn(2L);
    when(row.getString("name")).thenReturn("Merchant B");
    when(row.getString("status")).thenReturn(null);

    Merchant merchant = Merchant.fromRow(row);

    assertThat(merchant).isNotNull();
    assertThat(merchant.getStatus()).isNull();
  }

  @Test
  void fromJson_shouldDeserialize() {
    JsonObject json = new JsonObject()
        .put("merchantId", 1)
        .put("userId", 1)
        .put("name", "Merchant A")
        .put("description", "Description")
        .put("address", "Address")
        .put("contactEmail", "email@test.com")
        .put("contactPhone", "081234")
        .put("status", "ACTIVE")
        .put("createdAt", "2024-01-01T00:00:00Z")
        .put("updatedAt", "2024-06-01T00:00:00Z");

    Merchant merchant = Merchant.fromJson(json);

    assertThat(merchant).isNotNull();
    assertThat(merchant.getMerchantId()).isEqualTo(1L);
    assertThat(merchant.getUserId()).isEqualTo(1L);
    assertThat(merchant.getName()).isEqualTo("Merchant A");
    assertThat(merchant.getDescription()).isEqualTo("Description");
    assertThat(merchant.getAddress()).isEqualTo("Address");
    assertThat(merchant.getContactEmail()).isEqualTo("email@test.com");
    assertThat(merchant.getContactPhone()).isEqualTo("081234");
    assertThat(merchant.getStatus()).isEqualTo(io.example.merchant.enums.Status.ACTIVE);
    assertThat(merchant.getCreatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-01-01T00:00:00Z")));
    assertThat(merchant.getUpdatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-06-01T00:00:00Z")));
    assertThat(merchant.getDeletedAt()).isNull();
  }

  @Test
  void fromJson_shouldReturnNullForNullJson() {
    assertThat(Merchant.fromJson(null)).isNull();
  }

  @Test
  void fromJson_shouldHandleInvalidStatus() {
    JsonObject json = new JsonObject()
        .put("merchantId", 1)
        .put("status", "UNKNOWN_STATUS");

    Merchant merchant = Merchant.fromJson(json);

    assertThat(merchant).isNotNull();
    assertThat(merchant.getStatus()).isEqualTo(io.example.merchant.enums.Status.PENDING);
  }

  @Test
  void toJson_shouldSerializeAllFields() {
    Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
    Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));

    Merchant merchant = Merchant.builder()
        .merchantId(1L)
        .userId(1L)
        .name("Merchant A")
        .description("Description")
        .address("Address")
        .contactEmail("email@test.com")
        .contactPhone("081234")
        .status(io.example.merchant.enums.Status.ACTIVE)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();

    JsonObject json = merchant.toJson();

    assertThat(json.getLong("merchantId")).isEqualTo(1L);
    assertThat(json.getLong("userId")).isEqualTo(1L);
    assertThat(json.getString("name")).isEqualTo("Merchant A");
    assertThat(json.getString("description")).isEqualTo("Description");
    assertThat(json.getString("address")).isEqualTo("Address");
    assertThat(json.getString("contactEmail")).isEqualTo("email@test.com");
    assertThat(json.getString("contactPhone")).isEqualTo("081234");
    assertThat(json.getString("status")).isEqualTo("ACTIVE");
    assertThat(json.getString("createdAt")).isEqualTo(createdAt.toString());
    assertThat(json.getString("updatedAt")).isEqualTo(updatedAt.toString());
  }

  @Test
  void toJson_shouldSkipNullDates() {
    Merchant merchant = Merchant.builder()
        .merchantId(2L)
        .userId(2L)
        .name("Merchant B")
        .build();

    JsonObject json = merchant.toJson();

    assertThat(json.getLong("merchantId")).isEqualTo(2L);
    assertThat(json.getString("name")).isEqualTo("Merchant B");
    assertThat(json.containsKey("createdAt")).isFalse();
    assertThat(json.containsKey("updatedAt")).isFalse();
    assertThat(json.containsKey("deletedAt")).isFalse();
  }

  @Test
  void toJson_shouldSkipNullStatus() {
    Merchant merchant = Merchant.builder()
        .merchantId(3L)
        .userId(3L)
        .name("Merchant C")
        .build();

    JsonObject json = merchant.toJson();

    assertThat(json.getValue("status")).isNull();
  }

  @Test
  void builder_shouldCreateMerchant() {
    Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
    Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));
    Timestamp deletedAt = Timestamp.valueOf(LocalDateTime.of(2024, 7, 1, 0, 0));

    Merchant merchant = Merchant.builder()
        .merchantId(1L)
        .userId(1L)
        .name("Merchant A")
        .description("Description")
        .address("Address")
        .contactEmail("email@test.com")
        .contactPhone("081234")
        .status(io.example.merchant.enums.Status.ACTIVE)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();

    assertThat(merchant.getMerchantId()).isEqualTo(1L);
    assertThat(merchant.getUserId()).isEqualTo(1L);
    assertThat(merchant.getName()).isEqualTo("Merchant A");
    assertThat(merchant.getDescription()).isEqualTo("Description");
    assertThat(merchant.getAddress()).isEqualTo("Address");
    assertThat(merchant.getContactEmail()).isEqualTo("email@test.com");
    assertThat(merchant.getContactPhone()).isEqualTo("081234");
    assertThat(merchant.getStatus()).isEqualTo(io.example.merchant.enums.Status.ACTIVE);
    assertThat(merchant.getCreatedAt()).isEqualTo(createdAt);
    assertThat(merchant.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(merchant.getDeletedAt()).isEqualTo(deletedAt);
  }

  @Nested
  class MerchantDocumentTest {

    @Mock
    Row docRow;

    @Test
    void fromRow_shouldMapAllFields() {
      LocalDateTime uploadedAt = LocalDateTime.of(2024, 1, 15, 10, 30);
      LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
      LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 0, 0);
      LocalDateTime deletedAt = LocalDateTime.of(2024, 7, 1, 0, 0);

      when(docRow.getInteger("document_id")).thenReturn(1);
      when(docRow.getInteger("merchant_id")).thenReturn(1);
      when(docRow.getString("document_type")).thenReturn("NPWP");
      when(docRow.getString("document_url")).thenReturn("https://storage.example.com/doc1.pdf");
      when(docRow.getString("status")).thenReturn("APPROVED");
      when(docRow.getString("note")).thenReturn("All good");
      when(docRow.get(LocalDateTime.class, "uploaded_at")).thenReturn(uploadedAt);
      when(docRow.get(LocalDateTime.class, "created_at")).thenReturn(createdAt);
      when(docRow.get(LocalDateTime.class, "updated_at")).thenReturn(updatedAt);
      when(docRow.get(LocalDateTime.class, "deleted_at")).thenReturn(deletedAt);

      MerchantDocument doc = MerchantDocument.fromRow(docRow);

      assertThat(doc).isNotNull();
      assertThat(doc.getDocumentId()).isEqualTo(1);
      assertThat(doc.getMerchantId()).isEqualTo(1);
      assertThat(doc.getDocumentType()).isEqualTo("NPWP");
      assertThat(doc.getDocumentUrl()).isEqualTo("https://storage.example.com/doc1.pdf");
      assertThat(doc.getStatus()).isEqualTo("APPROVED");
      assertThat(doc.getNote()).isEqualTo("All good");
      assertThat(doc.getUploadedAt()).isEqualTo(Timestamp.valueOf(uploadedAt));
      assertThat(doc.getCreatedAt()).isEqualTo(Timestamp.valueOf(createdAt));
      assertThat(doc.getUpdatedAt()).isEqualTo(Timestamp.valueOf(updatedAt));
      assertThat(doc.getDeletedAt()).isEqualTo(Timestamp.valueOf(deletedAt));
    }

    @Test
    void fromRow_shouldReturnNullForNullRow() {
      assertThat(MerchantDocument.fromRow(null)).isNull();
    }

    @Test
    void fromRow_shouldFallbackToCamelCaseColumns() {
      when(docRow.getInteger("document_id")).thenReturn(null);
      when(docRow.getInteger("documentId")).thenReturn(2);
      when(docRow.getInteger("merchant_id")).thenReturn(null);
      when(docRow.getInteger("merchantId")).thenReturn(2);
      when(docRow.getString("document_type")).thenReturn(null);
      when(docRow.getString("documentType")).thenReturn("SIUP");
      when(docRow.getString("document_url")).thenReturn(null);
      when(docRow.getString("documentUrl")).thenReturn("https://storage.example.com/doc2.pdf");
      when(docRow.getString("status")).thenReturn("PENDING");
      when(docRow.getString("note")).thenReturn("Under review");

      MerchantDocument doc = MerchantDocument.fromRow(docRow);

      assertThat(doc).isNotNull();
      assertThat(doc.getDocumentId()).isEqualTo(2);
      assertThat(doc.getMerchantId()).isEqualTo(2);
      assertThat(doc.getDocumentType()).isEqualTo("SIUP");
      assertThat(doc.getDocumentUrl()).isEqualTo("https://storage.example.com/doc2.pdf");
      assertThat(doc.getStatus()).isEqualTo("PENDING");
      assertThat(doc.getNote()).isEqualTo("Under review");
    }

    @Test
    void fromJson_shouldDeserialize() {
      JsonObject json = new JsonObject()
          .put("documentId", 1)
          .put("merchantId", 1)
          .put("documentType", "NPWP")
          .put("documentUrl", "https://storage.example.com/doc1.pdf")
          .put("status", "APPROVED")
          .put("note", "All good")
          .put("uploadedAt", "2024-01-15T10:30:00Z")
          .put("createdAt", "2024-01-01T00:00:00Z")
          .put("updatedAt", "2024-06-01T00:00:00Z");

      MerchantDocument doc = MerchantDocument.fromJson(json);

      assertThat(doc).isNotNull();
      assertThat(doc.getDocumentId()).isEqualTo(1);
      assertThat(doc.getMerchantId()).isEqualTo(1);
      assertThat(doc.getDocumentType()).isEqualTo("NPWP");
      assertThat(doc.getDocumentUrl()).isEqualTo("https://storage.example.com/doc1.pdf");
      assertThat(doc.getStatus()).isEqualTo("APPROVED");
      assertThat(doc.getNote()).isEqualTo("All good");
      assertThat(doc.getUploadedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-01-15T10:30:00Z")));
      assertThat(doc.getCreatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-01-01T00:00:00Z")));
      assertThat(doc.getUpdatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-06-01T00:00:00Z")));
      assertThat(doc.getDeletedAt()).isNull();
    }

    @Test
    void fromJson_shouldReturnNullForNullJson() {
      assertThat(MerchantDocument.fromJson(null)).isNull();
    }

    @Test
    void toJson_shouldSerializeAllFields() {
      Timestamp uploadedAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 15, 10, 30));
      Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
      Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));

      MerchantDocument doc = MerchantDocument.builder()
          .documentId(1)
          .merchantId(1)
          .documentType("NPWP")
          .documentUrl("https://storage.example.com/doc1.pdf")
          .status("APPROVED")
          .note("All good")
          .uploadedAt(uploadedAt)
          .createdAt(createdAt)
          .updatedAt(updatedAt)
          .build();

      JsonObject json = doc.toJson();

      assertThat(json.getInteger("documentId")).isEqualTo(1);
      assertThat(json.getInteger("merchantId")).isEqualTo(1);
      assertThat(json.getString("documentType")).isEqualTo("NPWP");
      assertThat(json.getString("documentUrl")).isEqualTo("https://storage.example.com/doc1.pdf");
      assertThat(json.getString("status")).isEqualTo("APPROVED");
      assertThat(json.getString("note")).isEqualTo("All good");
      assertThat(json.getString("uploadedAt")).isEqualTo(uploadedAt.toString());
      assertThat(json.getString("createdAt")).isEqualTo(createdAt.toString());
      assertThat(json.getString("updatedAt")).isEqualTo(updatedAt.toString());
    }

    @Test
    void toJson_shouldSkipNullDates() {
      MerchantDocument doc = MerchantDocument.builder()
          .documentId(2)
          .merchantId(2)
          .documentType("SIUP")
          .build();

      JsonObject json = doc.toJson();

      assertThat(json.getInteger("documentId")).isEqualTo(2);
      assertThat(json.getString("documentType")).isEqualTo("SIUP");
      assertThat(json.containsKey("uploadedAt")).isFalse();
      assertThat(json.containsKey("createdAt")).isFalse();
      assertThat(json.containsKey("updatedAt")).isFalse();
      assertThat(json.containsKey("deletedAt")).isFalse();
    }

    @Test
    void builder_shouldCreateDocument() {
      Timestamp uploadedAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 15, 10, 30));
      Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
      Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));
      Timestamp deletedAt = Timestamp.valueOf(LocalDateTime.of(2024, 7, 1, 0, 0));

      MerchantDocument doc = MerchantDocument.builder()
          .documentId(1)
          .merchantId(1)
          .documentType("NPWP")
          .documentUrl("https://storage.example.com/doc1.pdf")
          .status("APPROVED")
          .note("All good")
          .uploadedAt(uploadedAt)
          .createdAt(createdAt)
          .updatedAt(updatedAt)
          .deletedAt(deletedAt)
          .build();

      assertThat(doc.getDocumentId()).isEqualTo(1);
      assertThat(doc.getMerchantId()).isEqualTo(1);
      assertThat(doc.getDocumentType()).isEqualTo("NPWP");
      assertThat(doc.getDocumentUrl()).isEqualTo("https://storage.example.com/doc1.pdf");
      assertThat(doc.getStatus()).isEqualTo("APPROVED");
      assertThat(doc.getNote()).isEqualTo("All good");
      assertThat(doc.getUploadedAt()).isEqualTo(uploadedAt);
      assertThat(doc.getCreatedAt()).isEqualTo(createdAt);
      assertThat(doc.getUpdatedAt()).isEqualTo(updatedAt);
      assertThat(doc.getDeletedAt()).isEqualTo(deletedAt);
    }
  }
}
