package io.example.category.domain.response.category;

import io.example.category.model.CategoryMonthTotalPrice;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriesMonthlyTotalPriceResponse {
    private String year;
    private String month;
    private Long totalRevenue;

    public static CategoriesMonthlyTotalPriceResponse from(CategoryMonthTotalPrice response) {
        return CategoriesMonthlyTotalPriceResponse.builder()
                .year(response.getYear())
                .month(response.getMonth())
                .totalRevenue((long) response.getTotalRevenue())
                .build();
    }

    public static List<CategoriesMonthlyTotalPriceResponse> fromList(
            List<CategoryMonthTotalPrice> response) {
        if (response == null)
            return List.of();
        return response.stream().map(CategoriesMonthlyTotalPriceResponse::from).toList();
    }
}
