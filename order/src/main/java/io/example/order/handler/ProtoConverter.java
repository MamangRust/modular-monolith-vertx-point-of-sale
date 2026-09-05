package io.example.order.handler;

import com.google.protobuf.StringValue;

import io.example.common.domain.PagedResult;
import pb.common.PaginationMeta;

public class ProtoConverter {
    public static pb.order.Order.OrderResponse toResponse(io.example.order.domain.response.order.OrderResponse order) {
        if (order == null) {
            return pb.order.Order.OrderResponse.getDefaultInstance();
        }
        return pb.order.Order.OrderResponse.newBuilder()
                .setId(order.getId().intValue())
                .setMerchantId(order.getMerchantId() != null ? order.getMerchantId() : 0)
                .setCashierId(order.getCashierId() != null ? order.getCashierId() : 0)
                .setTotalPrice(order.getTotalPrice() != null ? order.getTotalPrice() : 0)
                .setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt() : "")
                .setUpdatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt() : "")
                .build();
    }

    public static pb.order.Order.OrderResponseDeleteAt toResponseDeleteAt(
            io.example.order.domain.response.order.OrderResponseDeleteAt order) {
        if (order == null) {
            return pb.order.Order.OrderResponseDeleteAt.getDefaultInstance();
        }
        pb.order.Order.OrderResponseDeleteAt.Builder builder = pb.order.Order.OrderResponseDeleteAt.newBuilder()
                .setId(order.getId().intValue())
                .setMerchantId(order.getMerchantId() != null ? order.getMerchantId() : 0)
                .setCashierId(order.getCashierId() != null ? order.getCashierId() : 0)
                .setTotalPrice(order.getTotalPrice() != null ? order.getTotalPrice() : 0)
                .setCreatedAt(order.getCreatedAt() != null ? order.getCreatedAt() : "")
                .setUpdatedAt(order.getUpdatedAt() != null ? order.getUpdatedAt() : "");

        if (order.getDeletedAt() != null) {
            builder.setDeletedAt(StringValue.of(order.getDeletedAt()));
        }
        return builder.build();
    }

    public static pb.order.Order.OrderMonthlyTotalRevenueResponse toMonthlyTotalRevenueResponse(
            io.example.order.domain.response.order.OrderMonthlyTotalRevenueResponse r) {
        if (r == null) {
            return pb.order.Order.OrderMonthlyTotalRevenueResponse.getDefaultInstance();
        }
        return pb.order.Order.OrderMonthlyTotalRevenueResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setTotalRevenue(r.getTotalRevenue() != null ? r.getTotalRevenue() : 0)
                .build();
    }

    public static pb.order.Order.OrderYearlyTotalRevenueResponse toYearlyTotalRevenueResponse(
            io.example.order.domain.response.order.OrderYearlyTotalRevenueResponse r) {
        if (r == null) {
            return pb.order.Order.OrderYearlyTotalRevenueResponse.getDefaultInstance();
        }
        return pb.order.Order.OrderYearlyTotalRevenueResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setTotalRevenue(r.getTotalRevenue() != null ? r.getTotalRevenue() : 0)
                .build();
    }

    public static pb.order.Order.OrderMonthlyResponse toMonthlyResponse(
            io.example.order.domain.response.order.OrderMonthlyResponse r) {
        if (r == null) {
            return pb.order.Order.OrderMonthlyResponse.getDefaultInstance();
        }
        return pb.order.Order.OrderMonthlyResponse.newBuilder()
                .setMonth(r.getMonth() != null ? r.getMonth() : "")
                .setOrderCount(r.getOrderCount() != null ? r.getOrderCount() : 0)
                .setTotalRevenue(r.getTotalRevenue() != null ? r.getTotalRevenue() : 0)
                .setTotalItemsSold(r.getTotalItemsSold() != null ? r.getTotalItemsSold() : 0)
                .build();
    }

    public static pb.order.Order.OrderYearlyResponse toYearlyResponse(
            io.example.order.domain.response.order.OrderYearlyResponse r) {
        if (r == null) {
            return pb.order.Order.OrderYearlyResponse.getDefaultInstance();
        }
        return pb.order.Order.OrderYearlyResponse.newBuilder()
                .setYear(r.getYear() != null ? r.getYear() : "")
                .setOrderCount(r.getOrderCount() != null ? r.getOrderCount() : 0)
                .setTotalRevenue(r.getTotalRevenue().intValue())
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