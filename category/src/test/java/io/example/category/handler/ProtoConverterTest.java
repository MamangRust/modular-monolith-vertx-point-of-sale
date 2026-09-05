package io.example.category.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoryResponse;
import io.example.category.domain.response.category.CategoryResponseDeleteAt;
import io.example.category.model.Category;

class ProtoConverterTest {

    @Test
    void toCategoryResponseFromDto_shouldMapAllFields() {
        CategoryResponse dto = CategoryResponse.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic gadgets")
                .slugCategory("electronics")
                .createdAt("2024-01-01")
                .updatedAt("2024-06-01")
                .build();

        pb.category.Category.CategoryResponse response = ProtoConverter.toCategoryResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getName()).isEqualTo("Electronics");
        assertThat(response.getDescription()).isEqualTo("Electronic gadgets");
        assertThat(response.getSlugCategory()).isEqualTo("electronics");
        assertThat(response.getCreatedAt()).isEqualTo("2024-01-01");
        assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01");
    }

    @Test
    void toCategoryResponseFromDto_shouldHandleNullAndEmpty() {
        pb.category.Category.CategoryResponse response = ProtoConverter.toCategoryResponse((CategoryResponse) null);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(0);
        assertThat(response.getName()).isEmpty();
    }

    @Test
    void toCategoryResponseFromModel_shouldMapAllFields() {
        Category model = Category.builder()
                .categoryId(10L)
                .name("Books")
                .description("Reading materials")
                .slugCategory("books")
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 10, 0)))
                .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 12, 0)))
                .build();

        pb.category.Category.CategoryResponse response = ProtoConverter.toCategoryResponse(model);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getName()).isEqualTo("Books");
        assertThat(response.getDescription()).isEqualTo("Reading materials");
        assertThat(response.getSlugCategory()).isEqualTo("books");
        assertThat(response.getCreatedAt()).isEqualTo("2024-01-01 10:00:00.0");
        assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01 12:00:00.0");
    }

    @Test
    void toCategoryResponseFromModel_shouldHandleNullAndEmpty() {
        pb.category.Category.CategoryResponse response = ProtoConverter.toCategoryResponse((Category) null);
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(0);
    }

    @Test
    void toCategoryResponseDeleteAtFromDto_shouldIncludeDeletedAt() {
        CategoryResponseDeleteAt dto = CategoryResponseDeleteAt.builder()
                .id(5L)
                .name("Clothes")
                .description("Wears")
                .slugCategory("clothes")
                .createdAt("2024-01-01")
                .updatedAt("2024-02-01")
                .deletedAt("2024-03-01")
                .build();

        pb.category.Category.CategoryResponseDeleteAt response = ProtoConverter.toCategoryResponseDeleteAt(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5);
        assertThat(response.getName()).isEqualTo("Clothes");
        assertThat(response.hasDeletedAt()).isTrue();
        assertThat(response.getDeletedAt().getValue()).isEqualTo("2024-03-01");
    }

    @Test
    void toCategoryResponseDeleteAtFromModel_shouldIncludeDeletedAt() {
        Category model = Category.builder()
                .categoryId(10L)
                .name("Home")
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 2, 1, 0, 0)))
                .deletedAt(Timestamp.valueOf(LocalDateTime.of(2024, 3, 1, 0, 0)))
                .build();

        pb.category.Category.CategoryResponseDeleteAt response = ProtoConverter.toCategoryResponseDeleteAt(model);

        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getDeletedAt().getValue()).isEqualTo("2024-03-01 00:00:00.0");
    }

    @Test
    void toCategoryMonthPriceResponse_shouldMapAllFields() {
        CategoriesMonthPriceResponse dto = CategoriesMonthPriceResponse.builder()
                .month("January")
                .categoryId(1)
                .categoryName("Books")
                .orderCount(100)
                .itemsSold(250)
                .totalRevenue(500000L)
                .build();

        pb.category.Category.CategoryMonthPriceResponse response = ProtoConverter.toCategoryMonthPriceResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getMonth()).isEqualTo("January");
        assertThat(response.getCategoryId()).isEqualTo(1);
        assertThat(response.getCategoryName()).isEqualTo("Books");
        assertThat(response.getOrderCount()).isEqualTo(100);
        assertThat(response.getItemsSold()).isEqualTo(250);
        assertThat(response.getTotalRevenue()).isEqualTo(500000);
    }

    @Test
    void toCategoryYearPriceResponse_shouldMapAllFields() {
        CategoriesYearPriceResponse dto = CategoriesYearPriceResponse.builder()
                .year("2024")
                .categoryId(2)
                .categoryName("Food")
                .orderCount(1200)
                .itemsSold(3500)
                .totalRevenue(6000000L)
                .uniqueProductsSold(50)
                .build();

        pb.category.Category.CategoryYearPriceResponse response = ProtoConverter.toCategoryYearPriceResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getYear()).isEqualTo("2024");
        assertThat(response.getCategoryId()).isEqualTo(2);
        assertThat(response.getCategoryName()).isEqualTo("Food");
        assertThat(response.getOrderCount()).isEqualTo(1200);
        assertThat(response.getItemsSold()).isEqualTo(3500);
        assertThat(response.getTotalRevenue()).isEqualTo(6000000);
        assertThat(response.getUniqueProductsSold()).isEqualTo(50);
    }

    @Test
    void toCategoriesMonthlyTotalPriceResponse_shouldMapAllFields() {
        CategoriesMonthlyTotalPriceResponse dto = CategoriesMonthlyTotalPriceResponse.builder()
                .year("2024")
                .month("January")
                .totalRevenue(150000L)
                .build();

        pb.category.Category.CategoriesMonthlyTotalPriceResponse response = ProtoConverter.toCategoriesMonthlyTotalPriceResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getYear()).isEqualTo("2024");
        assertThat(response.getMonth()).isEqualTo("January");
        assertThat(response.getTotalRevenue()).isEqualTo(150000);
    }

    @Test
    void toCategoriesYearlyTotalPriceResponse_shouldMapAllFields() {
        CategoriesYearlyTotalPriceResponse dto = CategoriesYearlyTotalPriceResponse.builder()
                .year("2024")
                .totalRevenue(2500000L)
                .build();

        pb.category.Category.CategoriesYearlyTotalPriceResponse response = ProtoConverter.toCategoriesYearlyTotalPriceResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getYear()).isEqualTo("2024");
        assertThat(response.getTotalRevenue()).isEqualTo(2500000);
    }

    @Test
    void nullSafety_forAllConverterMethods() {
        assertThat(ProtoConverter.toCategoryResponse((CategoryResponse) null)).isNotNull();
        assertThat(ProtoConverter.toCategoryResponse((Category) null)).isNotNull();
        assertThat(ProtoConverter.toCategoryResponseDeleteAt((CategoryResponseDeleteAt) null)).isNotNull();
        assertThat(ProtoConverter.toCategoryResponseDeleteAt((Category) null)).isNotNull();
        assertThat(ProtoConverter.toCategoryMonthPriceResponse(null)).isNotNull();
        assertThat(ProtoConverter.toCategoryYearPriceResponse(null)).isNotNull();
        assertThat(ProtoConverter.toCategoriesMonthlyTotalPriceResponse(null)).isNotNull();
        assertThat(ProtoConverter.toCategoriesYearlyTotalPriceResponse(null)).isNotNull();
    }
}
