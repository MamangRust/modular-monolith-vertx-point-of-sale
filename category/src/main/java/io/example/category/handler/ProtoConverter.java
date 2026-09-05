package io.example.category.handler;

import io.example.category.domain.response.category.CategoriesMonthPriceResponse;
import io.example.category.domain.response.category.CategoriesMonthlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoriesYearPriceResponse;
import io.example.category.domain.response.category.CategoriesYearlyTotalPriceResponse;
import io.example.category.domain.response.category.CategoryResponse;
import io.example.category.domain.response.category.CategoryResponseDeleteAt;
import io.example.category.model.Category;

public class ProtoConverter {

    public static pb.category.Category.CategoryResponse toCategoryResponse(CategoryResponse model) {
        if (model == null)
            return pb.category.Category.CategoryResponse.getDefaultInstance();

        return pb.category.Category.CategoryResponse.newBuilder()
                .setId(model.getId().intValue())
                .setName(model.getName() != null ? model.getName() : "")
                .setDescription(model.getDescription() != null ? model.getDescription() : "")
                .setSlugCategory(model.getSlugCategory() != null ? model.getSlugCategory() : "")
                .setImageCategory("")
                .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
                .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "")
                .build();
    }

    public static pb.category.Category.CategoryResponse toCategoryResponse(Category category) {
        if (category == null)
            return pb.category.Category.CategoryResponse.getDefaultInstance();

        return pb.category.Category.CategoryResponse.newBuilder()
                .setId(category.getCategoryId() != null ? category.getCategoryId().intValue() : 0)
                .setName(category.getName() != null ? category.getName() : "")
                .setDescription(category.getDescription() != null ? category.getDescription() : "")
                .setSlugCategory(category.getSlugCategory() != null ? category.getSlugCategory() : "")
                .setImageCategory("")
                .setCreatedAt(category.getCreatedAt() != null ? category.getCreatedAt().toString() : "")
                .setUpdatedAt(category.getUpdatedAt() != null ? category.getUpdatedAt().toString() : "")
                .build();
    }

    public static pb.category.Category.CategoryResponseDeleteAt toCategoryResponseDeleteAt(
            CategoryResponseDeleteAt model) {
        if (model == null)
            return pb.category.Category.CategoryResponseDeleteAt.getDefaultInstance();

        var builder = pb.category.Category.CategoryResponseDeleteAt.newBuilder()
                .setId(model.getId().intValue())
                .setName(model.getName() != null ? model.getName() : "")
                .setDescription(model.getDescription() != null ? model.getDescription() : "")
                .setSlugCategory(model.getSlugCategory() != null ? model.getSlugCategory() : "")
                .setImageCategory("")
                .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
                .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "");

        if (model.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(model.getDeletedAt()));
        }

        return builder.build();
    }

    public static pb.category.Category.CategoryResponseDeleteAt toCategoryResponseDeleteAt(Category category) {
        if (category == null)
            return pb.category.Category.CategoryResponseDeleteAt.getDefaultInstance();

        var builder = pb.category.Category.CategoryResponseDeleteAt.newBuilder()
                .setId(category.getCategoryId().intValue())
                .setName(category.getName() != null ? category.getName() : "")
                .setDescription(category.getDescription() != null ? category.getDescription() : "")
                .setSlugCategory(category.getSlugCategory() != null ? category.getSlugCategory() : "")
                .setImageCategory("")
                .setCreatedAt(category.getCreatedAt() != null ? category.getCreatedAt().toString() : "")
                .setUpdatedAt(category.getUpdatedAt() != null ? category.getUpdatedAt().toString() : "");

        if (category.getDeletedAt() != null) {
            builder.setDeletedAt(
                    com.google.protobuf.StringValue.newBuilder().setValue(category.getDeletedAt().toString()).build());
        }

        return builder.build();
    }

    public static pb.category.Category.CategoryMonthPriceResponse toCategoryMonthPriceResponse(
            CategoriesMonthPriceResponse src) {
        if (src == null)
            return pb.category.Category.CategoryMonthPriceResponse.getDefaultInstance();

        return pb.category.Category.CategoryMonthPriceResponse.newBuilder()
                .setMonth(src.getMonth() != null ? src.getMonth() : "")
                .setCategoryId(src.getCategoryId() != null ? src.getCategoryId() : 0)
                .setCategoryName(src.getCategoryName() != null ? src.getCategoryName() : "")
                .setOrderCount(src.getOrderCount() != null ? src.getOrderCount() : 0)
                .setItemsSold(src.getItemsSold() != null ? src.getItemsSold() : 0)
                .setTotalRevenue(src.getTotalRevenue() != null ? src.getTotalRevenue().intValue() : 0)
                .build();
    }

    public static pb.category.Category.CategoryYearPriceResponse toCategoryYearPriceResponse(
            CategoriesYearPriceResponse src) {
        if (src == null)
            return pb.category.Category.CategoryYearPriceResponse.getDefaultInstance();

        return pb.category.Category.CategoryYearPriceResponse.newBuilder()
                .setYear(src.getYear() != null ? src.getYear() : "")
                .setCategoryId(src.getCategoryId() != null ? src.getCategoryId() : 0)
                .setCategoryName(src.getCategoryName() != null ? src.getCategoryName() : "")
                .setOrderCount(src.getOrderCount() != null ? src.getOrderCount() : 0)
                .setItemsSold(src.getItemsSold() != null ? src.getItemsSold() : 0)
                .setTotalRevenue(src.getTotalRevenue() != null ? src.getTotalRevenue().intValue() : 0)
                .setUniqueProductsSold(src.getUniqueProductsSold() != null ? src.getUniqueProductsSold() : 0)
                .build();
    }

    public static pb.category.Category.CategoriesMonthlyTotalPriceResponse toCategoriesMonthlyTotalPriceResponse(
            CategoriesMonthlyTotalPriceResponse src) {
        if (src == null)
            return pb.category.Category.CategoriesMonthlyTotalPriceResponse.getDefaultInstance();

        return pb.category.Category.CategoriesMonthlyTotalPriceResponse.newBuilder()
                .setYear(src.getYear() != null ? src.getYear() : "")
                .setMonth(src.getMonth() != null ? src.getMonth() : "")
                .setTotalRevenue(src.getTotalRevenue() != null ? src.getTotalRevenue().intValue() : 0)
                .build();
    }

    public static pb.category.Category.CategoriesYearlyTotalPriceResponse toCategoriesYearlyTotalPriceResponse(
            CategoriesYearlyTotalPriceResponse src) {
        if (src == null)
            return pb.category.Category.CategoriesYearlyTotalPriceResponse.getDefaultInstance();

        return pb.category.Category.CategoriesYearlyTotalPriceResponse.newBuilder()
                .setYear(src.getYear() != null ? src.getYear() : "")
                .setTotalRevenue(src.getTotalRevenue() != null ? src.getTotalRevenue().intValue() : 0)
                .build();
    }
}