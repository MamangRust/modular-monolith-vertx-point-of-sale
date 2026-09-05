package io.example.transaction.handler;

import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.transaction.domain.response.TransactionResponse;
import io.example.transaction.domain.response.TransactionResponseDeleteAt;
import io.example.transaction.service.TransactionQueryService;
import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.transaction.ApiResponsePaginationTransaction;
import pb.transaction.ApiResponsePaginationTransactionDeleteAt;
import pb.transaction.ApiResponseTransaction;
import pb.transaction.FindAllTransactionMerchantRequest;
import pb.transaction.FindAllTransactionRequest;
import pb.transaction.FindByIdTransactionRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionQueryHandlerTest {

    @Mock
    private TransactionQueryService queryService;

    private TransactionQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new TransactionQueryHandler(queryService);
    }

    @Test
    void findAll_shouldReturnPagedResponse() {
        TransactionResponse resp = new TransactionResponse(1L, 1, 1, "credit_card", 50000, 0,
                "SUCCESS", "2024-01-01", "2024-06-01");
        PagedResult<TransactionResponse> paged = new PagedResult<>(List.of(resp), 1);
        doReturn(Future.succeededFuture(paged)).when(queryService).findAllTransaction(any());

        FindAllTransactionRequest req = FindAllTransactionRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        Future<ApiResponsePaginationTransaction> future = handler.findAll(req);

        assertTrue(future.succeeded());
        ApiResponsePaginationTransaction result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Transactions retrieved successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);
        assertNotNull(result.getPagination());

        verify(queryService).findAllTransaction(any());
    }

    @Test
    void findByMerchant_shouldReturnPagedResponse() {
        TransactionResponse resp = new TransactionResponse(1L, 1, 1, "credit_card", 50000, 0,
                "SUCCESS", "2024-01-01", "2024-06-01");
        PagedResult<TransactionResponse> paged = new PagedResult<>(List.of(resp), 1);
        doReturn(Future.succeededFuture(paged)).when(queryService).findAllTransactionByMerchant(any());

        FindAllTransactionMerchantRequest req = FindAllTransactionMerchantRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .setMerchantId(1)
                .build();

        Future<ApiResponsePaginationTransaction> future = handler.findByMerchant(req);

        assertTrue(future.succeeded());
        ApiResponsePaginationTransaction result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Transactions by merchant retrieved successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);
        assertNotNull(result.getPagination());

        verify(queryService).findAllTransactionByMerchant(any());
    }

    @Test
    void findById_shouldReturnResponse() {
        TransactionResponse resp = new TransactionResponse(1L, 1, 1, "credit_card", 50000, 0,
                "SUCCESS", "2024-01-01", "2024-06-01");
        doReturn(Future.succeededFuture(resp)).when(queryService).findByIdTransaction(any());

        FindByIdTransactionRequest req = FindByIdTransactionRequest.newBuilder()
                .setTransactionId(1)
                .build();

        Future<ApiResponseTransaction> future = handler.findById(req);

        assertTrue(future.succeeded());
        ApiResponseTransaction result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Transaction found successfully", result.getMessage());
        assertNotNull(result.getData());

        verify(queryService).findByIdTransaction(any());
    }

    @Test
    void findByActive_shouldReturnPagedDeleteAt() {
        TransactionResponseDeleteAt delResp = new TransactionResponseDeleteAt(1L, 1, 1, "credit_card", 50000, 0,
                "SUCCESS", "2024-01-01", "2024-06-01", null);
        PagedResult<TransactionResponseDeleteAt> pagedDel = new PagedResult<>(List.of(delResp), 1);
        doReturn(Future.succeededFuture(pagedDel)).when(queryService).findByActiveTransaction(any());

        FindAllTransactionRequest req = FindAllTransactionRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        Future<ApiResponsePaginationTransactionDeleteAt> future = handler.findByActive(req);

        assertTrue(future.succeeded());
        ApiResponsePaginationTransactionDeleteAt result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Active transactions retrieved successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);
        assertNotNull(result.getPagination());

        verify(queryService).findByActiveTransaction(any());
    }

    @Test
    void findByTrashed_shouldReturnPagedDeleteAt() {
        TransactionResponseDeleteAt delResp = new TransactionResponseDeleteAt(1L, 1, 1, "credit_card", 50000, 0,
                "SUCCESS", "2024-01-01", "2024-06-01", "2024-07-01");
        PagedResult<TransactionResponseDeleteAt> pagedDel = new PagedResult<>(List.of(delResp), 1);
        doReturn(Future.succeededFuture(pagedDel)).when(queryService).findByTrashedTransaction(any());

        FindAllTransactionRequest req = FindAllTransactionRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        Future<ApiResponsePaginationTransactionDeleteAt> future = handler.findByTrashed(req);

        assertTrue(future.succeeded());
        ApiResponsePaginationTransactionDeleteAt result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Trashed transactions retrieved successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);
        assertNotNull(result.getPagination());

        verify(queryService).findByTrashedTransaction(any());
    }
}
