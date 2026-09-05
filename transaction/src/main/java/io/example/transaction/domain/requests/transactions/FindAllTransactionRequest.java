package io.example.transaction.domain.requests.transactions;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FindAllTransactionRequest {
    private String search;

    @Builder.Default
    private Integer page = 1;

    @Builder.Default
    private Integer pageSize = 10;
}