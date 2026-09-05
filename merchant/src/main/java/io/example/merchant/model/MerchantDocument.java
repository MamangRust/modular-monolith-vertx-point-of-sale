package io.example.merchant.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantDocument {
    private Integer documentId;
    private Integer merchantId;
    private String documentType;
    private String documentUrl;
    private String status;
    private String note;
    private Timestamp uploadedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("documentId", documentId)
                .put("merchantId", merchantId)
                .put("documentType", documentType)
                .put("documentUrl", documentUrl)
                .put("status", status)
                .put("note", note);

        if (uploadedAt != null) {
            json.put("uploadedAt", uploadedAt.toString());
        }
        if (createdAt != null) {
            json.put("createdAt", createdAt.toString());
        }
        if (updatedAt != null) {
            json.put("updatedAt", updatedAt.toString());
        }
        if (deletedAt != null) {
            json.put("deletedAt", deletedAt.toString());
        }

        return json;
    }

    public static MerchantDocument fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }

        MerchantDocument doc = new MerchantDocument();
        doc.setDocumentId(json.getInteger("documentId"));
        doc.setMerchantId(json.getInteger("merchantId"));
        doc.setDocumentType(json.getString("documentType"));
        doc.setDocumentUrl(json.getString("documentUrl"));
        doc.setStatus(json.getString("status"));
        doc.setNote(json.getString("note"));
        doc.setUploadedAt(parseTimestamp(json, "uploadedAt"));
        doc.setCreatedAt(parseTimestamp(json, "createdAt"));
        doc.setUpdatedAt(parseTimestamp(json, "updatedAt"));
        doc.setDeletedAt(parseTimestamp(json, "deletedAt"));

        return doc;
    }

    public static MerchantDocument fromRow(Row row) {
        if (row == null) {
            return null;
        }

        Integer documentId = row.getInteger("document_id");
        if (documentId == null) {
            documentId = row.getInteger("documentId");
        }

        Integer merchantId = row.getInteger("merchant_id");
        if (merchantId == null) {
            merchantId = row.getInteger("merchantId");
        }

        String documentType = row.getString("document_type");
        if (documentType == null) {
            documentType = row.getString("documentType");
        }

        String documentUrl = row.getString("document_url");
        if (documentUrl == null) {
            documentUrl = row.getString("documentUrl");
        }

        String status = row.getString("status");
        String note = row.getString("note");

        Timestamp uploadedAt = null;
        LocalDateTime uploadedAtLocal = row.get(LocalDateTime.class, "uploaded_at");
        if (uploadedAtLocal != null) {
            uploadedAt = Timestamp.valueOf(uploadedAtLocal);
        }

        Timestamp createdAt = null;
        LocalDateTime createdAtLocal = row.get(LocalDateTime.class, "created_at");
        if (createdAtLocal != null) {
            createdAt = Timestamp.valueOf(createdAtLocal);
        }

        Timestamp updatedAt = null;
        LocalDateTime updatedAtLocal = row.get(LocalDateTime.class, "updated_at");
        if (updatedAtLocal != null) {
            updatedAt = Timestamp.valueOf(updatedAtLocal);
        }

        Timestamp deletedAt = null;
        LocalDateTime deletedAtLocal = row.get(LocalDateTime.class, "deleted_at");
        if (deletedAtLocal != null) {
            deletedAt = Timestamp.valueOf(deletedAtLocal);
        }

        return MerchantDocument.builder()
                .documentId(documentId)
                .merchantId(merchantId)
                .documentType(documentType)
                .documentUrl(documentUrl)
                .status(status)
                .note(note)
                .uploadedAt(uploadedAt)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .deletedAt(deletedAt)
                .build();
    }

    private static Timestamp parseTimestamp(JsonObject json, String field) {
        Object value = json.getValue(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp ts) {
            return ts;
        }
        if (value instanceof String str && !str.isBlank()) {
            try {
                return Timestamp.from(Instant.parse(str));
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        if (value instanceof Number num) {
            return new Timestamp(num.longValue());
        }
        return null;
    }

    @Override
    public String toString() {
        return toJson().encode();
    }
}
