package io.example.order.domain.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderItemRequest {
    @JsonProperty("product_id")
    private Long productId;

    @JsonProperty("quantity")
    private int quantity;
}
