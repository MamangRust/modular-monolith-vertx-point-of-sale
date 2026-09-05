package io.example.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.order.repository.impl.CashierQueryRepositoryImpl;
import io.vertx.core.Future;
import pb.cashier.VertxCashierServiceGrpcClient;
import pb.cashier.Cashier.ApiResponseCashier;
import pb.cashier.Cashier.CashierResponse;
import pb.cashier.Cashier.FindByIdCashierRequest;

@ExtendWith(MockitoExtension.class)
class CashierQueryRepositoryImplTest {

    @Mock
    private VertxCashierServiceGrpcClient client;

    private CashierQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CashierQueryRepositoryImpl(client);
    }

    @Test
    void existsById_shouldReturnTrueWhenCashierExists() {
        ApiResponseCashier response = ApiResponseCashier.newBuilder()
                .setData(CashierResponse.newBuilder().setId(1).build())
                .build();

        when(client.findById(any(FindByIdCashierRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.existsById(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void existsById_shouldReturnFalseWhenIdIsNull() {
        Future<Boolean> result = repository.existsById(null);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void existsById_shouldReturnFalseWhenCashierDoesNotExist() {
        ApiResponseCashier response = ApiResponseCashier.newBuilder().build();

        when(client.findById(any(FindByIdCashierRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.existsById(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void existsById_shouldReturnFalseOnGrpcError() {
        when(client.findById(any(FindByIdCashierRequest.class)))
                .thenReturn(Future.failedFuture(new RuntimeException("gRPC error")));

        Future<Boolean> result = repository.existsById(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }
}
