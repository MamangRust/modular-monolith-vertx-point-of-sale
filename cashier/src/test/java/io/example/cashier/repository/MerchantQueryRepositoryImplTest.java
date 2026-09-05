package io.example.cashier.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.cashier.repository.impl.MerchantQueryRepositoryImpl;
import io.vertx.core.Future;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.VertxMerchantQueryServiceGrpcClient;

@ExtendWith(MockitoExtension.class)
class MerchantQueryRepositoryImplTest {

    @Mock
    private VertxMerchantQueryServiceGrpcClient merchantQueryClient;

    private MerchantQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new MerchantQueryRepositoryImpl(merchantQueryClient);
    }

    @Test
    void existsById_shouldReturnFalseWhenIdIsNull() {
        Future<Boolean> result = repository.existsById(null);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void existsById_shouldReturnTrueWhenMerchantExists() {
        pb.merchant.Merchant.ApiResponseMerchant response = pb.merchant.Merchant.ApiResponseMerchant.newBuilder()
                .setData(pb.merchant.Merchant.MerchantResponse.newBuilder().setId(1).build())
                .build();

        when(merchantQueryClient.findByIdMerchant(any(FindByIdMerchantRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.existsById(1);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void existsById_shouldReturnFalseWhenMerchantDoesNotExist() {
        pb.merchant.Merchant.ApiResponseMerchant response = pb.merchant.Merchant.ApiResponseMerchant.newBuilder().build();

        when(merchantQueryClient.findByIdMerchant(any(FindByIdMerchantRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.existsById(2);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void existsById_shouldReturnFalseOnGrpcError() {
        when(merchantQueryClient.findByIdMerchant(any(FindByIdMerchantRequest.class)))
                .thenReturn(Future.failedFuture(new RuntimeException("gRPC Connection Error")));

        Future<Boolean> result = repository.existsById(3);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }
}
