package io.example.order.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderItemRecordRequest {
    private Long orderId;
    private Long productId;
    private int quantity;
    private int price;
}
