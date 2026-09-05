package io.example.merchant.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.domain.PagedResult;
import io.example.merchant.domain.requests.FindAllMerchants;
import io.example.merchant.domain.response.MerchantResponse;
import io.example.merchant.domain.response.MerchantResponseDeleteAt;
import io.example.merchant.service.MerchantQueryService;
import io.vertx.core.Future;

import pb.merchant.Merchant.ApiResponseMerchant;
import pb.merchant.Merchant.FindAllMerchantRequest;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.MerchantQuery.ApiResponsePaginationMerchant;
import pb.merchant.MerchantQuery.ApiResponsePaginationMerchantDeleteAt;

@ExtendWith(MockitoExtension.class)
class MerchantQueryHandlerTest {

    @Mock
    private MerchantQueryService service;

    private MerchantQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MerchantQueryHandler(service);
    }

    @Test
    void findAllMerchant_shouldReturnPagedResponse() {
        FindAllMerchantRequest request = FindAllMerchantRequest.newBuilder()
                .setSearch("merchant")
                .setPage(1)
                .setPageSize(10)
                .build();

        MerchantResponse resp = new MerchantResponse(1L, 1, "Merchant A", "Desc", "Addr", "e@m.com", "0812", "ACTIVE",
                "2024-01-01", "2024-06-01");

        PagedResult<MerchantResponse> paged = new PagedResult<>(List.of(resp), 1);

        doReturn(Future.succeededFuture(paged)).when(service).findAll(any(FindAllMerchants.class));

        Future<ApiResponsePaginationMerchant> result = handler.findAllMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Merchants retrieved successfully");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("Merchant A");
        verify(service).findAll(any(FindAllMerchants.class));
    }

    @Test
    void findByIdMerchant_shouldReturnMerchantResponse() {
        FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                .setMerchantId(1)
                .build();

        MerchantResponse resp = new MerchantResponse(1L, 1, "Merchant A", "Desc", "Addr", "e@m.com", "0812", "ACTIVE",
                "2024-01-01", "2024-06-01");

        doReturn(Future.succeededFuture(resp)).when(service).findById(1L);

        Future<ApiResponseMerchant> result = handler.findByIdMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Merchant found successfully");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        verify(service).findById(1L);
    }

    @Test
    void findByActive_shouldReturnPagedDeleteAtResponse() {
        FindAllMerchantRequest request = FindAllMerchantRequest.newBuilder()
                .setSearch("active")
                .setPage(1)
                .setPageSize(10)
                .build();

        MerchantResponseDeleteAt delResp = new MerchantResponseDeleteAt(1L, 1, "Active Merchant", "Desc", "Addr",
                "e@m.com", "0812", "ACTIVE", "2024-01-01", "2024-06-01", null);

        PagedResult<MerchantResponseDeleteAt> paged = new PagedResult<>(List.of(delResp), 1);

        doReturn(Future.succeededFuture(paged)).when(service).findByActive(any(FindAllMerchants.class));

        Future<ApiResponsePaginationMerchantDeleteAt> result = handler.findByActive(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Active merchants retrieved successfully");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("Active Merchant");
        verify(service).findByActive(any(FindAllMerchants.class));
    }

    @Test
    void findByTrashed_shouldReturnPagedDeleteAtResponse() {
        FindAllMerchantRequest request = FindAllMerchantRequest.newBuilder()
                .setSearch("trashed")
                .setPage(1)
                .setPageSize(10)
                .build();

        MerchantResponseDeleteAt delResp = new MerchantResponseDeleteAt(1L, 1, "Trashed Merchant", "Desc", "Addr",
                "e@m.com", "0812", "INACTIVE", "2024-01-01", "2024-06-01", "2024-06-02");

        PagedResult<MerchantResponseDeleteAt> paged = new PagedResult<>(List.of(delResp), 1);

        doReturn(Future.succeededFuture(paged)).when(service).findByTrashed(any(FindAllMerchants.class));

        Future<ApiResponsePaginationMerchantDeleteAt> result = handler.findByTrashed(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Trashed merchants retrieved successfully");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("Trashed Merchant");
        verify(service).findByTrashed(any(FindAllMerchants.class));
    }
}
