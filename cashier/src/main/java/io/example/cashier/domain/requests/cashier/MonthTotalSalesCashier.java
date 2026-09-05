package io.example.cashier.domain.requests.cashier;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthTotalSalesCashier {
    private Integer cashierId;

    private Integer year;

    private Integer month;
}
