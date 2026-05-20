package io.example.category.domain.requests;

import lombok.Data;

@Data
public class MonthTotalPriceCategory {
    private Integer categoryId;
    private Integer year;
    private Integer month;
}
