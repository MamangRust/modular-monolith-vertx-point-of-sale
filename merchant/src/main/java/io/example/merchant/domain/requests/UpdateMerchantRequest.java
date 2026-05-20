package io.example.merchant.domain.requests;

import lombok.Data;

@Data
public class UpdateMerchantRequest {
    private Integer merchantId;
    private String name;
    private Integer userId;
    private String status;
}
