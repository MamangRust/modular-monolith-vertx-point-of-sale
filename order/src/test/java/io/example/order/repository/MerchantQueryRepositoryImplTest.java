package io.example.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.order.repository.impl.MerchantQueryRepositoryImpl;
import io.vertx.core.Future;
import pb.merchant.Merchant.ApiResponseMerchant;
import pb.merchant.Merchant.FindByIdMerchantRequest;
import pb.merchant.Merchant.MerchantResponse;
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
    void existsById_shouldReturnTrueWhenMerchantExists() {
        ApiResponseMerchant response = ApiResponseMerchant.newBuilder()
                .setData(MerchantResponse.newBuilder().setId(1).build())
                .build();

        when(merchantQueryClient.findByIdMerchant(any(FindByIdMerchantRequest.class)))
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
    void existsById_shouldReturnFalseWhenMerchantDoesNotExist() {
        ApiResponseMerchant response = ApiResponseMerchant.newBuilder().build();

        when(merchantQueryClient.findByIdMerchant(any(FindByIdMerchantRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.existsById(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void existsById_shouldReturnFalseOnGrpcError() {
        when(merchantQueryClient.findByIdMerchant(any(FindByIdMerchantRequest.class)))
                .thenReturn(Future.failedFuture(new RuntimeException("gRPC error")));

        Future<Boolean> result = repository.existsById(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }
}
