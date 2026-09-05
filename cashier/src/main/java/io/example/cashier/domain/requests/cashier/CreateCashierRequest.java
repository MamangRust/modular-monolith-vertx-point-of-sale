package io.example.cashier.domain.requests.cashier;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateCashierRequest {
    @JsonProperty("merchant_id")
    private Integer merchantId;

    @JsonProperty("user_id")
    private Integer userId;

    @JsonProperty("name")
    private String name;
}