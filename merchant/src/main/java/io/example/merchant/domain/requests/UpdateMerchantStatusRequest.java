package io.example.merchant.domain.requests;

import lombok.Data;

@Data
public class UpdateMerchantStatusRequest {
    private Integer merchantId;
    private String status;
}
