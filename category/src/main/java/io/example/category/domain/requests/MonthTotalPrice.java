package io.example.category.domain.requests;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MonthTotalPrice {
    private Integer year;
    private Integer month;
}
