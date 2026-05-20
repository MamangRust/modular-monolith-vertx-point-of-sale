package io.example.category.domain.requests;

import lombok.Data;

@Data
public class MonthTotalPriceMerchant {
    private Integer merchantId;
    private Integer year;
    private Integer month;
}
