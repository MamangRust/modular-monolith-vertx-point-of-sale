package io.example.category.service;

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

import io.example.category.domain.requests.FindAllCategory;
import io.example.category.domain.response.category.CategoryResponse;
import io.example.category.domain.response.category.CategoryResponseDeleteAt;
import io.example.category.model.Category;
import io.example.category.repository.CategoryQueryRepository;
import io.example.category.service.impl.CategoryQueryServiceImpl;
import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class CategoryQueryServiceImplTest {

    @Mock private CategoryQueryRepository queryRepo;
    @Mock private RedisService redis;
    @Mock private TracingMetrics metrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private CategoryQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        lenient().when(metrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redis.setJson(anyString(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));
        queryService = new CategoryQueryServiceImpl(queryRepo, redis, metrics);
    }

    private Category createCategory() {
        return Category.builder()
                .categoryId(1L)
                .name("Books")
                .description("Reading Books")
                .slugCategory("books")
                .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
                .build();
    }

    // --- getCategories ---

    @Test
    void getCategories_shouldFetchFromDbWhenCacheMiss() {
        FindAllCategory req = FindAllCategory.builder().search("Books").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getCategories(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createCategory()), 1)));

        Future<PagedResult<CategoryResponse>> result = queryService.getCategories(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Books");
        verify(queryRepo).getCategories(req);
    }

    @Test
    void getCategories_shouldReturnFromCacheWhenCacheHit() {
        FindAllCategory req = FindAllCategory.builder().search("Books").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"categoryId\":1,\"name\":\"Books\",\"description\":\"Reading Books\",\"slugCategory\":\"books\",\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<CategoryResponse>> result = queryService.getCategories(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Books");
        verify(queryRepo, never()).getCategories(any());
    }

    // --- getCategoryById ---

    @Test
    void getCategoryById_shouldFetchFromDbWhenCacheMiss() {
        Long id = 1L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getCategoryById(id)).thenReturn(Future.succeededFuture(createCategory()));

        Future<CategoryResponse> result = queryService.getCategoryById(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Books");
        verify(queryRepo).getCategoryById(id);
    }

    @Test
    void getCategoryById_shouldReturnFromCacheWhenCacheHit() {
        Long id = 1L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(createCategory()));

        Future<CategoryResponse> result = queryService.getCategoryById(id);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Books");
        verify(queryRepo, never()).getCategoryById(anyLong());
    }

    @Test
    void getCategoryById_shouldFailWhenNotFound() {
        Long id = 99L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getCategoryById(id)).thenReturn(Future.succeededFuture(null));

        Future<CategoryResponse> result = queryService.getCategoryById(id);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // --- getCategoriesActive ---

    @Test
    void getCategoriesActive_shouldFetchFromDbWhenCacheMiss() {
        FindAllCategory req = FindAllCategory.builder().search("").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getCategoriesActive(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createCategory()), 1)));

        Future<PagedResult<CategoryResponseDeleteAt>> result = queryService.getCategoriesActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).getCategoriesActive(req);
    }

    @Test
    void getCategoriesActive_shouldReturnFromCacheWhenCacheHit() {
        FindAllCategory req = FindAllCategory.builder().search("").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"categoryId\":1,\"name\":\"Books\",\"description\":\"Reading Books\",\"slugCategory\":\"books\",\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<CategoryResponseDeleteAt>> result = queryService.getCategoriesActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo, never()).getCategoriesActive(any());
    }

    // --- getTrashedCategories ---

    @Test
    void getTrashedCategories_shouldFetchFromDbWhenCacheMiss() {
        FindAllCategory req = FindAllCategory.builder().search("").page(1).pageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getCategoriesTrashed(req)).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createCategory()), 1)));

        Future<PagedResult<CategoryResponseDeleteAt>> result = queryService.getTrashedCategories(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).getCategoriesTrashed(req);
    }

    @Test
    void getTrashedCategories_shouldReturnFromCacheWhenCacheHit() {
        FindAllCategory req = FindAllCategory.builder().search("").page(1).pageSize(10).build();
        String json = "{\"data\":[{\"categoryId\":1,\"name\":\"Books\",\"description\":\"Reading Books\",\"slugCategory\":\"books\",\"createdAt\":1704067200000,\"updatedAt\":1704067200000}],\"totalRecords\":1}";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(json));

        Future<PagedResult<CategoryResponseDeleteAt>> result = queryService.getTrashedCategories(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo, never()).getCategoriesTrashed(any());
    }
}
