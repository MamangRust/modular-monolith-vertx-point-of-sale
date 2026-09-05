package io.example.cashier.handler;

import io.example.cashier.domain.response.cashier.CashierResponse;
import io.example.cashier.domain.response.cashier.CashierResponseDeleteAt;
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.example.cashier.model.Cashier;

public class ProtoConverter {

    public static pb.cashier.Cashier.CashierResponse toCashierResponse(CashierResponse model) {
        if (model == null)
            return pb.cashier.Cashier.CashierResponse.getDefaultInstance();

        return pb.cashier.Cashier.CashierResponse.newBuilder()
                .setId(model.getId() != null ? model.getId() : 0)
                .setMerchantId(model.getMerchantId() != null ? model.getMerchantId() : 0)
                .setName(model.getName() != null ? model.getName() : "")
                .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
                .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "")
                .build();
    }

    public static pb.cashier.Cashier.CashierResponse toCashierResponse(Cashier model) {
        if (model == null)
            return pb.cashier.Cashier.CashierResponse.getDefaultInstance();

        return pb.cashier.Cashier.CashierResponse.newBuilder()
                .setId(model.getCashierId() != null ? model.getCashierId().intValue() : 0)
                .setMerchantId(model.getMerchantId() != null ? model.getMerchantId().intValue() : 0)
                .setName(model.getName() != null ? model.getName() : "")
                .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt().toString() : "")
                .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt().toString() : "")
                .build();
    }

    public static pb.cashier.Cashier.CashierResponseDeleteAt toCashierResponseDeleteAt(CashierResponseDeleteAt model) {
        if (model == null)
            return pb.cashier.Cashier.CashierResponseDeleteAt.getDefaultInstance();

        var builder = pb.cashier.Cashier.CashierResponseDeleteAt.newBuilder()
                .setId(model.getId() != null ? model.getId() : 0)
                .setMerchantId(model.getMerchantId() != null ? model.getMerchantId() : 0)
                .setName(model.getName() != null ? model.getName() : "")
                .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt() : "")
                .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt() : "");

        if (model.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(model.getDeletedAt()));
        }

        return builder.build();
    }

    public static pb.cashier.Cashier.CashierResponseDeleteAt toCashierResponseDeleteAt(Cashier model) {
        if (model == null)
            return pb.cashier.Cashier.CashierResponseDeleteAt.getDefaultInstance();

        var builder = pb.cashier.Cashier.CashierResponseDeleteAt.newBuilder()
                .setId(model.getCashierId() != null ? model.getCashierId().intValue() : 0)
                .setMerchantId(model.getMerchantId() != null ? model.getMerchantId().intValue() : 0)
                .setName(model.getName() != null ? model.getName() : "")
                .setCreatedAt(model.getCreatedAt() != null ? model.getCreatedAt().toString() : "")
                .setUpdatedAt(model.getUpdatedAt() != null ? model.getUpdatedAt().toString() : "");

        if (model.getDeletedAt() != null) {
            builder.setDeletedAt(com.google.protobuf.StringValue.of(model.getDeletedAt().toString()));
        }

        return builder.build();
    }

    public static pb.cashier.Cashier.CashierResponseMonthSales toMonthSalesResponse(CashierResponseMonthSales src) {
        if (src == null)
            return pb.cashier.Cashier.CashierResponseMonthSales.getDefaultInstance();

        return pb.cashier.Cashier.CashierResponseMonthSales.newBuilder()
                .setMonth(src.getMonth() != null ? src.getMonth() : "")
                .setCashierId(src.getCashierId() != null ? src.getCashierId() : 0)
                .setCashierName(src.getCashierName() != null ? src.getCashierName() : "")
                .setOrderCount(src.getOrderCount() != null ? src.getOrderCount() : 0)
                .setTotalSales(src.getTotalSales() != null ? src.getTotalSales().intValue() : 0)
                .build();
    }

    public static pb.cashier.Cashier.CashierResponseYearSales toYearSalesResponse(CashierResponseYearSales src) {
        if (src == null)
            return pb.cashier.Cashier.CashierResponseYearSales.getDefaultInstance();

        return pb.cashier.Cashier.CashierResponseYearSales.newBuilder()
                .setYear(src.getYear() != null ? src.getYear() : "")
                .setCashierId(src.getCashierId() != null ? src.getCashierId() : 0)
                .setCashierName(src.getCashierName() != null ? src.getCashierName() : "")
                .setOrderCount(src.getOrderCount() != null ? src.getOrderCount() : 0)
                .setTotalSales(src.getTotalSales() != null ? src.getTotalSales().intValue() : 0)
                .build();
    }

    public static pb.cashier.Cashier.CashierResponseMonthTotalSales toMonthTotalSalesResponse(
            CashierResponseMonthTotalSales src) {
        if (src == null)
            return pb.cashier.Cashier.CashierResponseMonthTotalSales.getDefaultInstance();

        return pb.cashier.Cashier.CashierResponseMonthTotalSales.newBuilder()
                .setYear(src.getYear() != null ? src.getYear() : "")
                .setMonth(src.getMonth() != null ? src.getMonth() : "")
                .setTotalSales(src.getTotalSales() != null ? src.getTotalSales().intValue() : 0)
                .build();
    }

    public static pb.cashier.Cashier.CashierResponseYearTotalSales toYearTotalSalesResponse(
            CashierResponseYearTotalSales src) {
        if (src == null)
            return pb.cashier.Cashier.CashierResponseYearTotalSales.getDefaultInstance();

        return pb.cashier.Cashier.CashierResponseYearTotalSales.newBuilder()
                .setYear(src.getYear() != null ? src.getYear() : "")
                .setTotalSales(src.getTotalSales() != null ? src.getTotalSales().intValue() : 0)
                .build();
    }
}