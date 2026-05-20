package io.example.product.handler;

import com.google.protobuf.StringValue;
import io.example.product.model.ProductResponse;
import io.example.product.model.ProductResponseDeleteAt;

public class ProtoConverter {

    public static pb.product.Product.ProductResponse fromProductResponse(ProductResponse r) {
        if (r == null) return pb.product.Product.ProductResponse.getDefaultInstance();
        return pb.product.Product.ProductResponse.newBuilder()
                .setId(r.getId() != null ? r.getId().intValue() : 0)
                .setMerchantId(r.getMerchantId() != null ? r.getMerchantId() : 0)
                .setCategoryId(r.getCategoryId() != null ? r.getCategoryId() : 0)
                .setName(r.getName() != null ? r.getName() : "")
                .setDescription(r.getDescription() != null ? r.getDescription() : "")
                .setPrice(r.getPrice() != null ? r.getPrice() : 0)
                .setCountInStock(r.getCountInStock() != null ? r.getCountInStock() : 0)
                .setBrand(r.getBrand() != null ? r.getBrand() : "")
                .setWeight(r.getWeight() != null ? r.getWeight() : 0)
                .setSlugProduct(r.getSlugProduct() != null ? r.getSlugProduct() : "")
                .setImageProduct(r.getImageProduct() != null ? r.getImageProduct() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }

    public static pb.product.Product.ProductResponseDeleteAt fromProductResponseDeleteAt(ProductResponseDeleteAt r) {
        if (r == null) return pb.product.Product.ProductResponseDeleteAt.getDefaultInstance();
        pb.product.Product.ProductResponseDeleteAt.Builder b = pb.product.Product.ProductResponseDeleteAt.newBuilder()
                .setId(r.getId() != null ? r.getId().intValue() : 0)
                .setMerchantId(r.getMerchantId() != null ? r.getMerchantId() : 0)
                .setCategoryId(r.getCategoryId() != null ? r.getCategoryId() : 0)
                .setName(r.getName() != null ? r.getName() : "")
                .setDescription(r.getDescription() != null ? r.getDescription() : "")
                .setPrice(r.getPrice() != null ? r.getPrice() : 0)
                .setCountInStock(r.getCountInStock() != null ? r.getCountInStock() : 0)
                .setBrand(r.getBrand() != null ? r.getBrand() : "")
                .setWeight(r.getWeight() != null ? r.getWeight() : 0)
                .setSlugProduct(r.getSlugProduct() != null ? r.getSlugProduct() : "")
                .setImageProduct(r.getImageProduct() != null ? r.getImageProduct() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "");

        if (r.getDeletedAt() != null) {
            b.setDeletedAt(StringValue.of(r.getDeletedAt()));
        }
        return b.build();
    }

    public static pb.product.Product.ProductResponseDeleteAt fromProductResponseToDeleteAt(ProductResponse r) {
        if (r == null) return pb.product.Product.ProductResponseDeleteAt.getDefaultInstance();
        return pb.product.Product.ProductResponseDeleteAt.newBuilder()
                .setId(r.getId() != null ? r.getId().intValue() : 0)
                .setMerchantId(r.getMerchantId() != null ? r.getMerchantId() : 0)
                .setCategoryId(r.getCategoryId() != null ? r.getCategoryId() : 0)
                .setName(r.getName() != null ? r.getName() : "")
                .setDescription(r.getDescription() != null ? r.getDescription() : "")
                .setPrice(r.getPrice() != null ? r.getPrice() : 0)
                .setCountInStock(r.getCountInStock() != null ? r.getCountInStock() : 0)
                .setBrand(r.getBrand() != null ? r.getBrand() : "")
                .setWeight(r.getWeight() != null ? r.getWeight() : 0)
                .setSlugProduct(r.getSlugProduct() != null ? r.getSlugProduct() : "")
                .setImageProduct(r.getImageProduct() != null ? r.getImageProduct() : "")
                .setCreatedAt(r.getCreatedAt() != null ? r.getCreatedAt() : "")
                .setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : "")
                .build();
    }
}
