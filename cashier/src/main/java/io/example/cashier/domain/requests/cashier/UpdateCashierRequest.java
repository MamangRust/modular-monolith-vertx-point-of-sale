package io.example.cashier.domain.requests.cashier;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateCashierRequest {
    @JsonProperty("cashier_id")
    private Integer cashierId;

    @JsonProperty("name")
    private String name;
}
