package io.example.transaction.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import io.example.transaction.enums.PaymentStatus;
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
public class Transaction {
    private Long transactionId;
    private String transactionNo;
    private String cardNumber;
    private Integer amount;
    private String paymentMethod;
    private Integer merchantId;
    private Timestamp transactionTime;
    private PaymentStatus status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private Timestamp deletedAt;

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("transactionId", transactionId)
                .put("transactionNo", transactionNo)
                .put("cardNumber", cardNumber)
                .put("amount", amount)
                .put("paymentMethod", paymentMethod)
                .put("merchantId", merchantId)
                .put("status", status != null ? status.name() : null);

        if (transactionTime != null) json.put("transactionTime", transactionTime.toString());
        if (createdAt != null) json.put("createdAt", createdAt.toString());
        if (updatedAt != null) json.put("updatedAt", updatedAt.toString());
        if (deletedAt != null) json.put("deletedAt", deletedAt.toString());

        return json;
    }

    public static Transaction fromJson(JsonObject json) {
        if (json == null) return null;

        Transaction t = new Transaction();
        t.setTransactionId(json.getLong("transactionId"));
        t.setTransactionNo(json.getString("transactionNo"));
        t.setCardNumber(json.getString("cardNumber"));
        t.setAmount(json.getInteger("amount"));
        t.setPaymentMethod(json.getString("paymentMethod"));
        t.setMerchantId(json.getInteger("merchantId"));
        
        String statusStr = json.getString("status");
        if (statusStr != null) {
            try { t.setStatus(PaymentStatus.valueOf(statusStr.toUpperCase())); } 
            catch (IllegalArgumentException e) { t.setStatus(PaymentStatus.PENDING); }
        }

        t.setTransactionTime(parseTimestamp(json, "transactionTime"));
        t.setCreatedAt(parseTimestamp(json, "createdAt"));
        t.setUpdatedAt(parseTimestamp(json, "updatedAt"));
        t.setDeletedAt(parseTimestamp(json, "deletedAt"));

        return t;
    }

    public static Transaction fromRow(Row row) {
        if (row == null) return null;

        PaymentStatus status = null;
        String statusStr = row.getString("status");
        if (statusStr != null) {
            try { status = PaymentStatus.valueOf(statusStr.toUpperCase()); } 
            catch (IllegalArgumentException e) { status = PaymentStatus.PENDING; }
        }

        Long txId = row.getLong("transaction_id");
        if (txId == null) txId = row.getLong("transactionId");

        String txNo = row.getString("transaction_no");
        if (txNo == null) txNo = row.getString("transactionNo");

        String cn = row.getString("card_number");
        if (cn == null) cn = row.getString("cardNumber");

        Integer amt = row.getInteger("amount");

        String pm = row.getString("payment_method");
        if (pm == null) pm = row.getString("paymentMethod");

        Integer merId = row.getInteger("merchant_id");
        if (merId == null) merId = row.getInteger("merchantId");

        return Transaction.builder()
                .transactionId(txId)
                .transactionNo(txNo)
                .cardNumber(cn)
                .amount(amt)
                .paymentMethod(pm)
                .merchantId(merId)
                .status(status)
                .transactionTime(row.get(LocalDateTime.class, "transaction_time") != null ? Timestamp.valueOf(row.get(LocalDateTime.class, "transaction_time")) : null)
                .createdAt(row.get(LocalDateTime.class, "created_at") != null ? Timestamp.valueOf(row.get(LocalDateTime.class, "created_at")) : null)
                .updatedAt(row.get(LocalDateTime.class, "updated_at") != null ? Timestamp.valueOf(row.get(LocalDateTime.class, "updated_at")) : null)
                .deletedAt(row.get(LocalDateTime.class, "deleted_at") != null ? Timestamp.valueOf(row.get(LocalDateTime.class, "deleted_at")) : null)
                .build();
    }

    private static Timestamp parseTimestamp(JsonObject json, String field) {
        Object value = json.getValue(field);
        if (value == null) return null;
        if (value instanceof Timestamp ts) return ts;
        if (value instanceof String str && !str.isBlank()) {
            try { return Timestamp.from(Instant.parse(str)); } catch (DateTimeParseException e) { return null; }
        }
        if (value instanceof Number num) return new Timestamp(num.longValue());
        return null;
    }
}
