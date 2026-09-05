package io.example.product.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import io.example.product.domain.response.ProductResponse;
import io.example.product.domain.response.ProductResponseDeleteAt;
import io.example.product.model.Product;

class ProtoConverterTest {

    @Test
    void fromProductResponse_shouldMapAllFields() {
        ProductResponse dto = ProductResponse.builder()
                .id(1L)
                .merchantId(1)
                .categoryId(1)
                .name("Test Product")
                .description("Desc")
                .price(50000)
                .countInStock(10)
                .brand("Brand")
                .weight(500)
                .slugProduct("test-product")
                .imageProduct("test.jpg")
                .createdAt("2024-01-01")
                .updatedAt("2024-06-01")
                .build();

        pb.product.Product.ProductResponse response = ProtoConverter.fromProductResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getPrice()).isEqualTo(50000);
        assertThat(response.getCountInStock()).isEqualTo(10);
        assertThat(response.getBrand()).isEqualTo("Brand");
        assertThat(response.getWeight()).isEqualTo(500);
        assertThat(response.getSlugProduct()).isEqualTo("test-product");
        assertThat(response.getImageProduct()).isEqualTo("test.jpg");
        assertThat(response.getCreatedAt()).isEqualTo("2024-01-01");
        assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01");
    }

    @Test
    void fromProductResponse_shouldHandleNull() {
        pb.product.Product.ProductResponse response = ProtoConverter.fromProductResponse(null);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(0);
    }

    @Test
    void fromProductResponseDeleteAt_shouldIncludeDeletedAt() {
        ProductResponseDeleteAt dto = ProductResponseDeleteAt.builder()
                .id(5L)
                .merchantId(1)
                .categoryId(1)
                .name("Deleted Product")
                .description("Desc")
                .price(50000)
                .countInStock(0)
                .brand("Brand")
                .weight(500)
                .slugProduct("deleted-product")
                .imageProduct("deleted.jpg")
                .createdAt("2024-01-01")
                .updatedAt("2024-02-01")
                .deletedAt("2024-03-01")
                .build();

        pb.product.Product.ProductResponseDeleteAt response = ProtoConverter.fromProductResponseDeleteAt(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5);
        assertThat(response.getName()).isEqualTo("Deleted Product");
        assertThat(response.getPrice()).isEqualTo(50000);
        assertThat(response.hasDeletedAt()).isTrue();
        assertThat(response.getDeletedAt().getValue()).isEqualTo("2024-03-01");
    }

    @Test
    void fromProductResponseDeleteAt_shouldHandleNullDeletedAt() {
        ProductResponseDeleteAt dto = ProductResponseDeleteAt.builder()
                .id(5L)
                .name("Product No Delete")
                .build();

        pb.product.Product.ProductResponseDeleteAt response = ProtoConverter.fromProductResponseDeleteAt(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5);
        assertThat(response.hasDeletedAt()).isFalse();
    }

    @Test
    void fromProductResponseDeleteAt_shouldHandleNull() {
        pb.product.Product.ProductResponseDeleteAt response = ProtoConverter.fromProductResponseDeleteAt(null);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(0);
    }

    @Test
    void fromProductResponseToDeleteAt_shouldMapFromProductResponse() {
        ProductResponse dto = ProductResponse.builder()
                .id(1L)
                .merchantId(1)
                .categoryId(1)
                .name("Converted")
                .price(75000)
                .countInStock(5)
                .createdAt("2024-01-01")
                .updatedAt("2024-06-01")
                .build();

        pb.product.Product.ProductResponseDeleteAt response = ProtoConverter.fromProductResponseToDeleteAt(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getName()).isEqualTo("Converted");
        assertThat(response.getPrice()).isEqualTo(75000);
        assertThat(response.hasDeletedAt()).isFalse();
    }

    @Test
    void fromProductResponseToDeleteAt_shouldHandleNull() {
        pb.product.Product.ProductResponseDeleteAt response = ProtoConverter.fromProductResponseToDeleteAt(null);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(0);
    }
}
