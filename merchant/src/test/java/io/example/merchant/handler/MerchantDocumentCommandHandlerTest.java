package io.example.merchant.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;

import io.example.merchant.domain.requests.CreateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentRequest;
import io.example.merchant.domain.requests.UpdateMerchantDocumentStatusRequest;
import io.example.merchant.domain.response.MerchantDocumentResponse;
import io.example.merchant.domain.response.MerchantDocumentResponseDeleteAt;
import io.example.merchant.service.MerchantDocumentCommandService;
import io.vertx.core.Future;

import pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentAll;
import pb.merchant_document.MerchantDocumentCommand.ApiResponseMerchantDocumentDelete;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocument;
import pb.merchant_document.MerchantDocumentOuterClass.ApiResponseMerchantDocumentDeleteAt;
import pb.merchant_document.MerchantDocumentOuterClass.FindMerchantDocumentByIdRequest;

@ExtendWith(MockitoExtension.class)
class MerchantDocumentCommandHandlerTest {

    @Mock
    private MerchantDocumentCommandService service;

    private MerchantDocumentCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MerchantDocumentCommandHandler(service);
    }

    @Test
    void create_shouldCallServiceAndReturnSuccess() {
        pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest request = pb.merchant_document.MerchantDocumentCommand.CreateMerchantDocumentRequest
                .newBuilder()
                .setMerchantId(1)
                .setDocumentType("KTP")
                .setDocumentUrl("http://example.com/doc")
                .build();

        MerchantDocumentResponse resp = new MerchantDocumentResponse(1, 1, "KTP", "http://example.com/doc",
                "ACTIVE", null, "2024-01-01", "2024-06-01");

        doReturn(Future.succeededFuture(resp)).when(service)
                .createMerchantDocument(any(CreateMerchantDocumentRequest.class));

        Future<ApiResponseMerchantDocument> result = handler.create(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Document created successfully");
        assertThat(result.result().getData().getDocumentType()).isEqualTo("KTP");
        verify(service).createMerchantDocument(any(CreateMerchantDocumentRequest.class));
    }

    @Test
    void update_shouldCallServiceAndReturnSuccess() {
        pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest request = pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentRequest
                .newBuilder()
                .setDocumentId(1)
                .setMerchantId(1)
                .setDocumentType("NPWP")
                .setDocumentUrl("http://example.com/npwp")
                .setNote("Updated note")
                .setStatus("ACTIVE")
                .build();

        MerchantDocumentResponse resp = new MerchantDocumentResponse(1, 1, "NPWP", "http://example.com/npwp",
                "ACTIVE", "Updated note", "2024-01-01", "2024-06-01");

        doReturn(Future.succeededFuture(resp)).when(service)
                .updateMerchantDocument(any(UpdateMerchantDocumentRequest.class));

        Future<ApiResponseMerchantDocument> result = handler.update(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Document updated successfully");
        assertThat(result.result().getData().getDocumentType()).isEqualTo("NPWP");
        verify(service).updateMerchantDocument(any(UpdateMerchantDocumentRequest.class));
    }

    @Test
    void updateStatus_shouldCallServiceAndReturnSuccess() {
        pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest request = pb.merchant_document.MerchantDocumentCommand.UpdateMerchantDocumentStatusRequest
                .newBuilder()
                .setDocumentId(1)
                .setMerchantId(1)
                .setNote("Verified")
                .setStatus("VERIFIED")
                .build();

        MerchantDocumentResponse resp = new MerchantDocumentResponse(1, 1, "KTP", "http://example.com/doc",
                "VERIFIED", "Verified", "2024-01-01", "2024-06-01");

        doReturn(Future.succeededFuture(resp)).when(service)
                .updateMerchantDocumentStatus(any(UpdateMerchantDocumentStatusRequest.class));

        Future<ApiResponseMerchantDocument> result = handler.updateStatus(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Document status updated successfully");
        assertThat(result.result().getData().getStatus()).isEqualTo("VERIFIED");
        verify(service).updateMerchantDocumentStatus(any(UpdateMerchantDocumentStatusRequest.class));
    }

    @Test
    void trashed_shouldCallServiceAndReturnSuccess() {
        FindMerchantDocumentByIdRequest request = FindMerchantDocumentByIdRequest.newBuilder()
                .setDocumentId(1)
                .build();

        MerchantDocumentResponseDeleteAt delResp = new MerchantDocumentResponseDeleteAt(1, 1, "KTP",
                "http://example.com/doc", "INACTIVE", null, "2024-01-01", "2024-06-01", "2024-06-02");

        doReturn(Future.succeededFuture(delResp)).when(service).trashedMerchantDocument(1L);

        Future<ApiResponseMerchantDocumentDeleteAt> result = handler.trashed(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Document trashed successfully");
        assertThat(result.result().getData().getDeletedAt().getValue()).isEqualTo("2024-06-02");
        verify(service).trashedMerchantDocument(1L);
    }

    @Test
    void restore_shouldCallServiceAndReturnSuccess() {
        FindMerchantDocumentByIdRequest request = FindMerchantDocumentByIdRequest.newBuilder()
                .setDocumentId(1)
                .build();

        MerchantDocumentResponseDeleteAt delResp = new MerchantDocumentResponseDeleteAt(1, 1, "KTP",
                "http://example.com/doc", "ACTIVE", null, "2024-01-01", "2024-06-01", null);

        doReturn(Future.succeededFuture(delResp)).when(service).restoreMerchantDocument(1L);

        Future<ApiResponseMerchantDocumentDeleteAt> result = handler.restore(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Document restored successfully");
        assertThat(result.result().getData().hasDeletedAt()).isFalse();
        verify(service).restoreMerchantDocument(1L);
    }

    @Test
    void deletePermanent_shouldCallServiceAndReturnSuccess() {
        FindMerchantDocumentByIdRequest request = FindMerchantDocumentByIdRequest.newBuilder()
                .setDocumentId(1)
                .build();

        doReturn(Future.succeededFuture(true)).when(service).deleteMerchantDocumentPermanent(1L);

        Future<ApiResponseMerchantDocumentDelete> result = handler.deletePermanent(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Document permanently deleted");
        verify(service).deleteMerchantDocumentPermanent(1L);
    }

    @Test
    void restoreAll_shouldCallServiceAndReturnSuccess() {
        doReturn(Future.succeededFuture((Void) null)).when(service).restoreAllMerchantDocument();

        Future<ApiResponseMerchantDocumentAll> result = handler.restoreAll(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("All documents restored successfully");
        verify(service).restoreAllMerchantDocument();
    }

    @Test
    void deleteAllPermanent_shouldCallServiceAndReturnSuccess() {
        doReturn(Future.succeededFuture((Void) null)).when(service).deleteAllMerchantDocumentPermanent();

        Future<ApiResponseMerchantDocumentAll> result = handler.deleteAllPermanent(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("All documents permanently deleted successfully");
        verify(service).deleteAllMerchantDocumentPermanent();
    }
}
