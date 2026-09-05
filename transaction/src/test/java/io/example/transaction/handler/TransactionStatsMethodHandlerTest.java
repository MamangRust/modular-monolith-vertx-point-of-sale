package io.example.transaction.handler;

import java.util.List;

import io.example.transaction.domain.response.TransactionMonthlyMethodResponse;
import io.example.transaction.domain.response.TransactionYearlyMethodResponse;
import io.example.transaction.service.TransactionStatsService;
import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.transaction.stats.ApiResponseTransactionMonthMethod;
import pb.transaction.stats.ApiResponseTransactionYearMethod;
import pb.transaction.stats.MonthTransactionMethod;
import pb.transaction.stats.MonthTransactionMethodByMerchant;
import pb.transaction.stats.YearTransactionMethod;
import pb.transaction.stats.YearTransactionMethodByMerchant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionStatsMethodHandlerTest {

    @Mock
    private TransactionStatsService statsService;

    private TransactionStatsMethodHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransactionStatsMethodHandler(statsService);
    }

    @Test
    void findMonthMethodSuccess_shouldReturnResponse() {
        TransactionMonthlyMethodResponse resp = new TransactionMonthlyMethodResponse("06", "credit_card", 10, 500000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findMonthlyPaymentMethodsSuccess(any());

        MonthTransactionMethod req = MonthTransactionMethod.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .build();

        Future<ApiResponseTransactionMonthMethod> future = handler.findMonthMethodSuccess(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionMonthMethod result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly payment methods (success) fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findMonthlyPaymentMethodsSuccess(any());
    }

    @Test
    void findYearMethodSuccess_shouldReturnResponse() {
        TransactionYearlyMethodResponse resp = new TransactionYearlyMethodResponse("2024", "credit_card", 100, 5000000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findYearlyPaymentMethodsSuccess(anyInt());

        YearTransactionMethod req = YearTransactionMethod.newBuilder()
                .setYear(2024)
                .build();

        Future<ApiResponseTransactionYearMethod> future = handler.findYearMethodSuccess(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionYearMethod result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly payment methods (success) fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findYearlyPaymentMethodsSuccess(anyInt());
    }

    @Test
    void findMonthMethodFailed_shouldReturnResponse() {
        TransactionMonthlyMethodResponse resp = new TransactionMonthlyMethodResponse("06", "credit_card", 2, 100000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findMonthlyPaymentMethodsFailed(any());

        MonthTransactionMethod req = MonthTransactionMethod.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .build();

        Future<ApiResponseTransactionMonthMethod> future = handler.findMonthMethodFailed(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionMonthMethod result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly payment methods (failed) fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findMonthlyPaymentMethodsFailed(any());
    }

    @Test
    void findYearMethodFailed_shouldReturnResponse() {
        TransactionYearlyMethodResponse resp = new TransactionYearlyMethodResponse("2024", "credit_card", 20, 1000000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findYearlyPaymentMethodsFailed(anyInt());

        YearTransactionMethod req = YearTransactionMethod.newBuilder()
                .setYear(2024)
                .build();

        Future<ApiResponseTransactionYearMethod> future = handler.findYearMethodFailed(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionYearMethod result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly payment methods (failed) fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findYearlyPaymentMethodsFailed(anyInt());
    }

    @Test
    void findMonthMethodByMerchantSuccess_shouldReturnResponse() {
        TransactionMonthlyMethodResponse resp = new TransactionMonthlyMethodResponse("06", "credit_card", 10, 500000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findMonthlyPaymentMethodsByMerchantSuccess(any());

        MonthTransactionMethodByMerchant req = MonthTransactionMethodByMerchant.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .setMerchantId(1)
                .build();

        Future<ApiResponseTransactionMonthMethod> future = handler.findMonthMethodByMerchantSuccess(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionMonthMethod result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly payment methods by merchant (success) fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findMonthlyPaymentMethodsByMerchantSuccess(any());
    }

    @Test
    void findYearMethodByMerchantSuccess_shouldReturnResponse() {
        TransactionYearlyMethodResponse resp = new TransactionYearlyMethodResponse("2024", "credit_card", 100, 5000000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findYearlyPaymentMethodsByMerchantSuccess(any());

        YearTransactionMethodByMerchant req = YearTransactionMethodByMerchant.newBuilder()
                .setYear(2024)
                .setMerchantId(1)
                .build();

        Future<ApiResponseTransactionYearMethod> future = handler.findYearMethodByMerchantSuccess(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionYearMethod result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly payment methods by merchant (success) fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findYearlyPaymentMethodsByMerchantSuccess(any());
    }

    @Test
    void findMonthMethodByMerchantFailed_shouldReturnResponse() {
        TransactionMonthlyMethodResponse resp = new TransactionMonthlyMethodResponse("06", "credit_card", 2, 100000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findMonthlyPaymentMethodsByMerchantFailed(any());

        MonthTransactionMethodByMerchant req = MonthTransactionMethodByMerchant.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .setMerchantId(1)
                .build();

        Future<ApiResponseTransactionMonthMethod> future = handler.findMonthMethodByMerchantFailed(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionMonthMethod result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly payment methods by merchant (failed) fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findMonthlyPaymentMethodsByMerchantFailed(any());
    }

    @Test
    void findYearMethodByMerchantFailed_shouldReturnResponse() {
        TransactionYearlyMethodResponse resp = new TransactionYearlyMethodResponse("2024", "credit_card", 20, 1000000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findYearlyPaymentMethodsByMerchantFailed(any());

        YearTransactionMethodByMerchant req = YearTransactionMethodByMerchant.newBuilder()
                .setYear(2024)
                .setMerchantId(1)
                .build();

        Future<ApiResponseTransactionYearMethod> future = handler.findYearMethodByMerchantFailed(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionYearMethod result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly payment methods by merchant (failed) fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findYearlyPaymentMethodsByMerchantFailed(any());
    }
}
