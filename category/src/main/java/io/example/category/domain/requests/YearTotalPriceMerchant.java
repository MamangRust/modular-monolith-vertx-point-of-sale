package io.example.category.domain.requests;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class YearTotalPriceMerchant {
    private Integer merchantId;
    private Integer year;
}
