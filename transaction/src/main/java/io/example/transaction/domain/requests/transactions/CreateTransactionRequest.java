package io.example.transaction.domain.requests.transactions;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateTransactionRequest {
    @JsonProperty("order_id")
    private Integer orderID;

    @JsonProperty("merchant_id")
    private Integer merchantId;

    @JsonProperty("payment_method")
    private String paymentMethod;

    @JsonProperty("amount")
    private Integer amount;

    @JsonProperty("payment_status")
    private String paymentStatus;
}
