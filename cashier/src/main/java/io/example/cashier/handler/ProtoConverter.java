package io.example.cashier.handler;

import io.example.cashier.model.Cashier;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import pb.cashier.Cashier.CashierResponse;
import pb.cashier.Cashier.CashierResponseDeleteAt;
import pb.cashier.Cashier.CashierResponseMonthSales;
import pb.cashier.Cashier.CashierResponseMonthTotalSales;
import pb.cashier.Cashier.CashierResponseYearSales;
import pb.cashier.Cashier.CashierResponseYearTotalSales;

public class ProtoConverter {

    public static CashierResponse toCashierResponse(Cashier cashier) {
        if (cashier == null)
            return CashierResponse.getDefaultInstance();
        return CashierResponse.newBuilder()
                .setId(cashier.getCashierId().intValue())
                .setMerchantId(cashier.getMerchantId().intValue())
                .setName(cashier.getName())
                .setCreatedAt(cashier.getCreatedAt() != null ? cashier.getCreatedAt().toString() : "")
                .setUpdatedAt(cashier.getUpdatedAt() != null ? cashier.getUpdatedAt().toString() : "")
                .build();
    }

    public static CashierResponseDeleteAt toCashierResponseDeleteAt(Cashier cashier) {
        if (cashier == null)
            return CashierResponseDeleteAt.getDefaultInstance();
        var builder = CashierResponseDeleteAt.newBuilder()
                .setId(cashier.getCashierId().intValue())
                .setMerchantId(cashier.getMerchantId().intValue())
                .setName(cashier.getName())
                .setCreatedAt(cashier.getCreatedAt() != null ? cashier.getCreatedAt().toString() : "")
                .setUpdatedAt(cashier.getUpdatedAt() != null ? cashier.getUpdatedAt().toString() : "");
        if (cashier.getDeletedAt() != null) {
            builder.setDeletedAt(
                    com.google.protobuf.StringValue.newBuilder().setValue(cashier.getDeletedAt().toString()).build());
        }
        return builder.build();
    }

    public static CashierResponseMonthSales toMonthSalesResponse(CashierMonthSales s) {
        if (s == null)
            return CashierResponseMonthSales.getDefaultInstance();
        return CashierResponseMonthSales.newBuilder()
                .setMonth(s.getMonth() != null ? s.getMonth() : "")
                .setCashierId(s.getCashierId())
                .setCashierName(s.getCashierName() != null ? s.getCashierName() : "")
                .setOrderCount(s.getOrderCount() != null ? s.getOrderCount() : 0)
                .setTotalSales(s.getTotalSales() != null ? s.getTotalSales().intValue() : 0)
                .build();
    }

    public static CashierResponseYearSales toYearSalesResponse(CashierYearSales s) {
        if (s == null)
            return CashierResponseYearSales.getDefaultInstance();
        return CashierResponseYearSales.newBuilder()
                .setYear(s.getYear() != null ? s.getYear() : "")
                .setCashierId(s.getCashierId())
                .setCashierName(s.getCashierName() != null ? s.getCashierName() : "")
                .setOrderCount(s.getOrderCount() != null ? s.getOrderCount() : 0)
                .setTotalSales(s.getTotalSales() != null ? s.getTotalSales().intValue() : 0)
                .build();
    }

    public static CashierResponseMonthTotalSales toMonthTotalSalesResponse(CashierMonthTotalSales s) {
        if (s == null)
            return CashierResponseMonthTotalSales.getDefaultInstance();
        return CashierResponseMonthTotalSales.newBuilder()
                .setYear(s.getYear() != null ? s.getYear() : "")
                .setMonth(s.getMonth() != null ? s.getMonth() : "")
                .setTotalSales(s.getTotalSales() != null ? s.getTotalSales().intValue() : 0)
                .build();
    }

    public static CashierResponseYearTotalSales toYearTotalSalesResponse(CashierYearTotalSales s) {
        if (s == null)
            return CashierResponseYearTotalSales.getDefaultInstance();
        return CashierResponseYearTotalSales.newBuilder()
                .setYear(s.getYear() != null ? s.getYear() : "")
                .setTotalSales(s.getTotalSales() != null ? s.getTotalSales().intValue() : 0)
                .build();
    }
}
