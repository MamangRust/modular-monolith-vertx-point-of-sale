package io.example.transaction.handler;

import com.google.protobuf.Empty;

import io.example.transaction.domain.response.TransactionResponse;
import io.example.transaction.domain.response.TransactionResponseDeleteAt;
import io.example.transaction.service.TransactionCommandService;
import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.transaction.ApiResponseTransaction;
import pb.transaction.ApiResponseTransactionAll;
import pb.transaction.ApiResponseTransactionDelete;
import pb.transaction.ApiResponseTransactionDeleteAt;
import pb.transaction.CreateTransactionRequest;
import pb.transaction.FindByIdTransactionRequest;
import pb.transaction.UpdateTransactionRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionCommandHandlerTest {

    @Mock
    private TransactionCommandService commandService;

    private TransactionCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransactionCommandHandler(commandService);
    }

    @Test
    void create_shouldReturnApiResponse() {
        TransactionResponse resp = new TransactionResponse(1L, 1, 1, "credit_card", 50000, 0,
                "SUCCESS", "2024-01-01", "2024-06-01");
        doReturn(Future.succeededFuture(resp)).when(commandService).createTransaction(any());

        CreateTransactionRequest req = CreateTransactionRequest.newBuilder()
                .setOrderId(1)
                .setMerchantId(1)
                .setPaymentMethod("credit_card")
                .setAmount(50000)
                .setPaymentStatus("SUCCESS")
                .build();

        Future<ApiResponseTransaction> future = handler.create(req);

        assertTrue(future.succeeded());
        ApiResponseTransaction result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Transaction created successfully", result.getMessage());
        assertNotNull(result.getData());

        verify(commandService).createTransaction(any());
    }

    @Test
    void update_shouldReturnApiResponse() {
        TransactionResponse resp = new TransactionResponse(1L, 1, 1, "credit_card", 50000, 0,
                "SUCCESS", "2024-01-01", "2024-06-01");
        doReturn(Future.succeededFuture(resp)).when(commandService).updateTransaction(any());

        UpdateTransactionRequest req = UpdateTransactionRequest.newBuilder()
                .setOrderId(1)
                .setPaymentMethod("credit_card")
                .setAmount(50000)
                .setPaymentStatus("SUCCESS")
                .build();

        Future<ApiResponseTransaction> future = handler.update(req);

        assertTrue(future.succeeded());
        ApiResponseTransaction result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Transaction updated successfully", result.getMessage());
        assertNotNull(result.getData());

        verify(commandService).updateTransaction(any());
    }

    @Test
    void trashedTransaction_shouldReturnApiResponse() {
        TransactionResponseDeleteAt resp = new TransactionResponseDeleteAt(1L, 1, 1, "credit_card", 50000, 0,
                "SUCCESS", "2024-01-01", "2024-06-01", "2024-07-01");
        doReturn(Future.succeededFuture(resp)).when(commandService).trashTransaction(any());

        FindByIdTransactionRequest req = FindByIdTransactionRequest.newBuilder()
                .setTransactionId(1)
                .build();

        Future<ApiResponseTransactionDeleteAt> future = handler.trashedTransaction(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionDeleteAt result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Transaction trashed successfully", result.getMessage());
        assertNotNull(result.getData());

        verify(commandService).trashTransaction(any());
    }

    @Test
    void restoreTransaction_shouldReturnApiResponse() {
        TransactionResponseDeleteAt resp = new TransactionResponseDeleteAt(1L, 1, 1, "credit_card", 50000, 0,
                "SUCCESS", "2024-01-01", "2024-06-01", "2024-07-01");
        doReturn(Future.succeededFuture(resp)).when(commandService).restoreTransaction(any());

        FindByIdTransactionRequest req = FindByIdTransactionRequest.newBuilder()
                .setTransactionId(1)
                .build();

        Future<ApiResponseTransactionDeleteAt> future = handler.restoreTransaction(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionDeleteAt result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Transaction restored successfully", result.getMessage());
        assertNotNull(result.getData());

        verify(commandService).restoreTransaction(any());
    }

    @Test
    void deleteTransactionPermanent_shouldReturnApiResponse() {
        doReturn(Future.succeededFuture(null)).when(commandService).deletePermanent(any());

        FindByIdTransactionRequest req = FindByIdTransactionRequest.newBuilder()
                .setTransactionId(1)
                .build();

        Future<ApiResponseTransactionDelete> future = handler.deleteTransactionPermanent(req);

        assertTrue(future.succeeded());
        ApiResponseTransactionDelete result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Transaction permanently deleted successfully", result.getMessage());

        verify(commandService).deletePermanent(any());
    }

    @Test
    void restoreAllTransaction_shouldReturnApiResponse() {
        doReturn(Future.succeededFuture(null)).when(commandService).restoreAllTransactions();

        Future<ApiResponseTransactionAll> future = handler.restoreAllTransaction(Empty.getDefaultInstance());

        assertTrue(future.succeeded());
        ApiResponseTransactionAll result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("All transactions restored successfully", result.getMessage());

        verify(commandService).restoreAllTransactions();
    }

    @Test
    void deleteAllTransactionPermanent_shouldReturnApiResponse() {
        doReturn(Future.succeededFuture(null)).when(commandService).deleteAllPermanentTransactions();

        Future<ApiResponseTransactionAll> future = handler.deleteAllTransactionPermanent(Empty.getDefaultInstance());

        assertTrue(future.succeeded());
        ApiResponseTransactionAll result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("All transactions permanently deleted successfully", result.getMessage());

        verify(commandService).deleteAllPermanentTransactions();
    }
}
