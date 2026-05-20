package io.example.order.handler;

import com.google.protobuf.StringValue;
import io.example.common.domain.PagedResult;
import io.example.order.model.*;
import pb.common.PaginationMeta;
import pb.order.Order.*;

public class ProtoConverter {

    public static OrderResponse toResponse(Order order) {
        if (order == null) {
            return OrderResponse.getDefaultInstance();
        }
        return OrderResponse.newBuilder()
                .setId(order.getOrderId() != null ? order.getOrderId().intValue() : 0)
                .setMerchantId(order.getMerchantId() != null ? order.getMerchantId().intValue() : 0)
                .setCashierId(order.getCashierId() != null ? order.getCashierId().intValue() : 0)
                .setTotalPrice(order.getTotalPrice() != null ? order.getTotalPrice().intValue() : 0)
                .setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : "")
                .setUpdatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : "")
                .build();
    }

    public static OrderResponseDeleteAt toResponseDeleteAt(Order order) {
        if (order == null) {
            return OrderResponseDeleteAt.getDefaultInstance();
        }
        OrderResponseDeleteAt.Builder builder = OrderResponseDeleteAt.newBuilder()
                .setId(order.getOrderId() != null ? order.getOrderId().intValue() : 0)
                .setMerchantId(order.getMerchantId() != null ? order.getMerchantId().intValue() : 0)
                .setCashierId(order.getCashierId() != null ? order.getCashierId().intValue() : 0)
                .setTotalPrice(order.getTotalPrice() != null ? order.getTotalPrice().intValue() : 0)
                .setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : "")
                .setUpdatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt().toString() : "");

        if (order.getDeletedAt() != null) {
            builder.setDeletedAt(StringValue.of(order.getDeletedAt().toString()));
        }
        return builder.build();
    }

    public static OrderMonthlyTotalRevenueResponse toMonthlyTotalRevenueResponse(OrderMonthTotalRevenue r) {
        if (r == null) {
            return OrderMonthlyTotalRevenueResponse.getDefaultInstance();
        }
        return OrderMonthlyTotalRevenueResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setTotalRevenue(r.getTotalRevenue() != null ? r.getTotalRevenue() : 0)
                .build();
    }

    public static OrderYearlyTotalRevenueResponse toYearlyTotalRevenueResponse(OrderYearTotalRevenue r) {
        if (r == null) {
            return OrderYearlyTotalRevenueResponse.getDefaultInstance();
        }
        return OrderYearlyTotalRevenueResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setTotalRevenue(r.getTotalRevenue() != null ? r.getTotalRevenue() : 0)
                .build();
    }

    public static OrderMonthlyResponse toMonthlyResponse(OrderMonth r) {
        if (r == null) {
            return OrderMonthlyResponse.getDefaultInstance();
        }
        return OrderMonthlyResponse.newBuilder()
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setOrderCount(r.getOrderCount() != null ? r.getOrderCount() : 0)
                .setTotalRevenue(r.getTotalRevenue() != null ? r.getTotalRevenue().intValue() : 0)
                .setTotalItemsSold(r.getTotalItemsSold() != null ? r.getTotalItemsSold() : 0)
                .build();
    }

    public static OrderYearlyResponse toYearlyResponse(OrderYear r) {
        if (r == null) {
            return OrderYearlyResponse.getDefaultInstance();
        }
        return OrderYearlyResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setOrderCount(r.getOrderCount() != null ? r.getOrderCount() : 0)
                .setTotalRevenue(r.getTotalRevenue() != null ? r.getTotalRevenue().intValue() : 0)
                .setTotalItemsSold(r.getTotalItemsSold() != null ? r.getTotalItemsSold() : 0)
                .setActiveCashiers(r.getActiveCashiers() != null ? r.getActiveCashiers() : 0)
                .setUniqueProductsSold(r.getUniqueProductsSold() != null ? r.getUniqueProductsSold() : 0)
                .build();
    }

    public static PaginationMeta toPaginationMeta(PagedResult<?> result, int page, int pageSize) {
        int totalRecords = result.getTotalRecords();
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        return PaginationMeta.newBuilder()
                .setCurrentPage(page)
                .setPageSize(pageSize)
                .setTotalPages(totalPages)
                .setTotalRecords(totalRecords)
                .build();
    }
}
