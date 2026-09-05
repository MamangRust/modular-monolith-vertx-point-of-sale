package io.example.transaction.domain.response;

import io.example.transaction.model.TransactionYearlyAmountFailed;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionYearlyAmountFailedResponse {
    private String year;
    private Integer totalFailed;
    private Long totalAmount;

    public static TransactionYearlyAmountFailedResponse from(TransactionYearlyAmountFailed entity) {
        return TransactionYearlyAmountFailedResponse.builder()
                .year(entity.getYear())
                .totalFailed(entity.getTotalFailed())
                .totalAmount(entity.getTotalAmount())
                .build();
    }
}