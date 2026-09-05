package io.example.order.domain.requests;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateOrderRequest {
    @JsonProperty("order_id")
    private Integer orderId;

    @JsonProperty("cashier_id")
    private Integer cashierId;

    @JsonProperty("items")
    private List<UpdateOrderItemRequest> items;
}
