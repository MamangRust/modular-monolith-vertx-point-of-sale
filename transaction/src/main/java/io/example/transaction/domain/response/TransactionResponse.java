package io.example.transaction.domain.response;

import io.example.transaction.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private Integer orderId;
    private Integer merchantId;
    private String paymentMethod;
    private Integer amount;
    private Integer changeAmount;
    private String paymentStatus;
    private String createdAt;
    private String updatedAt;

    public static TransactionResponse from(Transaction entity) {
        return TransactionResponse.builder()
                .id(entity.getTransactionId())
                .orderId(entity.getOrderId().intValue())
                .merchantId(entity.getMerchantId().intValue())
                .paymentMethod(entity.getPaymentMethod())
                .amount(entity.getAmount())
                .changeAmount(entity.getChangeAmount())
                .paymentStatus(entity.getStatus().toString())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .build();
    }
}