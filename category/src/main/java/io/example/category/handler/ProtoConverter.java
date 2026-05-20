package io.example.category.handler;

import io.example.category.model.*;
import pb.category.Category.*;

public class ProtoConverter {

    public static CategoryResponse toCategoryResponse(Category category) {
        if (category == null) return CategoryResponse.getDefaultInstance();
        return CategoryResponse.newBuilder()
                .setId(category.getCategoryId().intValue())
                .setName(category.getName() != null ? category.getName() : "")
                .setDescription(category.getDescription() != null ? category.getDescription() : "")
                .setSlugCategory(category.getSlugCategory() != null ? category.getSlugCategory() : "")
                .setImageCategory("")
                .setCreatedAt(category.getCreatedAt() != null ? category.getCreatedAt().toString() : "")
                .setUpdatedAt(category.getUpdatedAt() != null ? category.getUpdatedAt().toString() : "")
                .build();
    }

    public static CategoryResponseDeleteAt toCategoryResponseDeleteAt(Category category) {
        if (category == null) return CategoryResponseDeleteAt.getDefaultInstance();
        var builder = CategoryResponseDeleteAt.newBuilder()
                .setId(category.getCategoryId().intValue())
                .setName(category.getName() != null ? category.getName() : "")
                .setDescription(category.getDescription() != null ? category.getDescription() : "")
                .setSlugCategory(category.getSlugCategory() != null ? category.getSlugCategory() : "")
                .setImageCategory("")
                .setCreatedAt(category.getCreatedAt() != null ? category.getCreatedAt().toString() : "")
                .setUpdatedAt(category.getUpdatedAt() != null ? category.getUpdatedAt().toString() : "");
        if (category.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.newBuilder().setValue(category.getDeletedAt().toString()).build());
        }
        return builder.build();
    }

    public static CategoryMonthPriceResponse toCategoryMonthPriceResponse(CategoryMonthPrice price) {
        if (price == null) return CategoryMonthPriceResponse.getDefaultInstance();
        return CategoryMonthPriceResponse.newBuilder()
                .setMonth(price.getMonth() != null ? price.getMonth() : "")
                .setCategoryId(price.getCategoryId() != null ? price.getCategoryId() : 0)
                .setCategoryName(price.getCategoryName() != null ? price.getCategoryName() : "")
                .setOrderCount(price.getOrderCount() != null ? price.getOrderCount() : 0)
                .setItemsSold(price.getItemsSold() != null ? price.getItemsSold() : 0)
                .setTotalRevenue(price.getTotalRevenue() != null ? price.getTotalRevenue().intValue() : 0)
                .build();
    }

    public static CategoryYearPriceResponse toCategoryYearPriceResponse(CategoryYearPrice price) {
        if (price == null) return CategoryYearPriceResponse.getDefaultInstance();
        return CategoryYearPriceResponse.newBuilder()
                .setYear(price.getYear() != null ? price.getYear() : "")
                .setCategoryId(price.getCategoryId() != null ? price.getCategoryId() : 0)
                .setCategoryName(price.getCategoryName() != null ? price.getCategoryName() : "")
                .setOrderCount(price.getOrderCount() != null ? price.getOrderCount() : 0)
                .setItemsSold(price.getItemsSold() != null ? price.getItemsSold() : 0)
                .setTotalRevenue(price.getTotalRevenue() != null ? price.getTotalRevenue().intValue() : 0)
                .setUniqueProductsSold(price.getUniqueProductsSold() != null ? price.getUniqueProductsSold() : 0)
                .build();
    }

    public static CategoriesMonthlyTotalPriceResponse toCategoriesMonthlyTotalPriceResponse(CategoryMonthTotalPrice price) {
        if (price == null) return CategoriesMonthlyTotalPriceResponse.getDefaultInstance();
        return CategoriesMonthlyTotalPriceResponse.newBuilder()
                .setYear(price.getYear() != null ? price.getYear() : "")
                .setMonth(price.getMonth() != null ? price.getMonth() : "")
                .setTotalRevenue(price.getTotalRevenue() != null ? price.getTotalRevenue().intValue() : 0)
                .build();
    }

    public static CategoriesYearlyTotalPriceResponse toCategoriesYearlyTotalPriceResponse(CategoryYearTotalPrice price) {
        if (price == null) return CategoriesYearlyTotalPriceResponse.getDefaultInstance();
        return CategoriesYearlyTotalPriceResponse.newBuilder()
                .setYear(price.getYear() != null ? price.getYear() : "")
                .setTotalRevenue(price.getTotalRevenue() != null ? price.getTotalRevenue().intValue() : 0)
                .build();
    }
}
