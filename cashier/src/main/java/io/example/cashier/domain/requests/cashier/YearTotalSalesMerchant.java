package io.example.cashier.domain.requests.cashier;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YearTotalSalesMerchant {
    private Integer merchantId;

    private Integer year;
}
