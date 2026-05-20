package io.example.order.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthTotalRevenueMerchant {
    private Long merchantId;
    private int year;
    private int month;
}
