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
import io.example.order.repository.impl.ProductCommandRepositoryImpl;
import io.vertx.core.Future;
import pb.product.VertxProductCommandServiceGrpcClient;
import pb.product.Product.ApiResponseProduct;
import pb.product.Product.ProductResponse;
import pb.product.ProductCommand.DecrementStockRequest;
import pb.product.ProductCommand.IncrementStockRequest;

@ExtendWith(MockitoExtension.class)
class ProductCommandRepositoryImplTest {

    @Mock
    private VertxProductCommandServiceGrpcClient commandClient;

    private ProductCommandRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ProductCommandRepositoryImpl(commandClient);
    }

    @Test
    void decrementStock_shouldReturnUpdatedProduct() {
        ProductResponse updatedProto = ProductResponse.newBuilder()
                .setId(1)
                .setName("Test Product")
                .setPrice(50000)
                .setCountInStock(7)
                .build();
        ApiResponseProduct updateResponse = ApiResponseProduct.newBuilder()
                .setData(updatedProto)
                .build();

        when(commandClient.decrementStock(any(DecrementStockRequest.class)))
                .thenReturn(Future.succeededFuture(updateResponse));

        Future<Product> result = repository.decrementStock(1L, 3);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getProductId()).isEqualTo(1L);
        assertThat(result.result().getCountInStock()).isEqualTo(7);
        assertThat(result.result().getName()).isEqualTo("Test Product");
    }

    @Test
    void decrementStock_shouldReturnNullWhenProductIdIsNull() {
        Future<Product> result = repository.decrementStock(null, 3);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void incrementStock_shouldReturnUpdatedProduct() {
        ProductResponse updatedProto = ProductResponse.newBuilder()
                .setId(1)
                .setName("Test Product")
                .setPrice(50000)
                .setCountInStock(10)
                .build();
        ApiResponseProduct updateResponse = ApiResponseProduct.newBuilder()
                .setData(updatedProto)
                .build();

        when(commandClient.incrementStock(any(IncrementStockRequest.class)))
                .thenReturn(Future.succeededFuture(updateResponse));

        Future<Product> result = repository.incrementStock(1L, 3);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getProductId()).isEqualTo(1L);
        assertThat(result.result().getCountInStock()).isEqualTo(10);
    }

    @Test
    void incrementStock_shouldReturnNullWhenProductIdIsNull() {
        Future<Product> result = repository.incrementStock(null, 3);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void decrementStock_shouldPropagateInsufficientStockFailure() {
        when(commandClient.decrementStock(any(DecrementStockRequest.class)))
                .thenReturn(Future.failedFuture(new io.grpc.StatusRuntimeException(
                        io.grpc.Status.FAILED_PRECONDITION.withDescription("Insufficient product stock"))));

        Future<Product> result = repository.decrementStock(1L, 999);

        // The error must NOT be swallowed: insufficient stock rejects the order.
        assertThat(result.failed()).isTrue();
        assertThat(result.cause().getMessage()).contains("Insufficient product stock");
    }
}
