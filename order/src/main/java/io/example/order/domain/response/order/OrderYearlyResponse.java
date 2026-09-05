package io.example.order.domain.response.order;

import java.util.List;

import io.example.order.model.OrderYear;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderYearlyResponse {
    private String year;
    private Integer orderCount;
    private Long totalRevenue;
    private Integer totalItemsSold;
    private Integer activeCashiers;
    private Integer uniqueProductsSold;

    public static OrderYearlyResponse from(OrderYear response) {
        return OrderYearlyResponse.builder()
                .year(response.getYear())
                .orderCount(response.getOrderCount())
                .totalRevenue((long) response.getTotalRevenue())
                .totalItemsSold(response.getTotalItemsSold())
                .activeCashiers(response.getActiveCashiers())
                .uniqueProductsSold(response.getUniqueProductsSold())
                .build();
    }

    public static List<OrderYearlyResponse> fromList(List<OrderYear> response) {
        if (response == null)
            return List.of();
        return response.stream().map(OrderYearlyResponse::from).toList();
    }
}