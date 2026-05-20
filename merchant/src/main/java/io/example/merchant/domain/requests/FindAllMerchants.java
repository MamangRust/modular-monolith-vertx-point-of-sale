package io.example.merchant.domain.requests;

import lombok.Data;

@Data
public class FindAllMerchants {
    private String search;
    private Integer page;
    private Integer pageSize;
}
