package io.example.merchant.domain.requests;

import lombok.Data;

@Data
public class CreateMerchantRequest {
    private String name;
    private Integer userId;
}
