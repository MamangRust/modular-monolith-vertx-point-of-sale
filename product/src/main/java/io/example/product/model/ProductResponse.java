package io.example.product.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private Integer merchantId;
    private Integer categoryId;
    private String name;
    private String description;
    private Integer price;
    private Integer countInStock;
    private String brand;
    private Integer weight;
    private String slugProduct;
    private String imageProduct;
    private String createdAt;
    private String updatedAt;

    public static ProductResponse from(Product entity) {
        if (entity == null) return null;
        return ProductResponse.builder()
                .id(entity.getProductId())
                .merchantId(entity.getMerchantId() != null ? entity.getMerchantId().intValue() : null)
                .categoryId(entity.getCategoryId() != null ? entity.getCategoryId().intValue() : null)
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .countInStock(entity.getCountInStock())
                .brand(entity.getBrand())
                .weight(entity.getWeight())
                .slugProduct(entity.getSlugProduct())
                .imageProduct(entity.getImageProduct())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null)
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null)
                .build();
    }
}
