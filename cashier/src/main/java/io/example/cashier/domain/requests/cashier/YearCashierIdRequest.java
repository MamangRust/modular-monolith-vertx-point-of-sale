package io.example.cashier.domain.requests.cashier;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YearCashierIdRequest {
    private Integer cashierId;

    private Integer year;
}