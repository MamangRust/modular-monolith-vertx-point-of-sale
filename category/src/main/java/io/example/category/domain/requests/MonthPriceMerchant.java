package io.example.category.domain.requests;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthPriceMerchant {
    private Integer merchantId;
    private Integer year;
}
