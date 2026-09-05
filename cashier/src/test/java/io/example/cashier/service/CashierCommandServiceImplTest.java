package io.example.cashier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.cashier.domain.requests.cashier.CreateCashierRequest;
import io.example.cashier.domain.requests.cashier.UpdateCashierRequest;
import io.example.cashier.domain.response.cashier.CashierResponse;
import io.example.cashier.domain.response.cashier.CashierResponseDeleteAt;
import io.example.cashier.model.Cashier;
import io.example.cashier.repository.CashierCommandRepository;
import io.example.cashier.repository.CashierQueryRepository;
import io.example.cashier.repository.MerchantQueryRepository;
import io.example.cashier.repository.UserQueryRepository;
import io.example.cashier.service.impl.CashierCommandServiceImpl;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class CashierCommandServiceImplTest {

    @Mock private CashierCommandRepository commandRepository;
    @Mock private CashierQueryRepository queryRepository;
    @Mock private MerchantQueryRepository merchantQueryRepository;
    @Mock private UserQueryRepository userQueryRepository;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private CashierCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        commandService = new CashierCommandServiceImpl(
                commandRepository,
                queryRepository,
                merchantQueryRepository,
                userQueryRepository,
                redisService,
                tracingMetrics
        );
    }

    private Cashier createCashier() {
        return Cashier.builder()
                .cashierId(1L)
                .merchantId(2L)
                .userId(3L)
                .name("Cashier Test")
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .build();
    }

    // --- createCashier ---

    @Test
    void createCashier_shouldCreateSuccessfully() {
        CreateCashierRequest req = CreateCashierRequest.builder()
                .merchantId(2)
                .userId(3)
                .name("New Cashier")
                .build();

        Cashier created = createCashier();
        created.setName("New Cashier");

        when(queryRepository.findByName("New Cashier")).thenReturn(Future.succeededFuture(null));
        when(merchantQueryRepository.existsById(2)).thenReturn(Future.succeededFuture(true));
        when(userQueryRepository.existsById(3)).thenReturn(Future.succeededFuture(true));
        when(commandRepository.createCashier(req)).thenReturn(Future.succeededFuture(created));

        Future<CashierResponse> result = commandService.createCashier(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("New Cashier");
        verify(redisService).deleteByPattern("cashier:list:*");
    }

    @Test
    void createCashier_shouldFailWhenNameExists() {
        CreateCashierRequest req = CreateCashierRequest.builder()
                .merchantId(2)
                .userId(3)
                .name("Existing Cashier")
                .build();

        when(queryRepository.findByName("Existing Cashier")).thenReturn(Future.succeededFuture(createCashier()));

        Future<CashierResponse> result = commandService.createCashier(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("already exists");
        verify(commandRepository, never()).createCashier(any());
    }

    @Test
    void createCashier_shouldFailWhenMerchantNotFound() {
        CreateCashierRequest req = CreateCashierRequest.builder()
                .merchantId(2)
                .userId(3)
                .name("New Cashier")
                .build();

        when(queryRepository.findByName("New Cashier")).thenReturn(Future.succeededFuture(null));
        when(merchantQueryRepository.existsById(2)).thenReturn(Future.succeededFuture(false));

        Future<CashierResponse> result = commandService.createCashier(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).contains("Merchant not found");
        verify(commandRepository, never()).createCashier(any());
    }

    @Test
    void createCashier_shouldFailWhenUserNotFound() {
        CreateCashierRequest req = CreateCashierRequest.builder()
                .merchantId(2)
                .userId(3)
                .name("New Cashier")
                .build();

        when(queryRepository.findByName("New Cashier")).thenReturn(Future.succeededFuture(null));
        when(merchantQueryRepository.existsById(2)).thenReturn(Future.succeededFuture(true));
        when(userQueryRepository.existsById(3)).thenReturn(Future.succeededFuture(false));

        Future<CashierResponse> result = commandService.createCashier(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).contains("User not found");
        verify(commandRepository, never()).createCashier(any());
    }

    // --- updateCashier ---

    @Test
    void updateCashier_shouldUpdateSuccessfully() {
        UpdateCashierRequest req = UpdateCashierRequest.builder()
                .cashierId(1)
                .name("Updated Cashier")
                .build();

        Cashier existing = createCashier();
        Cashier updated = createCashier();
        updated.setName("Updated Cashier");

        when(queryRepository.findById(1L)).thenReturn(Future.succeededFuture(existing));
        when(queryRepository.findByName("Updated Cashier")).thenReturn(Future.succeededFuture(null));
        when(commandRepository.updateCashier(req)).thenReturn(Future.succeededFuture(updated));

        Future<CashierResponse> result = commandService.updateCashier(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Updated Cashier");
        verify(redisService).delete("cashier:1");
        verify(redisService).deleteByPattern("cashier:list:*");
    }

    @Test
    void updateCashier_shouldFailWhenCashierNotFound() {
        UpdateCashierRequest req = UpdateCashierRequest.builder()
                .cashierId(99)
                .name("Updated Cashier")
                .build();

        when(queryRepository.findById(99L)).thenReturn(Future.succeededFuture(null));

        Future<CashierResponse> result = commandService.updateCashier(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        verify(commandRepository, never()).updateCashier(any());
    }

    @Test
    void updateCashier_shouldFailWhenNameUsedByAnother() {
        UpdateCashierRequest req = UpdateCashierRequest.builder()
                .cashierId(1)
                .name("Other Name")
                .build();

        Cashier existing = createCashier();
        Cashier another = createCashier();
        another.setCashierId(2L);

        when(queryRepository.findById(1L)).thenReturn(Future.succeededFuture(existing));
        when(queryRepository.findByName("Other Name")).thenReturn(Future.succeededFuture(another));

        Future<CashierResponse> result = commandService.updateCashier(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        verify(commandRepository, never()).updateCashier(any());
    }

    // --- trashCashier ---

    @Test
    void trashCashier_shouldTrashSuccessfully() {
        Long id = 1L;
        Cashier trashed = createCashier();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(commandRepository.trashCashier(id)).thenReturn(Future.succeededFuture(trashed));

        Future<CashierResponseDeleteAt> result = commandService.trashCashier(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
        verify(redisService).delete("cashier:1");
    }

    @Test
    void trashCashier_shouldFailWhenNotFoundOrAlreadyTrashed() {
        Long id = 99L;
        when(commandRepository.trashCashier(id)).thenReturn(Future.succeededFuture(null));

        Future<CashierResponseDeleteAt> result = commandService.trashCashier(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- restoreCashier ---

    @Test
    void restoreCashier_shouldRestoreSuccessfully() {
        Long id = 1L;
        Cashier trashed = createCashier();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
        Cashier restored = createCashier();

        when(queryRepository.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepository.restoreCashier(id)).thenReturn(Future.succeededFuture(restored));

        Future<CashierResponseDeleteAt> result = commandService.restoreCashier(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNull();
        verify(redisService).delete("cashier:1");
    }

    @Test
    void restoreCashier_shouldFailWhenNotTrashed() {
        Long id = 1L;
        when(queryRepository.findByTrashedId(id)).thenReturn(Future.succeededFuture(null));

        Future<CashierResponseDeleteAt> result = commandService.restoreCashier(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        verify(commandRepository, never()).restoreCashier(anyLong());
    }

    // --- deleteCashierPermanent ---

    @Test
    void deleteCashierPermanent_shouldDeleteSuccessfully() {
        Long id = 1L;
        Cashier trashed = createCashier();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(queryRepository.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepository.deleteCashierPermanent(id)).thenReturn(Future.succeededFuture(true));

        Future<Void> result = commandService.deleteCashierPermanent(id);

        assertThat(result.succeeded()).isTrue();
        verify(redisService).delete("cashier:1");
    }

    @Test
    void deleteCashierPermanent_shouldFailWhenNotTrashed() {
        Long id = 1L;
        when(queryRepository.findByTrashedId(id)).thenReturn(Future.succeededFuture(null));

        Future<Void> result = commandService.deleteCashierPermanent(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
    }

    // --- restoreAllCashier ---

    @Test
    void restoreAllCashier_shouldRestoreAll() {
        when(commandRepository.restoreAllCashier()).thenReturn(Future.succeededFuture(5));

        Future<Void> result = commandService.restoreAllCashier();

        assertThat(result.succeeded()).isTrue();
        verify(redisService).deleteByPattern("cashier:list:*");
    }

    @Test
    void restoreAllCashier_shouldFailWhenNoneTrashed() {
        when(commandRepository.restoreAllCashier()).thenReturn(Future.succeededFuture(0));

        Future<Void> result = commandService.restoreAllCashier();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- deleteAllCashierPermanent ---

    @Test
    void deleteAllCashierPermanent_shouldDeleteAll() {
        when(commandRepository.deleteAllCashierPermanent()).thenReturn(Future.succeededFuture(3));

        Future<Void> result = commandService.deleteAllCashierPermanent();

        assertThat(result.succeeded()).isTrue();
        verify(redisService).deleteByPattern("cashier:list:*");
    }

    @Test
    void deleteAllCashierPermanent_shouldFailWhenNoneTrashed() {
        when(commandRepository.deleteAllCashierPermanent()).thenReturn(Future.succeededFuture(0));

        Future<Void> result = commandService.deleteAllCashierPermanent();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }
}
