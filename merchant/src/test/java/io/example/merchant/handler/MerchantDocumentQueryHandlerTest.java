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
import io.example.merchant.domain.requests.FindAllMerchantDocuments;
import io.example.merchant.domain.response.MerchantDocumentResponse;
import io.example.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import io.example.merchant.service.MerchantDocumentQueryService;
import io.vertx.core.Future;

import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;
import pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocument;
import pb.merchant_document.MerchantDocumentQuery.ApiResponsePaginationMerchantDocumentAt;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentQueryHandlerTest {

    @Mock
    private MerchantDocumentQueryService service;

    private MerchantDocumentQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MerchantDocumentQueryHandler(service);
    }

    @Test
    void findAll_shouldReturnPagedResponse() {
        FindAllMerchantDocumentsRequest request = FindAllMerchantDocumentsRequest.newBuilder()
                .setSearch("doc")
                .setPage(1)
                .setPageSize(10)
                .build();

        MerchantDocumentResponse resp = new MerchantDocumentResponse(1, 1, "KTP", "http://example.com/doc",
                "ACTIVE", null, "2024-01-01", "2024-06-01");

        PagedResult<MerchantDocumentResponse> paged = new PagedResult<>(List.of(resp), 1);

        doReturn(Future.succeededFuture(paged)).when(service).findAll(any(FindAllMerchantDocuments.class));

        Future<ApiResponsePaginationMerchantDocument> result = handler.findAll(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Active documents retrieved successfully");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getDocumentType()).isEqualTo("KTP");
        verify(service).findAll(any(FindAllMerchantDocuments.class));
    }

    @Test
    void findAllActive_shouldReturnPagedDeleteAtResponse() {
        FindAllMerchantDocumentsRequest request = FindAllMerchantDocumentsRequest.newBuilder()
                .setSearch("active")
                .setPage(1)
                .setPageSize(10)
                .build();

        MerchantDocumentResponseDeleteAt delResp = new MerchantDocumentResponseDeleteAt(1, 1, "KTP",
                "http://example.com/doc", "ACTIVE", null, "2024-01-01", "2024-06-01", null);

        PagedResult<MerchantDocumentResponseDeleteAt> paged = new PagedResult<>(List.of(delResp), 1);

        doReturn(Future.succeededFuture(paged)).when(service).findByActive(any(FindAllMerchantDocuments.class));

        Future<ApiResponsePaginationMerchantDocumentAt> result = handler.findAllActive(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Active documents retrieved successfully");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getDocumentType()).isEqualTo("KTP");
        verify(service).findByActive(any(FindAllMerchantDocuments.class));
    }

    @Test
    void findAllTrashed_shouldReturnPagedDeleteAtResponse() {
        FindAllMerchantDocumentsRequest request = FindAllMerchantDocumentsRequest.newBuilder()
                .setSearch("trashed")
                .setPage(1)
                .setPageSize(10)
                .build();

        MerchantDocumentResponseDeleteAt delResp = new MerchantDocumentResponseDeleteAt(1, 1, "NPWP",
                "http://example.com/npwp", "INACTIVE", null, "2024-01-01", "2024-06-01", "2024-06-02");

        PagedResult<MerchantDocumentResponseDeleteAt> paged = new PagedResult<>(List.of(delResp), 1);

        doReturn(Future.succeededFuture(paged)).when(service).findByTrashed(any(FindAllMerchantDocuments.class));

        Future<ApiResponsePaginationMerchantDocumentAt> result = handler.findAllTrashed(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Trashed documents retrieved successfully");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getDocumentType()).isEqualTo("NPWP");
        verify(service).findByTrashed(any(FindAllMerchantDocuments.class));
    }

    @Test
    void findById_shouldReturnDocumentResponse() {
        FindMerchantDocumentByIdRequest request = FindMerchantDocumentByIdRequest.newBuilder()
                .setDocumentId(1)
                .build();

        MerchantDocumentResponse resp = new MerchantDocumentResponse(1, 1, "KTP", "http://example.com/doc",
                "ACTIVE", null, "2024-01-01", "2024-06-01");

        doReturn(Future.succeededFuture(resp)).when(service).findById(1L);

        Future<ApiResponseMerchantDocument> result = handler.findById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Document found successfully");
        assertThat(result.result().getData().getDocumentId()).isEqualTo(1);
        verify(service).findById(1L);
    }
}
