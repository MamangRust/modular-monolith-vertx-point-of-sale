package io.example.product.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.product.repository.impl.CategoryQueryRepositoryImpl;
import io.vertx.core.Future;
import pb.category.Category.ApiResponseCategory;
import pb.category.Category.CategoryResponse;
import pb.category.Category.FindByIdCategoryRequest;
import pb.category.VertxCategoryServiceGrpcClient;

@ExtendWith(MockitoExtension.class)
class CategoryQueryRepositoryImplTest {

    @Mock
    private VertxCategoryServiceGrpcClient client;

    private CategoryQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CategoryQueryRepositoryImpl(client);
    }

    @Test
    void existsById_shouldReturnTrueWhenCategoryExists() {
        ApiResponseCategory response = ApiResponseCategory.newBuilder()
                .setData(CategoryResponse.newBuilder().setId(1).build())
                .build();

        when(client.findById(any(FindByIdCategoryRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.existsById(1);

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
    void existsById_shouldReturnFalseWhenIdIsZeroOrNegative() {
        Future<Boolean> result = repository.existsById(0);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void existsById_shouldReturnFalseWhenCategoryDoesNotExist() {
        ApiResponseCategory response = ApiResponseCategory.newBuilder().build();

        when(client.findById(any(FindByIdCategoryRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Boolean> result = repository.existsById(999);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void existsById_shouldReturnFalseOnGrpcError() {
        when(client.findById(any(FindByIdCategoryRequest.class)))
                .thenReturn(Future.failedFuture(new RuntimeException("gRPC error")));

        Future<Boolean> result = repository.existsById(1);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }
}
