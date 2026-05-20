package io.example.category.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryYearTotalPrice {
    private String year;
    private Long totalRevenue;
}
