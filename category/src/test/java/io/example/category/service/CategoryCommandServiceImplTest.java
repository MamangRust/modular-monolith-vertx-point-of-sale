package io.example.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.category.domain.requests.CreateCategoryRequest;
import io.example.category.domain.requests.UpdateCategoryRequest;
import io.example.category.domain.response.category.CategoryResponse;
import io.example.category.domain.response.category.CategoryResponseDeleteAt;
import io.example.category.model.Category;
import io.example.category.repository.CategoryCommandRepository;
import io.example.category.repository.CategoryQueryRepository;
import io.example.category.service.impl.CategoryCommandServiceImpl;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class CategoryCommandServiceImplTest {

    @Mock private CategoryCommandRepository commandRepository;
    @Mock private CategoryQueryRepository queryRepository;
    @Mock private RedisService redisService;
    @Mock private TracingMetrics tracingMetrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private CategoryCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        commandService = new CategoryCommandServiceImpl(
                commandRepository,
                queryRepository,
                redisService,
                tracingMetrics
        );
    }

    private Category createCategory() {
        return Category.builder()
                .categoryId(1L)
                .name("Books")
                .description("Reading materials")
                .slugCategory("books")
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .build();
    }

    // --- createCategory ---

    @Test
    void createCategory_shouldCreateSuccessfully() {
        CreateCategoryRequest req = CreateCategoryRequest.builder()
                .name("New Category")
                .description("Desc")
                .build();

        Category created = createCategory();
        created.setName("New Category");
        created.setSlugCategory("new-category");

        when(queryRepository.getCategoryByName("New Category")).thenReturn(Future.succeededFuture(null));
        when(commandRepository.createCategory(req)).thenReturn(Future.succeededFuture(created));

        Future<CategoryResponse> result = commandService.createCategory(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("New Category");
        verify(redisService).deleteByPattern("categories:list:*");
    }

    @Test
    void createCategory_shouldFailWhenNameExists() {
        CreateCategoryRequest req = CreateCategoryRequest.builder()
                .name("Existing")
                .build();

        when(queryRepository.getCategoryByName("Existing")).thenReturn(Future.succeededFuture(createCategory()));

        Future<CategoryResponse> result = commandService.createCategory(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        verify(commandRepository, never()).createCategory(any());
    }

    // --- updateCategory ---

    @Test
    void updateCategory_shouldUpdateSuccessfully() {
        UpdateCategoryRequest req = UpdateCategoryRequest.builder()
                .categoryId(1)
                .name("Updated Category")
                .description("New Desc")
                .build();

        Category existing = createCategory();
        Category updated = createCategory();
        updated.setName("Updated Category");

        when(queryRepository.getCategoryById(1L)).thenReturn(Future.succeededFuture(existing));
        when(queryRepository.getCategoryByName("Updated Category")).thenReturn(Future.succeededFuture(null));
        when(commandRepository.updateCategory(req)).thenReturn(Future.succeededFuture(updated));

        Future<CategoryResponse> result = commandService.updateCategory(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Updated Category");
        verify(redisService).delete("category:1");
        verify(redisService).deleteByPattern("categories:list:*");
    }

    @Test
    void updateCategory_shouldFailWhenNotFound() {
        UpdateCategoryRequest req = UpdateCategoryRequest.builder()
                .categoryId(99)
                .name("Updated")
                .build();

        when(queryRepository.getCategoryById(99L)).thenReturn(Future.succeededFuture(null));

        Future<CategoryResponse> result = commandService.updateCategory(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateCategory_shouldFailWhenNameUsedByAnother() {
        UpdateCategoryRequest req = UpdateCategoryRequest.builder()
                .categoryId(1)
                .name("Another Name")
                .build();

        Category existing = createCategory();
        Category another = createCategory();
        another.setCategoryId(2L);

        when(queryRepository.getCategoryById(1L)).thenReturn(Future.succeededFuture(existing));
        when(queryRepository.getCategoryByName("Another Name")).thenReturn(Future.succeededFuture(another));

        Future<CategoryResponse> result = commandService.updateCategory(req);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
    }

    // --- trashCategory ---

    @Test
    void trashCategory_shouldTrashSuccessfully() {
        Long id = 1L;
        Category trashed = createCategory();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(commandRepository.trashCategory(id)).thenReturn(Future.succeededFuture(trashed));

        Future<CategoryResponseDeleteAt> result = commandService.trashCategory(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
        verify(redisService).delete("category:1");
    }

    @Test
    void trashCategory_shouldFailWhenNotFoundOrAlreadyTrashed() {
        Long id = 99L;
        when(commandRepository.trashCategory(id)).thenReturn(Future.succeededFuture(null));

        Future<CategoryResponseDeleteAt> result = commandService.trashCategory(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- restoreCategory ---

    @Test
    void restoreCategory_shouldRestoreSuccessfully() {
        Long id = 1L;
        Category trashed = createCategory();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));
        Category restored = createCategory();

        when(queryRepository.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepository.restoreCategory(id)).thenReturn(Future.succeededFuture(restored));

        Future<CategoryResponseDeleteAt> result = commandService.restoreCategory(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNull();
        verify(redisService).delete("category:1");
    }

    @Test
    void restoreCategory_shouldFailWhenNotTrashed() {
        Long id = 1L;
        when(queryRepository.findByTrashedId(id)).thenReturn(Future.succeededFuture(null));

        Future<CategoryResponseDeleteAt> result = commandService.restoreCategory(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
    }

    // --- deleteCategoryPermanently ---

    @Test
    void deleteCategoryPermanently_shouldDeleteSuccessfully() {
        Long id = 1L;
        Category trashed = createCategory();
        trashed.setDeletedAt(Timestamp.valueOf(LocalDateTime.now()));

        when(queryRepository.findByTrashedId(id)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepository.deleteCategoryPermanently(id)).thenReturn(Future.succeededFuture(true));

        Future<Boolean> result = commandService.deleteCategoryPermanently(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
        verify(redisService).delete("category:1");
    }

    @Test
    void deleteCategoryPermanently_shouldFailWhenNotTrashed() {
        Long id = 1L;
        when(queryRepository.findByTrashedId(id)).thenReturn(Future.succeededFuture(null));

        Future<Boolean> result = commandService.deleteCategoryPermanently(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
    }

    // --- restoreAllCategories ---

    @Test
    void restoreAllCategories_shouldRestoreAll() {
        when(commandRepository.restoreAllCategories()).thenReturn(Future.succeededFuture(5));

        Future<Void> result = commandService.restoreAllCategories();

        assertThat(result.succeeded()).isTrue();
        verify(redisService).deleteByPattern("categories:list:*");
    }

    @Test
    void restoreAllCategories_shouldFailWhenNoneTrashed() {
        when(commandRepository.restoreAllCategories()).thenReturn(Future.succeededFuture(0));

        Future<Void> result = commandService.restoreAllCategories();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- deleteAllPermanentCategories ---

    @Test
    void deleteAllPermanentCategories_shouldDeleteAll() {
        when(commandRepository.deleteAllPermanentCategories()).thenReturn(Future.succeededFuture(3));

        Future<Void> result = commandService.deleteAllPermanentCategories();

        assertThat(result.succeeded()).isTrue();
        verify(redisService).deleteByPattern("categories:list:*");
    }

    @Test
    void deleteAllPermanentCategories_shouldFailWhenNoneTrashed() {
        when(commandRepository.deleteAllPermanentCategories()).thenReturn(Future.succeededFuture(0));

        Future<Void> result = commandService.deleteAllPermanentCategories();

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }
}
