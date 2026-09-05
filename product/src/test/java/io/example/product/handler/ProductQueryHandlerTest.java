package io.example.product.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.domain.PagedResult;
import io.example.product.domain.requests.FindAllProducts;
import io.example.product.domain.response.ProductResponse;
import io.example.product.domain.response.ProductResponseDeleteAt;
import io.example.product.service.ProductQueryService;
import io.vertx.core.Future;

import pb.product.Product.ApiResponseProduct;
import pb.product.Product.FindAllProductRequest;
import pb.product.Product.FindByIdProductRequest;
import pb.product.ProductQuery.ApiResponsePaginationProduct;
import pb.product.ProductQuery.ApiResponsePaginationProductDeleteAt;

@ExtendWith(MockitoExtension.class)
class ProductQueryHandlerTest {

    @Mock private ProductQueryService queryService;

    private ProductQueryHandler queryHandler;

    @BeforeEach
    void setUp() {
        queryHandler = new ProductQueryHandler(queryService);
    }

    @Test
    void findAll_shouldReturnPagedResponse() {
        FindAllProductRequest request = FindAllProductRequest.newBuilder()
                .setSearch("test")
                .setPage(1)
                .setPageSize(10)
                .build();

        ProductResponse responseDto = ProductResponse.builder()
                .id(1L)
                .merchantId(1)
                .categoryId(1)
                .name("Test Product")
                .description("Desc")
                .price(50000)
                .countInStock(10)
                .brand("Brand")
                .createdAt("2024-01-01")
                .updatedAt("2024-01-01")
                .build();

        PagedResult<ProductResponse> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getAll(any(FindAllProducts.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationProduct> result = queryHandler.findAll(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getDataCount()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("Test Product");
    }

    @Test
    void findById_shouldReturnProductResponse() {
        FindByIdProductRequest request = FindByIdProductRequest.newBuilder()
                .setId(1)
                .build();

        ProductResponse responseDto = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .build();

        when(queryService.getById(1L)).thenReturn(Future.succeededFuture(responseDto));

        Future<ApiResponseProduct> result = queryHandler.findById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData().getId()).isEqualTo(1);
    }

    @Test
    void findByActive_shouldReturnPagedDeleteAtResponse() {
        FindAllProductRequest request = FindAllProductRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        ProductResponseDeleteAt responseDto = ProductResponseDeleteAt.builder()
                .id(1L)
                .name("active-product")
                .build();

        PagedResult<ProductResponseDeleteAt> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getActive(any(FindAllProducts.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationProductDeleteAt> result = queryHandler.findByActive(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getName()).isEqualTo("active-product");
    }

    @Test
    void findByTrashed_shouldReturnPagedDeleteAtResponse() {
        FindAllProductRequest request = FindAllProductRequest.newBuilder()
                .setPage(1)
                .setPageSize(10)
                .build();

        ProductResponseDeleteAt responseDto = ProductResponseDeleteAt.builder()
                .id(1L)
                .name("trashed-product")
                .build();

        PagedResult<ProductResponseDeleteAt> paged = new PagedResult<>(List.of(responseDto), 1);
        when(queryService.getTrashed(any(FindAllProducts.class))).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationProductDeleteAt> result = queryHandler.findByTrashed(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getData(0).getName()).isEqualTo("trashed-product");
    }
}
