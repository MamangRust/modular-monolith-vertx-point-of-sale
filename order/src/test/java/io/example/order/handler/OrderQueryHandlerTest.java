package io.example.order.handler;

import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.order.domain.response.order.OrderMonthlyResponse;
import io.example.order.domain.response.order.OrderMonthlyTotalRevenueResponse;
import io.example.order.domain.response.order.OrderResponse;
import io.example.order.domain.response.order.OrderResponseDeleteAt;
import io.example.order.domain.response.order.OrderYearlyResponse;
import io.example.order.domain.response.order.OrderYearlyTotalRevenueResponse;
import io.example.order.service.OrderQueryService;
import io.example.order.service.OrderStatByMerchantService;
import io.example.order.service.OrderStatsService;
import io.vertx.core.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pb.order.Order.ApiResponseOrder;
import pb.order.Order.ApiResponseOrderMonthly;
import pb.order.Order.ApiResponseOrderMonthlyTotalRevenue;
import pb.order.Order.ApiResponseOrderYearly;
import pb.order.Order.ApiResponseOrderYearlyTotalRevenue;
import pb.order.Order.FindAllOrderMerchantRequest;
import pb.order.Order.FindAllOrderRequest;
import pb.order.Order.FindByIdOrderRequest;
import pb.order.Order.FindYearMonthTotalRevenue;
import pb.order.Order.FindYearMonthTotalRevenueByMerchant;
import pb.order.Order.FindYearOrder;
import pb.order.Order.FindYearOrderByMerchant;
import pb.order.Order.FindYearTotalRevenue;
import pb.order.Order.FindYearTotalRevenueByMerchant;
import pb.order.OrderQuery.ApiResponsePaginationOrder;
import pb.order.OrderQuery.ApiResponsePaginationOrderDeleteAt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderQueryHandlerTest {

    @Mock
    private OrderQueryService queryService;

    @Mock
    private OrderStatsService statsService;

    @Mock
    private OrderStatByMerchantService statByMerchantService;

    private OrderQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OrderQueryHandler(queryService, statsService, statByMerchantService);
    }

    @Test
    void findAll_shouldReturnPagedResponse() {
        OrderResponse resp = new OrderResponse(1L, 1, 1, 5000L, "2024-01-01", "2024-06-01");
        PagedResult<OrderResponse> paged = new PagedResult<>(List.of(resp), 1);
        doReturn(Future.succeededFuture(paged)).when(queryService).findAll(any());

        FindAllOrderRequest req = FindAllOrderRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .setSearch("")
                .build();

        Future<ApiResponsePaginationOrder> future = handler.findAll(req);

        assertTrue(future.succeeded());
        ApiResponsePaginationOrder result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Orders retrieved successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);
        assertNotNull(result.getPagination());

        verify(queryService).findAll(any());
    }

    @Test
    void findById_shouldReturnResponse() {
        OrderResponse resp = new OrderResponse(1L, 1, 1, 5000L, "2024-01-01", "2024-06-01");
        doReturn(Future.succeededFuture(resp)).when(queryService).findById(any());

        FindByIdOrderRequest req = FindByIdOrderRequest.newBuilder()
                .setId(1)
                .build();

        Future<ApiResponseOrder> future = handler.findById(req);

        assertTrue(future.succeeded());
        ApiResponseOrder result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Order found", result.getMessage());
        assertNotNull(result.getData());

        verify(queryService).findById(any());
    }

    @Test
    void findByActive_shouldReturnPagedDeleteAt() {
        OrderResponseDeleteAt delResp = new OrderResponseDeleteAt(1L, 1, 1, 5000L,
                "2024-01-01", "2024-06-01", null);
        PagedResult<OrderResponseDeleteAt> pagedDel = new PagedResult<>(List.of(delResp), 1);
        doReturn(Future.succeededFuture(pagedDel)).when(queryService).findByActive(any());

        FindAllOrderRequest req = FindAllOrderRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .setSearch("")
                .build();

        Future<ApiResponsePaginationOrderDeleteAt> future = handler.findByActive(req);

        assertTrue(future.succeeded());
        ApiResponsePaginationOrderDeleteAt result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Active orders retrieved successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);
        assertNotNull(result.getPagination());

        verify(queryService).findByActive(any());
    }

    @Test
    void findByTrashed_shouldReturnPagedDeleteAt() {
        OrderResponseDeleteAt delResp = new OrderResponseDeleteAt(1L, 1, 1, 5000L,
                "2024-01-01", "2024-06-01", "2024-07-01");
        PagedResult<OrderResponseDeleteAt> pagedDel = new PagedResult<>(List.of(delResp), 1);
        doReturn(Future.succeededFuture(pagedDel)).when(queryService).findByTrashed(any());

        FindAllOrderRequest req = FindAllOrderRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .setSearch("")
                .build();

        Future<ApiResponsePaginationOrderDeleteAt> future = handler.findByTrashed(req);

        assertTrue(future.succeeded());
        ApiResponsePaginationOrderDeleteAt result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Trashed orders retrieved successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);
        assertNotNull(result.getPagination());

        verify(queryService).findByTrashed(any());
    }

    @Test
    void findMonthlyRevenue_shouldReturnResponse() {
        OrderMonthlyResponse mr = new OrderMonthlyResponse("2024-06", 10, 50000L, 50);
        doReturn(Future.succeededFuture(List.of(mr))).when(statsService).findMonthlyOrder(anyInt());

        FindYearOrder req = FindYearOrder.newBuilder()
                .setYear(2024)
                .build();

        Future<ApiResponseOrderMonthly> future = handler.findMonthlyRevenue(req);

        assertTrue(future.succeeded());
        ApiResponseOrderMonthly result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly revenue statistics retrieved", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findMonthlyOrder(anyInt());
    }

    @Test
    void findYearlyRevenue_shouldReturnResponse() {
        OrderYearlyResponse yr = new OrderYearlyResponse("2024", 100, 500000L, 500, 5, 20);
        doReturn(Future.succeededFuture(List.of(yr))).when(statsService).findYearlyOrder(anyInt());

        FindYearOrder req = FindYearOrder.newBuilder()
                .setYear(2024)
                .build();

        Future<ApiResponseOrderYearly> future = handler.findYearlyRevenue(req);

        assertTrue(future.succeeded());
        ApiResponseOrderYearly result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly revenue statistics retrieved", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findYearlyOrder(anyInt());
    }

    @Test
    void findMonthlyTotalRevenue_shouldReturnResponse() {
        OrderMonthlyTotalRevenueResponse mtr = new OrderMonthlyTotalRevenueResponse("2024", "6", 50000L);
        doReturn(Future.succeededFuture(List.of(mtr))).when(statsService).findMonthlyTotalRevenue(any());

        FindYearMonthTotalRevenue req = FindYearMonthTotalRevenue.newBuilder()
                .setYear(2024)
                .setMonth(6)
                .build();

        Future<ApiResponseOrderMonthlyTotalRevenue> future = handler.findMonthlyTotalRevenue(req);

        assertTrue(future.succeeded());
        ApiResponseOrderMonthlyTotalRevenue result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly total revenue retrieved", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findMonthlyTotalRevenue(any());
    }

    @Test
    void findYearlyTotalRevenue_shouldReturnResponse() {
        OrderYearlyTotalRevenueResponse ytr = new OrderYearlyTotalRevenueResponse("2024", 500000L);
        doReturn(Future.succeededFuture(List.of(ytr))).when(statsService).findYearlyTotalRevenue(anyInt());

        FindYearTotalRevenue req = FindYearTotalRevenue.newBuilder()
                .setYear(2024)
                .build();

        Future<ApiResponseOrderYearlyTotalRevenue> future = handler.findYearlyTotalRevenue(req);

        assertTrue(future.succeeded());
        ApiResponseOrderYearlyTotalRevenue result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly total revenue retrieved", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statsService).findYearlyTotalRevenue(anyInt());
    }

    @Test
    void findByMerchant_shouldReturnPagedResponse() {
        OrderResponse resp = new OrderResponse(1L, 1, 1, 5000L, "2024-01-01", "2024-06-01");
        PagedResult<OrderResponse> paged = new PagedResult<>(List.of(resp), 1);
        doReturn(Future.succeededFuture(paged)).when(queryService).findByMerchant(any());

        FindAllOrderMerchantRequest req = FindAllOrderMerchantRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .setSearch("")
                .setMerchantId(1)
                .build();

        Future<ApiResponsePaginationOrder> future = handler.findByMerchant(req);

        assertTrue(future.succeeded());
        ApiResponsePaginationOrder result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Orders by merchant retrieved successfully", result.getMessage());
        assertTrue(result.getDataCount() > 0);
        assertNotNull(result.getPagination());

        verify(queryService).findByMerchant(any());
    }

    @Test
    void findMonthlyRevenueByMerchant_shouldReturnResponse() {
        OrderMonthlyResponse mr = new OrderMonthlyResponse("2024-06", 10, 50000L, 50);
        doReturn(Future.succeededFuture(List.of(mr))).when(statByMerchantService).findMonthlyOrderByMerchant(any());

        FindYearOrderByMerchant req = FindYearOrderByMerchant.newBuilder()
                .setMerchantId(1)
                .setYear(2024)
                .build();

        Future<ApiResponseOrderMonthly> future = handler.findMonthlyRevenueByMerchant(req);

        assertTrue(future.succeeded());
        ApiResponseOrderMonthly result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly revenue by merchant retrieved", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statByMerchantService).findMonthlyOrderByMerchant(any());
    }

    @Test
    void findMonthlyTotalRevenueByMerchant_shouldReturnResponse() {
        OrderMonthlyTotalRevenueResponse mtr = new OrderMonthlyTotalRevenueResponse("2024", "6", 50000L);
        doReturn(Future.succeededFuture(List.of(mtr))).when(statByMerchantService)
                .findMonthlyTotalRevenueByMerchant(any());

        FindYearMonthTotalRevenueByMerchant req = FindYearMonthTotalRevenueByMerchant.newBuilder()
                .setMerchantId(1)
                .setYear(2024)
                .setMonth(6)
                .build();

        Future<ApiResponseOrderMonthlyTotalRevenue> future = handler.findMonthlyTotalRevenueByMerchant(req);

        assertTrue(future.succeeded());
        ApiResponseOrderMonthlyTotalRevenue result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Monthly total revenue by merchant retrieved", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statByMerchantService).findMonthlyTotalRevenueByMerchant(any());
    }

    @Test
    void findYearlyTotalRevenueByMerchant_shouldReturnResponse() {
        OrderYearlyTotalRevenueResponse ytr = new OrderYearlyTotalRevenueResponse("2024", 500000L);
        doReturn(Future.succeededFuture(List.of(ytr))).when(statByMerchantService)
                .findYearlyTotalRevenueByMerchant(any());

        FindYearTotalRevenueByMerchant req = FindYearTotalRevenueByMerchant.newBuilder()
                .setMerchantId(1)
                .setYear(2024)
                .build();

        Future<ApiResponseOrderYearlyTotalRevenue> future = handler.findYearlyTotalRevenueByMerchant(req);

        assertTrue(future.succeeded());
        ApiResponseOrderYearlyTotalRevenue result = future.result();
        assertEquals("success", result.getStatus());
        assertEquals("Yearly total revenue by merchant retrieved", result.getMessage());
        assertTrue(result.getDataCount() > 0);

        verify(statByMerchantService).findYearlyTotalRevenueByMerchant(any());
    }
}
