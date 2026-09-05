package io.example.merchant.domain.requests;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class UpdateMerchantStatusRequest {
    private Integer merchantId;
    private String status;
}
