package io.example.transaction.handler;

import java.util.List;

import io.example.transaction.domain.response.TransactionMonthlyAmountFailedResponse;
import io.example.transaction.domain.response.TransactionMonthlyAmountSuccessResponse;
import io.example.transaction.domain.response.TransactionYearlyAmountFailedResponse;
import io.example.transaction.domain.response.TransactionYearlyAmountSuccessResponse;
import io.example.transaction.service.TransactionStatsService;
import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.transaction.stats.ApiResponseTransactionMonthStatusFailed;
import pb.transaction.stats.ApiResponseTransactionMonthStatusSuccess;
import pb.transaction.stats.ApiResponseTransactionYearStatusFailed;
import pb.transaction.stats.ApiResponseTransactionYearStatusSuccess;
import pb.transaction.stats.FindMonthlyTransactionStatus;
import pb.transaction.stats.FindMonthlyTransactionStatusByMerchant;
import pb.transaction.stats.FindYearlyTransactionStatus;
import pb.transaction.stats.FindYearlyTransactionStatusByMerchant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionStatsStatusHandlerTest {

    @Mock
    private TransactionStatsService statsService;

    private TransactionStatsStatusHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransactionStatsStatusHandler(statsService);
    }

    @Test
    void findMonthStatusSuccess_shouldReturnResponse() {
        TransactionMonthlyAmountSuccessResponse resp = new TransactionMonthlyAmountSuccessResponse(
                "2024", "06", 10, 500000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findMonthlyTransactionStatusSuccess(any());

        FindMonthlyTransactionStatus req = FindMonthlyTransactionStatus.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .build();

        Future<ApiResponseTransactionMonthStatusSuccess> future = handler.findMonthStatusSuccess(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionMonthStatusSuccess result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly successful transactions status fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findMonthlyTransactionStatusSuccess(any());
    }

    @Test
    void findYearStatusSuccess_shouldReturnResponse() {
        TransactionYearlyAmountSuccessResponse resp = new TransactionYearlyAmountSuccessResponse(
                "2024", 100, 5000000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findYearlyTransactionStatusSuccess(anyInt());

        FindYearlyTransactionStatus req = FindYearlyTransactionStatus.newBuilder()
                .setYear(2024)
                .build();

        Future<ApiResponseTransactionYearStatusSuccess> future = handler.findYearStatusSuccess(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionYearStatusSuccess result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly successful transactions status fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findYearlyTransactionStatusSuccess(anyInt());
    }

    @Test
    void findMonthStatusFailed_shouldReturnResponse() {
        TransactionMonthlyAmountFailedResponse resp = new TransactionMonthlyAmountFailedResponse(
                "2024", "06", 2, 100000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findMonthlyTransactionStatusFailed(any());

        FindMonthlyTransactionStatus req = FindMonthlyTransactionStatus.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .build();

        Future<ApiResponseTransactionMonthStatusFailed> future = handler.findMonthStatusFailed(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionMonthStatusFailed result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly failed transactions status fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findMonthlyTransactionStatusFailed(any());
    }

    @Test
    void findYearStatusFailed_shouldReturnResponse() {
        TransactionYearlyAmountFailedResponse resp = new TransactionYearlyAmountFailedResponse(
                "2024", 20, 1000000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService).findYearlyTransactionStatusFailed(anyInt());

        FindYearlyTransactionStatus req = FindYearlyTransactionStatus.newBuilder()
                .setYear(2024)
                .build();

        Future<ApiResponseTransactionYearStatusFailed> future = handler.findYearStatusFailed(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionYearStatusFailed result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly failed transactions status fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findYearlyTransactionStatusFailed(anyInt());
    }

    @Test
    void findMonthStatusSuccessByMerchant_shouldReturnResponse() {
        TransactionMonthlyAmountSuccessResponse resp = new TransactionMonthlyAmountSuccessResponse(
                "2024", "06", 10, 500000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService)
                .findMonthlyTransactionStatusSuccessByMerchant(any());

        FindMonthlyTransactionStatusByMerchant req = FindMonthlyTransactionStatusByMerchant.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .setMerchantId(1)
                .build();

        Future<ApiResponseTransactionMonthStatusSuccess> future = handler.findMonthStatusSuccessByMerchant(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionMonthStatusSuccess result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly successful transactions by merchant fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findMonthlyTransactionStatusSuccessByMerchant(any());
    }

    @Test
    void findYearStatusSuccessByMerchant_shouldReturnResponse() {
        TransactionYearlyAmountSuccessResponse resp = new TransactionYearlyAmountSuccessResponse(
                "2024", 100, 5000000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService)
                .findYearlyTransactionStatusSuccessByMerchant(any());

        FindYearlyTransactionStatusByMerchant req = FindYearlyTransactionStatusByMerchant.newBuilder()
                .setYear(2024)
                .setMerchantId(1)
                .build();

        Future<ApiResponseTransactionYearStatusSuccess> future = handler.findYearStatusSuccessByMerchant(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionYearStatusSuccess result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly successful transactions by merchant fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findYearlyTransactionStatusSuccessByMerchant(any());
    }

    @Test
    void findMonthStatusFailedByMerchant_shouldReturnResponse() {
        TransactionMonthlyAmountFailedResponse resp = new TransactionMonthlyAmountFailedResponse(
                "2024", "06", 2, 100000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService)
                .findMonthlyTransactionStatusFailedByMerchant(any());

        FindMonthlyTransactionStatusByMerchant req = FindMonthlyTransactionStatusByMerchant.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .setMerchantId(1)
                .build();

        Future<ApiResponseTransactionMonthStatusFailed> future = handler.findMonthStatusFailedByMerchant(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionMonthStatusFailed result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly failed transactions by merchant fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findMonthlyTransactionStatusFailedByMerchant(any());
    }

    @Test
    void findYearStatusFailedByMerchant_shouldReturnResponse() {
        TransactionYearlyAmountFailedResponse resp = new TransactionYearlyAmountFailedResponse(
                "2024", 20, 1000000L);
        doReturn(Future.succeededFuture(List.of(resp))).when(statsService)
                .findYearlyTransactionStatusFailedByMerchant(any());

        FindYearlyTransactionStatusByMerchant req = FindYearlyTransactionStatusByMerchant.newBuilder()
                .setYear(2024)
                .setMerchantId(1)
                .build();

        Future<ApiResponseTransactionYearStatusFailed> future = handler.findYearStatusFailedByMerchant(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionYearStatusFailed result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly failed transactions by merchant fetched successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findYearlyTransactionStatusFailedByMerchant(any());
    }
}
