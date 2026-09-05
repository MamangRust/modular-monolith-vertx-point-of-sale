package io.example.category.domain.requests;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthTotalPriceCategory {
    private Integer categoryId;
    private Integer year;
    private Integer month;
}
