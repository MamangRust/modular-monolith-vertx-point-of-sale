package io.example.order.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.order.model.Product;
import io.example.order.repository.impl.ProductQueryRepositoryImpl;
import io.vertx.core.Future;
import pb.product.VertxProductServiceGrpcClient;
import pb.product.Product.ApiResponseProduct;
import pb.product.Product.FindByIdProductRequest;
import pb.product.Product.ProductResponse;

@ExtendWith(MockitoExtension.class)
class ProductQueryRepositoryImplTest {

    @Mock
    private VertxProductServiceGrpcClient client;

    private ProductQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ProductQueryRepositoryImpl(client);
    }

    @Test
    void getProductById_shouldReturnProduct() {
        ProductResponse protoProduct = ProductResponse.newBuilder()
                .setId(1)
                .setName("Test Product")
                .setPrice(50000)
                .setCountInStock(10)
                .build();
        ApiResponseProduct response = ApiResponseProduct.newBuilder()
                .setData(protoProduct)
                .build();

        when(client.findById(any(FindByIdProductRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Product> result = repository.getProductById(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getProductId()).isEqualTo(1L);
        assertThat(result.result().getName()).isEqualTo("Test Product");
        assertThat(result.result().getPrice()).isEqualTo(50000);
        assertThat(result.result().getCountInStock()).isEqualTo(10);
    }

    @Test
    void getProductById_shouldReturnNullWhenProductIdIsNull() {
        Future<Product> result = repository.getProductById(null);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void getProductById_shouldReturnNullWhenProductNotFound() {
        ApiResponseProduct response = ApiResponseProduct.newBuilder().build();

        when(client.findById(any(FindByIdProductRequest.class)))
                .thenReturn(Future.succeededFuture(response));

        Future<Product> result = repository.getProductById(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void getProductById_shouldReturnNullOnGrpcError() {
        when(client.findById(any(FindByIdProductRequest.class)))
                .thenReturn(Future.failedFuture(new RuntimeException("gRPC error")));

        Future<Product> result = repository.getProductById(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }
}
