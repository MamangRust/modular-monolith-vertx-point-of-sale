package io.example.cashier.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import io.example.cashier.domain.response.cashier.CashierResponse;
import io.example.cashier.domain.response.cashier.CashierResponseDeleteAt;
import io.example.cashier.domain.response.cashier.CashierResponseMonthSales;
import io.example.cashier.domain.response.cashier.CashierResponseMonthTotalSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearSales;
import io.example.cashier.domain.response.cashier.CashierResponseYearTotalSales;
import io.example.cashier.model.Cashier;

class ProtoConverterTest {

    @Test
    void toCashierResponseFromDto_shouldMapAllFields() {
        CashierResponse dto = CashierResponse.builder()
                .id(1)
                .merchantId(2)
                .name("Cashier 1")
                .createdAt("2024-01-01")
                .updatedAt("2024-06-01")
                .build();

        pb.cashier.Cashier.CashierResponse response = ProtoConverter.toCashierResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getMerchantId()).isEqualTo(2);
        assertThat(response.getName()).isEqualTo("Cashier 1");
        assertThat(response.getCreatedAt()).isEqualTo("2024-01-01");
        assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01");
    }

    @Test
    void toCashierResponseFromDto_shouldHandleNullAndEmpty() {
        pb.cashier.Cashier.CashierResponse response = ProtoConverter.toCashierResponse((CashierResponse) null);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(0);
        assertThat(response.getMerchantId()).isEqualTo(0);
        assertThat(response.getName()).isEqualTo("");
        assertThat(response.getCreatedAt()).isEqualTo("");
        assertThat(response.getUpdatedAt()).isEqualTo("");
    }

    @Test
    void toCashierResponseFromModel_shouldMapAllFields() {
        Cashier model = Cashier.builder()
                .cashierId(10L)
                .merchantId(20L)
                .name("Cashier 2")
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 10, 0)))
                .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 12, 0)))
                .build();

        pb.cashier.Cashier.CashierResponse response = ProtoConverter.toCashierResponse(model);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getMerchantId()).isEqualTo(20);
        assertThat(response.getName()).isEqualTo("Cashier 2");
        assertThat(response.getCreatedAt()).isEqualTo("2024-01-01 10:00:00.0");
        assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01 12:00:00.0");
    }

    @Test
    void toCashierResponseFromModel_shouldHandleNullAndEmpty() {
        pb.cashier.Cashier.CashierResponse response = ProtoConverter.toCashierResponse((Cashier) null);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(0);
        assertThat(response.getMerchantId()).isEqualTo(0);
        assertThat(response.getName()).isEqualTo("");
        assertThat(response.getCreatedAt()).isEqualTo("");
        assertThat(response.getUpdatedAt()).isEqualTo("");
    }

    @Test
    void toCashierResponseDeleteAtFromDto_shouldIncludeDeletedAt() {
        CashierResponseDeleteAt dto = CashierResponseDeleteAt.builder()
                .id(5)
                .merchantId(6)
                .name("Cashier 5")
                .createdAt("2024-01-01")
                .updatedAt("2024-02-01")
                .deletedAt("2024-03-01")
                .build();

        pb.cashier.Cashier.CashierResponseDeleteAt response = ProtoConverter.toCashierResponseDeleteAt(dto);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(5);
        assertThat(response.getMerchantId()).isEqualTo(6);
        assertThat(response.getName()).isEqualTo("Cashier 5");
        assertThat(response.getCreatedAt()).isEqualTo("2024-01-01");
        assertThat(response.getUpdatedAt()).isEqualTo("2024-02-01");
        assertThat(response.hasDeletedAt()).isTrue();
        assertThat(response.getDeletedAt().getValue()).isEqualTo("2024-03-01");
    }

    @Test
    void toCashierResponseDeleteAtFromDto_shouldHandleNullDeletedAt() {
        CashierResponseDeleteAt dto = CashierResponseDeleteAt.builder()
                .id(5)
                .merchantId(6)
                .name("Cashier 5")
                .createdAt("2024-01-01")
                .updatedAt("2024-02-01")
                .deletedAt(null)
                .build();

        pb.cashier.Cashier.CashierResponseDeleteAt response = ProtoConverter.toCashierResponseDeleteAt(dto);

        assertThat(response.hasDeletedAt()).isFalse();
    }

    @Test
    void toCashierResponseDeleteAtFromModel_shouldIncludeDeletedAt() {
        Cashier model = Cashier.builder()
                .cashierId(10L)
                .merchantId(20L)
                .name("Cashier 10")
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 2, 1, 0, 0)))
                .deletedAt(Timestamp.valueOf(LocalDateTime.of(2024, 3, 1, 0, 0)))
                .build();

        pb.cashier.Cashier.CashierResponseDeleteAt response = ProtoConverter.toCashierResponseDeleteAt(model);

        assertThat(response.getId()).isEqualTo(10);
        assertThat(response.getDeletedAt().getValue()).isEqualTo("2024-03-01 00:00:00.0");
    }

    @Test
    void toMonthSalesResponse_shouldMapAllFields() {
        CashierResponseMonthSales dto = CashierResponseMonthSales.builder()
                .month("January")
                .cashierId(1)
                .cashierName("Cashier 1")
                .orderCount(100)
                .totalSales(500000L)
                .build();

        pb.cashier.Cashier.CashierResponseMonthSales response = ProtoConverter.toMonthSalesResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getMonth()).isEqualTo("January");
        assertThat(response.getCashierId()).isEqualTo(1);
        assertThat(response.getCashierName()).isEqualTo("Cashier 1");
        assertThat(response.getOrderCount()).isEqualTo(100);
        assertThat(response.getTotalSales()).isEqualTo(500000);
    }

    @Test
    void toYearSalesResponse_shouldMapAllFields() {
        CashierResponseYearSales dto = CashierResponseYearSales.builder()
                .year("2024")
                .cashierId(2)
                .cashierName("Cashier 2")
                .orderCount(1200)
                .totalSales(6000000L)
                .build();

        pb.cashier.Cashier.CashierResponseYearSales response = ProtoConverter.toYearSalesResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getYear()).isEqualTo("2024");
        assertThat(response.getCashierId()).isEqualTo(2);
        assertThat(response.getCashierName()).isEqualTo("Cashier 2");
        assertThat(response.getOrderCount()).isEqualTo(1200);
        assertThat(response.getTotalSales()).isEqualTo(6000000);
    }

    @Test
    void toMonthTotalSalesResponse_shouldMapAllFields() {
        CashierResponseMonthTotalSales dto = CashierResponseMonthTotalSales.builder()
                .year("2024")
                .month("January")
                .totalSales(150000L)
                .build();

        pb.cashier.Cashier.CashierResponseMonthTotalSales response = ProtoConverter.toMonthTotalSalesResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getYear()).isEqualTo("2024");
        assertThat(response.getMonth()).isEqualTo("January");
        assertThat(response.getTotalSales()).isEqualTo(150000);
    }

    @Test
    void toYearTotalSalesResponse_shouldMapAllFields() {
        CashierResponseYearTotalSales dto = CashierResponseYearTotalSales.builder()
                .year("2024")
                .totalSales(2500000L)
                .build();

        pb.cashier.Cashier.CashierResponseYearTotalSales response = ProtoConverter.toYearTotalSalesResponse(dto);

        assertThat(response).isNotNull();
        assertThat(response.getYear()).isEqualTo("2024");
        assertThat(response.getTotalSales()).isEqualTo(2500000);
    }

    @Test
    void nullSafety_forAllConverterMethods() {
        assertThat(ProtoConverter.toCashierResponse((CashierResponse) null)).isNotNull();
        assertThat(ProtoConverter.toCashierResponse((Cashier) null)).isNotNull();
        assertThat(ProtoConverter.toCashierResponseDeleteAt((CashierResponseDeleteAt) null)).isNotNull();
        assertThat(ProtoConverter.toCashierResponseDeleteAt((Cashier) null)).isNotNull();
        assertThat(ProtoConverter.toMonthSalesResponse(null)).isNotNull();
        assertThat(ProtoConverter.toYearSalesResponse(null)).isNotNull();
        assertThat(ProtoConverter.toMonthTotalSalesResponse(null)).isNotNull();
        assertThat(ProtoConverter.toYearTotalSalesResponse(null)).isNotNull();
    }
}
