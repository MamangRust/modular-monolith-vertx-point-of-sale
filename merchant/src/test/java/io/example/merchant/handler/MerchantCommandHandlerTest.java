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

import io.example.merchant.domain.requests.CreateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantRequest;
import io.example.merchant.domain.requests.UpdateMerchantStatusRequest;
import io.example.merchant.domain.response.MerchantResponse;
import io.example.merchant.domain.response.MerchantResponseDeleteAt;
import io.example.merchant.service.MerchantCommandService;
import io.vertx.core.Future;

import pb.merchant.Merchant.ApiResponseMerchant;
import pb.merchant.Merchant.ApiResponseMerchantDeleteAt;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.MerchantCommand.ApiResponseMerchantAll;
import pb.merchant.MerchantCommand.ApiResponseMerchantDelete;

@ExtendWith(MockitoExtension.class)
class MerchantCommandHandlerTest {

    @Mock
    private MerchantCommandService service;

    private MerchantCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MerchantCommandHandler(service);
    }

    @Test
    void createMerchant_shouldCallServiceAndReturnSuccess() {
        pb.merchant.MerchantCommand.CreateMerchantRequest request = pb.merchant.MerchantCommand.CreateMerchantRequest
                .newBuilder()
                .setName("Merchant A")
                .setUserId(1)
                .build();

        MerchantResponse resp = new MerchantResponse(1L, 1, "Merchant A", "Desc", "Addr", "e@m.com", "0812", "ACTIVE",
                "2024-01-01", "2024-06-01");

        doReturn(Future.succeededFuture(resp)).when(service).createMerchant(any(CreateMerchantRequest.class));

        Future<ApiResponseMerchant> result = handler.createMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Merchant created successfully");
        assertThat(result.result().getData().getName()).isEqualTo("Merchant A");
        verify(service).createMerchant(any(CreateMerchantRequest.class));
    }

    @Test
    void updateMerchant_shouldCallServiceAndReturnSuccess() {
        pb.merchant.MerchantCommand.UpdateMerchantRequest request = pb.merchant.MerchantCommand.UpdateMerchantRequest
                .newBuilder()
                .setMerchantId(1)
                .setName("Updated Merchant")
                .setUserId(1)
                .setStatus("ACTIVE")
                .build();

        MerchantResponse resp = new MerchantResponse(1L, 1, "Updated Merchant", "Desc", "Addr", "e@m.com", "0812",
                "ACTIVE", "2024-01-01", "2024-06-01");

        doReturn(Future.succeededFuture(resp)).when(service).updateMerchant(any(UpdateMerchantRequest.class));

        Future<ApiResponseMerchant> result = handler.updateMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Merchant updated successfully");
        assertThat(result.result().getData().getName()).isEqualTo("Updated Merchant");
        verify(service).updateMerchant(any(UpdateMerchantRequest.class));
    }

    @Test
    void updateMerchantStatus_shouldCallServiceAndReturnSuccess() {
        pb.merchant.MerchantCommand.UpdateMerchantStatusRequest request = pb.merchant.MerchantCommand.UpdateMerchantStatusRequest
                .newBuilder()
                .setMerchantId(1)
                .setStatus("ACTIVE")
                .build();

        MerchantResponse resp = new MerchantResponse(1L, 1, "Merchant A", "Desc", "Addr", "e@m.com", "0812", "ACTIVE",
                "2024-01-01", "2024-06-01");

        doReturn(Future.succeededFuture(resp)).when(service)
                .updateMerchantStatus(any(UpdateMerchantStatusRequest.class));

        Future<ApiResponseMerchant> result = handler.updateMerchantStatus(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Merchant status updated successfully");
        verify(service).updateMerchantStatus(any(UpdateMerchantStatusRequest.class));
    }

    @Test
    void trashedMerchant_shouldCallServiceAndReturnSuccess() {
        FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                .setMerchantId(1)
                .build();

        MerchantResponseDeleteAt delResp = new MerchantResponseDeleteAt(1L, 1, "Merchant A", "Desc", "Addr", "e@m.com",
                "0812", "ACTIVE", "2024-01-01", "2024-06-01", "2024-06-02");

        doReturn(Future.succeededFuture(delResp)).when(service).trashedMerchant(1L);

        Future<ApiResponseMerchantDeleteAt> result = handler.trashedMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Merchant trashed successfully");
        assertThat(result.result().getData().getDeletedAt().getValue()).isEqualTo("2024-06-02");
        verify(service).trashedMerchant(1L);
    }

    @Test
    void restoreMerchant_shouldCallServiceAndReturnSuccess() {
        FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                .setMerchantId(1)
                .build();

        MerchantResponseDeleteAt delResp = new MerchantResponseDeleteAt(1L, 1, "Merchant A", "Desc", "Addr", "e@m.com",
                "0812", "ACTIVE", "2024-01-01", "2024-06-01", null);

        doReturn(Future.succeededFuture(delResp)).when(service).restoreMerchant(1L);

        Future<ApiResponseMerchantDeleteAt> result = handler.restoreMerchant(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Merchant restored successfully");
        assertThat(result.result().getData().hasDeletedAt()).isFalse();
        verify(service).restoreMerchant(1L);
    }

    @Test
    void deleteMerchantPermanent_shouldCallServiceAndReturnSuccess() {
        FindByIdMerchantRequest request = FindByIdMerchantRequest.newBuilder()
                .setMerchantId(1)
                .build();

        doReturn(Future.succeededFuture((Void) null)).when(service).deleteMerchantPermanent(1L);

        Future<ApiResponseMerchantDelete> result = handler.deleteMerchantPermanent(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Merchant deleted successfully");
        verify(service).deleteMerchantPermanent(1L);
    }

    @Test
    void restoreAllMerchant_shouldCallServiceAndReturnSuccess() {
        doReturn(Future.succeededFuture((Void) null)).when(service).restoreAllMerchant();

        Future<ApiResponseMerchantAll> result = handler.restoreAllMerchant(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("All merchants restored successfully");
        verify(service).restoreAllMerchant();
    }

    @Test
    void deleteAllMerchantPermanent_shouldCallServiceAndReturnSuccess() {
        doReturn(Future.succeededFuture((Void) null)).when(service).deleteAllMerchantPermanent();

        Future<ApiResponseMerchantAll> result = handler.deleteAllMerchantPermanent(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("All merchants permanently deleted successfully");
        verify(service).deleteAllMerchantPermanent();
    }
}
