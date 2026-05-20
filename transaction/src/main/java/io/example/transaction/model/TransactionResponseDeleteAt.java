package io.example.transaction.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDeleteAt {
    private Long id;
    private String cardNumber;
    private String transactionNo;
    private Integer amount;
    private String paymentMethod;
    private Integer merchantId;
    private String transactionTime;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static TransactionResponseDeleteAt from(Transaction entity) {
        if (entity == null) return null;
        return TransactionResponseDeleteAt.builder()
                .id(entity.getTransactionId())
                .cardNumber(entity.getCardNumber())
                .transactionNo(entity.getTransactionNo())
                .amount(entity.getAmount())
                .paymentMethod(entity.getPaymentMethod())
                .merchantId(entity.getMerchantId())
                .transactionTime(entity.getTransactionTime() != null ? entity.getTransactionTime().toString() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .deletedAt(entity.getDeletedAt() != null ? entity.getDeletedAt().toString() : null)
                .build();
    }
}
