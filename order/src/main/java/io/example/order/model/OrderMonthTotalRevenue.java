package io.example.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMonthTotalRevenue {
    private String year;
    private String month;
    private Integer totalRevenue;
}
