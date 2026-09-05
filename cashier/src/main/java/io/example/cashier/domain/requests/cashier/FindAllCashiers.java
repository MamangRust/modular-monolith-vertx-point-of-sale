package io.example.cashier.domain.requests.cashier;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FindAllCashiers {
    private String search;

    private Integer page;

    private Integer pageSize;
}
