package io.example.merchant.domain.requests;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FindAllMerchants {
    private String search;
    private Integer page;
    private Integer pageSize;
}
