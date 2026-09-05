package io.example.cashier.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;

import io.example.cashier.domain.requests.cashier.CreateCashierRequest;
import io.example.cashier.domain.requests.cashier.UpdateCashierRequest;
import io.example.cashier.domain.response.cashier.CashierResponse;
import io.example.cashier.domain.response.cashier.CashierResponseDeleteAt;
import io.example.cashier.service.CashierCommandService;
import io.vertx.core.Future;

import pb.cashier.Cashier.ApiResponseCashier;
import pb.cashier.Cashier.ApiResponseCashierDeleteAt;
import pb.cashier.Cashier.FindByIdCashierRequest;
import pb.cashier.CashierCommand.ApiResponseCashierAll;
import pb.cashier.CashierCommand.ApiResponseCashierDelete;

@ExtendWith(MockitoExtension.class)
class CashierCommandHandlerTest {

    @Mock private CashierCommandService commandService;

    private CashierCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new CashierCommandHandler(commandService);
    }

    @Test
    void createCashier_shouldCallServiceAndReturnSuccess() {
        pb.cashier.Cashier.CreateCashierRequest request = pb.cashier.Cashier.CreateCashierRequest.newBuilder()
                .setMerchantId(2)
                .setUserId(3)
                .setName("New Cashier")
                .build();

        CashierResponse responseDto = CashierResponse.builder()
                .id(1)
                .merchantId(2)
                .name("New Cashier")
                .createdAt("2024-01-01")
                .updatedAt("2024-01-01")
                .build();

        when(commandService.createCashier(any(CreateCashierRequest.class)))
                .thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseCashier> result = commandHandler.createCashier(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getName()).isEqualTo("New Cashier");
        verify(commandService).createCashier(any(CreateCashierRequest.class));
    }

    @Test
    void updateCashier_shouldCallServiceAndReturnSuccess() {
        pb.cashier.Cashier.UpdateCashierRequest request = pb.cashier.Cashier.UpdateCashierRequest.newBuilder()
                .setCashierId(1)
                .setName("Updated Name")
                .build();

        CashierResponse responseDto = CashierResponse.builder()
                .id(1)
                .merchantId(2)
                .name("Updated Name")
                .build();

        when(commandService.updateCashier(any(UpdateCashierRequest.class)))
                .thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseCashier> result = commandHandler.updateCashier(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getName()).isEqualTo("Updated Name");
        verify(commandService).updateCashier(any(UpdateCashierRequest.class));
    }

    @Test
    void trashedCashier_shouldCallServiceAndReturnSuccess() {
        FindByIdCashierRequest request = FindByIdCashierRequest.newBuilder()
                .setId(1)
                .build();

        CashierResponseDeleteAt responseDto = CashierResponseDeleteAt.builder()
                .id(1)
                .name("Trashed Cashier")
                .deletedAt("2024-01-02")
                .build();

        when(commandService.trashCashier(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseCashierDeleteAt> result = commandHandler.trashedCashier(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getDeletedAt().getValue()).isEqualTo("2024-01-02");
        verify(commandService).trashCashier(1L);
    }

    @Test
    void restoreCashier_shouldCallServiceAndReturnSuccess() {
        FindByIdCashierRequest request = FindByIdCashierRequest.newBuilder()
                .setId(1)
                .build();

        CashierResponseDeleteAt responseDto = CashierResponseDeleteAt.builder()
                .id(1)
                .name("Restored Cashier")
                .deletedAt(null)
                .build();

        when(commandService.restoreCashier(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseCashierDeleteAt> result = commandHandler.restoreCashier(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().hasDeletedAt()).isFalse();
        verify(commandService).restoreCashier(1L);
    }

    @Test
    void deleteCashierPermanent_shouldCallServiceAndReturnSuccess() {
        FindByIdCashierRequest request = FindByIdCashierRequest.newBuilder()
                .setId(1)
                .build();

        when(commandService.deleteCashierPermanent(1L)).thenReturn(Future.succeededFuture());

        Future<ApiResponseCashierDelete> result = commandHandler.deleteCashierPermanent(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).deleteCashierPermanent(1L);
    }

    @Test
    void restoreAllCashier_shouldCallServiceAndReturnSuccess() {
        when(commandService.restoreAllCashier()).thenReturn(Future.succeededFuture());

        Future<ApiResponseCashierAll> result = commandHandler.restoreAllCashier(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).restoreAllCashier();
    }

    @Test
    void deleteAllCashierPermanent_shouldCallServiceAndReturnSuccess() {
        when(commandService.deleteAllCashierPermanent()).thenReturn(Future.succeededFuture());

        Future<ApiResponseCashierAll> result = commandHandler.deleteAllCashierPermanent(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).deleteAllCashierPermanent();
    }
}
