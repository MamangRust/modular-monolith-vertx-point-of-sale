package io.example.order.domain.response.order;

import java.util.List;

import io.example.order.model.OrderYearTotalRevenue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderYearlyTotalRevenueResponse {
    private String year;
    private Long totalRevenue;

    public static OrderYearlyTotalRevenueResponse from(OrderYearTotalRevenue response) {
        return OrderYearlyTotalRevenueResponse.builder()
                .year(response.getYear())
                .totalRevenue((long) response.getTotalRevenue())
                .build();
    }

    public static List<OrderYearlyTotalRevenueResponse> fromList(List<OrderYearTotalRevenue> response) {
        if (response == null)
            return List.of();
        return response.stream().map(OrderYearlyTotalRevenueResponse::from).toList();
    }
}