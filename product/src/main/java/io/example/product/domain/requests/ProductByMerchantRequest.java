package io.example.product.domain.requests;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductByMerchantRequest {
    private Integer merchantId;
    private String search;
    private Integer categoryId;
    private Integer minPrice;
    private Integer maxPrice;
    private Integer page;
    private Integer pageSize;
}
