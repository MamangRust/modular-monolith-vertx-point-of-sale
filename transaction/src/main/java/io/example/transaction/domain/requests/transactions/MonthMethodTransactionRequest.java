package io.example.transaction.domain.requests.transactions;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthMethodTransactionRequest {
    private Integer year;

    private Integer month;
}
