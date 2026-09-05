package io.example.product.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;

import io.example.product.domain.requests.CreateProductRequest;
import io.example.product.domain.requests.UpdateProductRequest;
import io.example.product.domain.response.ProductResponse;
import io.example.product.domain.response.ProductResponseDeleteAt;
import io.example.product.service.ProductCommandService;
import io.vertx.core.Future;

import pb.product.Product.ApiResponseProduct;
import pb.product.Product.ApiResponseProductDeleteAt;
import pb.product.Product.FindByIdProductRequest;
import pb.product.ProductCommand.ApiResponseProductAll;
import pb.product.ProductCommand.ApiResponseProductDelete;

@ExtendWith(MockitoExtension.class)
class ProductCommandHandlerTest {

    @Mock private ProductCommandService commandService;

    private ProductCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new ProductCommandHandler(commandService);
    }

    @Test
    void create_shouldCallServiceAndReturnSuccess() {
        pb.product.ProductCommand.CreateProductRequest request =
                pb.product.ProductCommand.CreateProductRequest.newBuilder()
                        .setMerchantId(1)
                        .setCategoryId(1)
                        .setName("New Product")
                        .setDescription("Desc")
                        .setPrice(50000)
                        .setCountInStock(10)
                        .setBrand("Brand")
                        .setWeight(500)
                        .setImageProduct("test.jpg")
                        .build();

        ProductResponse responseDto = ProductResponse.builder()
                .id(1L)
                .merchantId(1)
                .categoryId(1)
                .name("New Product")
                .price(50000)
                .countInStock(10)
                .build();

        when(commandService.create(any(CreateProductRequest.class)))
                .thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseProduct> result = commandHandler.create(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getName()).isEqualTo("New Product");
        verify(commandService).create(any(CreateProductRequest.class));
    }

    @Test
    void update_shouldCallServiceAndReturnSuccess() {
        pb.product.ProductCommand.UpdateProductRequest request =
                pb.product.ProductCommand.UpdateProductRequest.newBuilder()
                        .setProductId(1)
                        .setMerchantId(1)
                        .setCategoryId(1)
                        .setName("Updated Product")
                        .setDescription("Updated Desc")
                        .setPrice(60000)
                        .setCountInStock(20)
                        .setBrand("UpdatedBrand")
                        .setWeight(600)
                        .setImageProduct("updated.jpg")
                        .build();

        ProductResponse responseDto = ProductResponse.builder()
                .id(1L)
                .name("Updated Product")
                .price(60000)
                .countInStock(20)
                .build();

        when(commandService.update(any(UpdateProductRequest.class)))
                .thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseProduct> result = commandHandler.update(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getName()).isEqualTo("Updated Product");
        verify(commandService).update(any(UpdateProductRequest.class));
    }

    @Test
    void incrementStock_shouldCallServiceAndReturnSuccess() {
        pb.product.ProductCommand.IncrementStockRequest request =
                pb.product.ProductCommand.IncrementStockRequest.newBuilder()
                        .setProductId(1)
                        .setQuantity(5)
                        .build();

        ProductResponse responseDto = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .price(50000)
                .countInStock(15)
                .build();

        when(commandService.incrementStock(1L, 5)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseProduct> result = commandHandler.incrementStock(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getCountInStock()).isEqualTo(15);
        verify(commandService).incrementStock(1L, 5);
    }

    @Test
    void trashedProduct_shouldCallServiceAndReturnSuccess() {
        FindByIdProductRequest request = FindByIdProductRequest.newBuilder()
                .setId(1)
                .build();

        ProductResponseDeleteAt responseDto = ProductResponseDeleteAt.builder()
                .id(1L)
                .name("Trashed Product")
                .deletedAt("2024-01-02")
                .build();

        when(commandService.trash(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseProductDeleteAt> result = commandHandler.trashedProduct(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getDeletedAt().getValue()).isEqualTo("2024-01-02");
        verify(commandService).trash(1L);
    }

    @Test
    void restoreProduct_shouldCallServiceAndReturnSuccess() {
        FindByIdProductRequest request = FindByIdProductRequest.newBuilder()
                .setId(1)
                .build();

        ProductResponseDeleteAt responseDto = ProductResponseDeleteAt.builder()
                .id(1L)
                .name("Restored Product")
                .deletedAt(null)
                .build();

        when(commandService.restore(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseProductDeleteAt> result = commandHandler.restoreProduct(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().hasDeletedAt()).isFalse();
        verify(commandService).restore(1L);
    }

    @Test
    void deleteProductPermanent_shouldCallServiceAndReturnSuccess() {
        FindByIdProductRequest request = FindByIdProductRequest.newBuilder()
                .setId(1)
                .build();

        when(commandService.deletePermanent(1L)).thenReturn(Future.succeededFuture(true));

        Future<ApiResponseProductDelete> result = commandHandler.deleteProductPermanent(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).deletePermanent(1L);
    }

    @Test
    void restoreAllProduct_shouldCallServiceAndReturnSuccess() {
        when(commandService.restoreAll()).thenReturn(Future.succeededFuture(true));

        Future<ApiResponseProductAll> result = commandHandler.restoreAllProduct(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).restoreAll();
    }

    @Test
    void deleteAllProductPermanent_shouldCallServiceAndReturnSuccess() {
        when(commandService.deleteAllPermanent()).thenReturn(Future.succeededFuture(true));

        Future<ApiResponseProductAll> result = commandHandler.deleteAllProductPermanent(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        verify(commandService).deleteAllPermanent();
    }
}
