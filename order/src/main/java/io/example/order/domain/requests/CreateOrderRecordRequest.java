package io.example.order.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRecordRequest {
    private Long merchantId;
    private Long cashierId;
    private int totalPrice;
}
