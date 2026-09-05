package io.example.order_item.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemResponseDeleteAt {
    private Long id;
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private Integer price;
    private String createdAt;
    private String updatedAt;
    private String deletedAt;

    public static OrderItemResponseDeleteAt from(OrderItem item) {
        if (item == null)
            return null;
        return OrderItemResponseDeleteAt.builder()
                .id(item.getOrderItemId())
                .orderId(item.getOrderId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .createdAt(item.getCreatedAt() != null ? item.getCreatedAt().toInstant().toString() : "")
                .updatedAt(item.getUpdatedAt() != null ? item.getUpdatedAt().toInstant().toString() : "")
                .deletedAt(item.getDeletedAt() != null ? item.getDeletedAt().toInstant().toString() : null)
                .build();
    }
}
