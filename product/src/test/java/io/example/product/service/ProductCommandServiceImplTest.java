package io.example.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.product.domain.requests.CreateProductRequest;
import io.example.product.domain.requests.UpdateProductRequest;
import io.example.product.domain.response.ProductResponse;
import io.example.product.domain.response.ProductResponseDeleteAt;
import io.example.product.model.Product;
import io.example.product.repository.CategoryQueryRepository;
import io.example.product.repository.MerchantQueryRepository;
import io.example.product.repository.ProductCommandRepository;
import io.example.product.repository.ProductQueryRepository;
import io.example.product.service.impl.ProductCommandServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceImplTest {

    @Mock private ProductCommandRepository commandRepo;
    @Mock private ProductQueryRepository queryRepo;
    @Mock private CategoryQueryRepository categoryQueryRepo;
    @Mock private MerchantQueryRepository merchantQueryRepo;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private ProductCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        commandService = new ProductCommandServiceImpl(
                commandRepo, queryRepo, categoryQueryRepo, merchantQueryRepo,
                redisService, tracingMetrics);
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

    // --- create ---

    @Test
    void create_shouldCreateSuccessfully() {
        CreateProductRequest req = CreateProductRequest.builder()
                .merchantId(1)
                .categoryId(1)
                .name("Test Product")
                .description("Desc")
                .price(50000)
                .countInStock(10)
                .brand("Brand")
                .weight(500)
                .build();

        when(merchantQueryRepo.existsById(1)).thenReturn(Future.succeededFuture(true));
        when(categoryQueryRepo.existsById(1)).thenReturn(Future.succeededFuture(true));
        when(commandRepo.createProduct(req)).thenReturn(Future.succeededFuture(createProduct()));

        Future<ProductResponse> result = commandService.create(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        assertThat(result.result().getName()).isEqualTo("Test Product");
        verify(redisService).deleteByPattern("product:list:*");
    }

    @Test
    void create_shouldFailWhenMerchantNotFound() {
        CreateProductRequest req = CreateProductRequest.builder()
                .merchantId(999)
                .categoryId(1)
                .name("Test")
                .build();

        when(merchantQueryRepo.existsById(999)).thenReturn(Future.succeededFuture(false));

        Future<ProductResponse> result = commandService.create(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        verify(commandRepo, never()).createProduct(any());
    }

    @Test
    void create_shouldFailWhenCategoryNotFound() {
        CreateProductRequest req = CreateProductRequest.builder()
                .merchantId(1)
                .categoryId(999)
                .name("Test")
                .build();

        when(merchantQueryRepo.existsById(1)).thenReturn(Future.succeededFuture(true));
        when(categoryQueryRepo.existsById(999)).thenReturn(Future.succeededFuture(false));

        Future<ProductResponse> result = commandService.create(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        verify(commandRepo, never()).createProduct(any());
    }

    // --- update ---

    @Test
    void update_shouldUpdateSuccessfully() {
        UpdateProductRequest req = UpdateProductRequest.builder()
                .productId(1)
                .merchantId(1)
                .categoryId(1)
                .name("Updated")
                .price(60000)
                .countInStock(20)
                .build();

        Product updated = createProduct();
        updated.setName("Updated");
        updated.setPrice(60000);

        when(merchantQueryRepo.existsById(1)).thenReturn(Future.succeededFuture(true));
        when(categoryQueryRepo.existsById(1)).thenReturn(Future.succeededFuture(true));
        when(commandRepo.updateProduct(req)).thenReturn(Future.succeededFuture(updated));

        Future<ProductResponse> result = commandService.update(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Updated");
        assertThat(result.result().getPrice()).isEqualTo(60000);
        verify(redisService).deleteByPattern("product:list:*");
    }

    @Test
    void update_shouldFailWhenMerchantNotFound() {
        UpdateProductRequest req = UpdateProductRequest.builder()
                .productId(1)
                .merchantId(999)
                .build();

        when(merchantQueryRepo.existsById(999)).thenReturn(Future.succeededFuture(false));

        Future<ProductResponse> result = commandService.update(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        verify(commandRepo, never()).updateProduct(any());
    }

    @Test
    void update_shouldFailWhenCategoryNotFound() {
        UpdateProductRequest req = UpdateProductRequest.builder()
                .productId(1)
                .merchantId(1)
                .categoryId(999)
                .build();

        when(merchantQueryRepo.existsById(1)).thenReturn(Future.succeededFuture(true));
        when(categoryQueryRepo.existsById(999)).thenReturn(Future.succeededFuture(false));

        Future<ProductResponse> result = commandService.update(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        verify(commandRepo, never()).updateProduct(any());
    }

    @Test
    void update_shouldFailWhenProductNotFound() {
        UpdateProductRequest req = UpdateProductRequest.builder()
                .productId(999)
                .merchantId(1)
                .categoryId(1)
                .name("Updated")
                .build();

        when(merchantQueryRepo.existsById(1)).thenReturn(Future.succeededFuture(true));
        when(categoryQueryRepo.existsById(1)).thenReturn(Future.succeededFuture(true));
        when(commandRepo.updateProduct(req)).thenReturn(Future.succeededFuture(null));

        Future<ProductResponse> result = commandService.update(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- trash ---

    @Test
    void trash_shouldTrashSuccessfully() {
        Long id = 1L;
        Product trashed = createProduct();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(commandRepo.trashProduct(id)).thenReturn(Future.succeededFuture(trashed));

        Future<ProductResponseDeleteAt> result = commandService.trash(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
        verify(redisService).delete("product:1");
    }

    @Test
    void trash_shouldFailWhenNotFound() {
        Long id = 999L;
        when(commandRepo.trashProduct(id)).thenReturn(Future.succeededFuture(null));

        Future<ProductResponseDeleteAt> result = commandService.trash(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- restore ---

    @Test
    void restore_shouldRestoreSuccessfully() {
        Long id = 1L;
        Product trashed = createProduct();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
        Product restored = createProduct();

        when(queryRepo.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepo.restoreProduct(id)).thenReturn(Future.succeededFuture(restored));

        Future<ProductResponseDeleteAt> result = commandService.restore(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNull();
        verify(redisService).delete("product:1");
    }

    @Test
    void restore_shouldFailWhenNotTrashed() {
        Long id = 1L;
        when(queryRepo.findByTrashedId(id)).thenReturn(Future.succeededFuture(null));

        Future<ProductResponseDeleteAt> result = commandService.restore(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
    }

    // --- deletePermanent ---

    @Test
    void deletePermanent_shouldDeleteSuccessfully() {
        Long id = 1L;
        Product trashed = createProduct();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(queryRepo.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepo.deleteProductPermanently(id)).thenReturn(Future.succeededFuture(true));

        Future<Boolean> result = commandService.deletePermanent(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
        verify(redisService).delete("product:1");
    }

    @Test
    void deletePermanent_shouldFailWhenNotTrashed() {
        Long id = 1L;
        when(queryRepo.findByTrashedId(id)).thenReturn(Future.succeededFuture(null));

        Future<Boolean> result = commandService.deletePermanent(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
    }

    @Test
    void deletePermanent_shouldFailWhenRepoFails() {
        Long id = 1L;
        Product trashed = createProduct();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(queryRepo.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepo.deleteProductPermanently(id)).thenReturn(Future.succeededFuture(false));

        Future<Boolean> result = commandService.deletePermanent(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
    }

    // --- incrementStock ---

    @Test
    void incrementStock_shouldIncrementAndReturnProduct() {
        Product incremented = createProduct();
        incremented.setCountInStock(15);

        when(commandRepo.incrementStock(1L, 5)).thenReturn(Future.succeededFuture(incremented));

        Future<ProductResponse> result = commandService.incrementStock(1L, 5);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getId()).isEqualTo(1L);
        assertThat(result.result().getCountInStock()).isEqualTo(15);
        verify(redisService).delete("product:1");
    }

    @Test
    void incrementStock_shouldFailWhenProductNotFound() {
        when(commandRepo.incrementStock(999L, 5)).thenReturn(Future.succeededFuture(null));

        Future<ProductResponse> result = commandService.incrementStock(999L, 5);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        verify(commandRepo).incrementStock(999L, 5);
    }

    // --- restoreAll ---

    @Test
    void restoreAll_shouldRestoreAll() {
        when(commandRepo.restoreAllProducts()).thenReturn(Future.succeededFuture(5));

        Future<Boolean> result = commandService.restoreAll();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
        verify(redisService).deleteByPattern("product:list:*");
    }

    @Test
    void restoreAll_shouldFailWhenNoneTrashed() {
        when(commandRepo.restoreAllProducts()).thenReturn(Future.succeededFuture(0));

        Future<Boolean> result = commandService.restoreAll();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- deleteAllPermanent ---

    @Test
    void deleteAllPermanent_shouldDeleteAll() {
        when(commandRepo.deleteAllPermanentProducts()).thenReturn(Future.succeededFuture(3));

        Future<Boolean> result = commandService.deleteAllPermanent();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
        verify(redisService).deleteByPattern("product:list:*");
    }

    @Test
    void deleteAllPermanent_shouldFailWhenNoneTrashed() {
        when(commandRepo.deleteAllPermanentProducts()).thenReturn(Future.succeededFuture(0));

        Future<Boolean> result = commandService.deleteAllPermanent();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }
}
