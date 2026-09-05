package io.example.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.product.domain.requests.FindAllProducts;
import io.example.product.domain.response.ProductResponse;
import io.example.product.domain.response.ProductResponseDeleteAt;
import io.example.product.model.Product;
import io.example.product.repository.ProductQueryRepository;
import io.example.product.service.impl.ProductQueryServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceImplTest {

    @Mock private ProductQueryRepository queryRepo;
    @Mock private RedisService redis;
    @Mock private TracingMetrics metrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private ProductQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        lenient().when(metrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redis.setJson(anyString(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

        queryService = new ProductQueryServiceImpl(queryRepo, redis, metrics);
    }

    private Product createProduct() {
        return Product.builder()
                .productId(1L)
                .merchantId(1L)
                .categoryId(1L)
                .name("Test Product")
                .description("Desc")
                .price(50000)
                .countInStock(10)
                .brand("Brand")
                .weight(500)
                .slugProduct("test-product")
                .imageProduct("test.jpg")
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .build();
    }

    // --- getAll ---

    @Test
    void getAll_shouldFetchFromDbWhenCacheMiss() {
        FindAllProducts req = FindAllProducts.builder().search("test").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getProducts(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createProduct()), 1)));

        Future<PagedResult<ProductResponse>> result = queryService.getAll(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getId()).isEqualTo(1L);
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Test Product");
        verify(queryRepo).getProducts(req);
    }

    @Test
    void getAll_shouldReturnFromCacheWhenCacheHit() {
        FindAllProducts req = FindAllProducts.builder().search("test").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"productId\":1,\"merchantId\":1,\"categoryId\":1,\"name\":\"Test Product\",\"price\":50000,\"countInStock\":10,\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<ProductResponse>> result = queryService.getAll(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo, never()).getProducts(any());
    }

    // --- getActive ---

    @Test
    void getActive_shouldFetchFromDbWhenCacheMiss() {
        FindAllProducts req = FindAllProducts.builder().search("").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getProductsActive(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createProduct()), 1)));

        Future<PagedResult<ProductResponseDeleteAt>> result = queryService.getActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).getProductsActive(req);
    }

    @Test
    void getActive_shouldReturnFromCacheWhenCacheHit() {
        FindAllProducts req = FindAllProducts.builder().search("").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"productId\":1,\"merchantId\":1,\"categoryId\":1,\"name\":\"Test Product\",\"price\":50000,\"countInStock\":10,\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<ProductResponseDeleteAt>> result = queryService.getActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo, never()).getProductsActive(any());
    }

    // --- getTrashed ---

    @Test
    void getTrashed_shouldFetchFromDbWhenCacheMiss() {
        FindAllProducts req = FindAllProducts.builder().search("").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getProductsTrashed(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createProduct()), 1)));

        Future<PagedResult<ProductResponseDeleteAt>> result = queryService.getTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).getProductsTrashed(req);
    }

    @Test
    void getTrashed_shouldReturnFromCacheWhenCacheHit() {
        FindAllProducts req = FindAllProducts.builder().search("").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"productId\":1,\"merchantId\":1,\"categoryId\":1,\"name\":\"Test Product\",\"price\":50000,\"countInStock\":10,\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<ProductResponseDeleteAt>> result = queryService.getTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo, never()).getProductsTrashed(any());
    }

    // --- getById ---

    @Test
    void getById_shouldFetchFromDbWhenCacheMiss() {
        Long id = 1L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getProductById(id)).thenReturn(Future.succeededFuture(createProduct()));

        Future<ProductResponse> result = queryService.getById(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Test Product");
        verify(queryRepo).getProductById(id);
    }

    @Test
    void getById_shouldReturnFromCacheWhenCacheHit() {
        Long id = 1L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(createProduct()));

        Future<ProductResponse> result = queryService.getById(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Test Product");
        verify(queryRepo, never()).getProductById(anyLong());
    }

    @Test
    void getById_shouldFailWhenNotFound() {
        Long id = 999L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getProductById(id)).thenReturn(Future.succeededFuture(null));

        Future<ProductResponse> result = queryService.getById(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }
}
