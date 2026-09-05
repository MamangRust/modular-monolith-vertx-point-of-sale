package io.example.cashier.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.cashier.repository.impl.UserQueryRepositoryImpl;
import io.vertx.core.Future;
import pb.user.User.FindByIdUserRequest;
import pb.user.VertxUserQueryServiceGrpcClient;

@ExtendWith(MockitoExtension.class)
class UserQueryRepositoryImplTest {

    @Mock
    private VertxUserQueryServiceGrpcClient userQueryClient;

    private UserQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new UserQueryRepositoryImpl(userQueryClient);
    }

    @Test
    void existsById_shouldReturnFalseWhenIdIsNull() {
        Future<Boolean> result = repository.existsById(null);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void existsById_shouldReturnTrueWhenUserExists() {
        pb.user.User.ApiResponseUser response = pb.user.User.ApiResponseUser.newBuilder()
                .setData(pb.user.User.UserResponse.newBuilder().setId(1).build())
                .build();

        when(userQueryClient.findById(any(FindByIdUserRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.existsById(1);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void existsById_shouldReturnFalseWhenUserDoesNotExist() {
        pb.user.User.ApiResponseUser response = pb.user.User.ApiResponseUser.newBuilder().build();

        when(userQueryClient.findById(any(FindByIdUserRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.existsById(2);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void existsById_shouldReturnFalseOnGrpcError() {
        when(userQueryClient.findById(any(FindByIdUserRequest.class)))
                .thenReturn(Future.failedFuture(new RuntimeException("gRPC Connection Error")));

        Future<Boolean> result = repository.existsById(3);
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }
}
