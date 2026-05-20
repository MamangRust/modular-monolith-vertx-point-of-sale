package io.example.merchant.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

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
public class Merchant {
    private Integer merchantId;
    private String merchantNo;
    private String name;
    private String apiKey;
    private Integer userId;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("merchantId", merchantId)
                .put("merchantNo", merchantNo)
                .put("name", name)
                .put("apiKey", apiKey)
                .put("userId", userId)
                .put("status", status);

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

    public static Merchant fromJson(JsonObject json) {
        if (json == null) {
            return null;
        }

        Merchant merchant = new Merchant();
        merchant.setMerchantId(json.getInteger("merchantId"));
        merchant.setMerchantNo(json.getString("merchantNo"));
        merchant.setName(json.getString("name"));
        merchant.setApiKey(json.getString("apiKey"));
        merchant.setUserId(json.getInteger("userId"));
        merchant.setStatus(json.getString("status"));
        merchant.setCreatedAt(parseTimestamp(json, "createdAt"));
        merchant.setUpdatedAt(parseTimestamp(json, "updatedAt"));
        merchant.setDeletedAt(parseTimestamp(json, "deletedAt"));

        return merchant;
    }

    public static Merchant fromRow(Row row) {
        if (row == null) {
            return null;
        }

        Integer merchantId = row.getInteger("merchant_id");
        if (merchantId == null) {
            merchantId = row.getInteger("merchantId");
        }

        String merchantNo = null;
        try {
            UUID uuid = row.getUUID("merchant_no");
            if (uuid != null) {
                merchantNo = uuid.toString();
            }
        } catch (Exception e) {
            merchantNo = row.getString("merchant_no");
        }
        if (merchantNo == null) {
            merchantNo = row.getString("merchantNo");
        }

        String name = row.getString("name");

        String apiKey = row.getString("api_key");
        if (apiKey == null) {
            apiKey = row.getString("apiKey");
        }

        Integer userId = row.getInteger("user_id");
        if (userId == null) {
            userId = row.getInteger("userId");
        }

        String status = row.getString("status");

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

        return Merchant.builder()
                .merchantId(merchantId)
                .merchantNo(merchantNo)
                .name(name)
                .apiKey(apiKey)
                .userId(userId)
                .status(status)
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
