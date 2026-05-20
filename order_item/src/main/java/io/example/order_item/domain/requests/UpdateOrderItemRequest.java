package io.example.order_item.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderItemRequest {
    private Long orderItemId;
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private Integer price;
}
