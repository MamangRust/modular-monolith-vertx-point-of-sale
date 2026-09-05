package io.example.order.domain.response.order;

import io.example.order.model.Order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDeleteAt {

    private Long id;
    private Integer merchantId;
    private Integer cashierId;
    private Long totalPrice;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static OrderResponseDeleteAt from(Order entity) {
        return OrderResponseDeleteAt.builder()
                .id(entity.getOrderId())
                .merchantId(entity.getMerchantId().intValue())
                .cashierId(entity.getCashierId().intValue())
                .totalPrice(entity.getTotalPrice())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .deletedAt(entity.getDeletedAt() != null ? entity.getDeletedAt().toString() : null)
                .build();
    }
}