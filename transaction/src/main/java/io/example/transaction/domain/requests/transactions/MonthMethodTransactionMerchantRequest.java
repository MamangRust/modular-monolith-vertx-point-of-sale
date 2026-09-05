package io.example.transaction.domain.requests.transactions;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthMethodTransactionMerchantRequest {
    private Integer merchantId;

    private Integer year;

    private Integer month;
}
