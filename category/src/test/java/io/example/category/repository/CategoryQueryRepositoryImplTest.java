package io.example.category.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.category.domain.requests.FindAllCategory;
import io.example.category.model.Category;
import io.example.category.repository.impl.CategoryQueryRepositoryImpl;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class CategoryQueryRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private CategoryQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CategoryQueryRepositoryImpl(client);
    }

    @SuppressWarnings("unchecked")
    private RowSet<Row> mockRowSet(List<Row> rowsList) {
        RowSet<Row> rowSet = mock(RowSet.class);
        io.vertx.sqlclient.RowIterator<Row> rowIterator = mock(io.vertx.sqlclient.RowIterator.class);
        Iterator<Row> iterator = rowsList.iterator();
        org.mockito.Mockito.lenient().when(rowIterator.hasNext()).thenAnswer(inv -> iterator.hasNext());
        org.mockito.Mockito.lenient().when(rowIterator.next()).thenAnswer(inv -> iterator.next());
        org.mockito.Mockito.lenient().when(rowSet.iterator()).thenReturn(rowIterator);
        return rowSet;
    }

    private Row mockRow(Long id, String name, String description, String slug, Integer totalCount) {
        Row row = mock(Row.class);
        when(row.getLong("category_id")).thenReturn(id);
        when(row.getString("name")).thenReturn(name);
        when(row.getString("description")).thenReturn(description);
        when(row.getString("slug_category")).thenReturn(slug);
        
        LocalDateTime now = LocalDateTime.now();
        when(row.get(LocalDateTime.class, "created_at")).thenReturn(now);
        when(row.get(LocalDateTime.class, "updated_at")).thenReturn(now);
        
        if (totalCount != null) {
            when(row.getInteger("total_count")).thenReturn(totalCount);
        }
        return row;
    }

    @Test
    void getCategories_shouldReturnPagedResult() {
        FindAllCategory req = FindAllCategory.builder().page(1).pageSize(10).search("food").build();
        Row mockRow = mockRow(1L, "Food", "Food items", "food", 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Category>> result = repository.getCategories(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getTotalRecords()).isEqualTo(1);
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Food");
    }

    @Test
    void getCategoriesActive_shouldReturnPagedResult() {
        FindAllCategory req = FindAllCategory.builder().page(1).pageSize(10).build();
        Row mockRow = mockRow(1L, "Food Active", "Food items", "food-active", 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Category>> result = repository.getCategoriesActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Food Active");
    }

    @Test
    void getCategoriesTrashed_shouldReturnPagedResult() {
        FindAllCategory req = FindAllCategory.builder().page(1).pageSize(10).build();
        Row mockRow = mockRow(1L, "Food Trashed", "Food items", "food-trashed", 1);
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Category>> result = repository.getCategoriesTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData().get(0).getDeletedAt()).isNotNull();
    }

    @Test
    void getCategoryById_shouldReturnCategoryWhenFound() {
        Row mockRow = mockRow(1L, "Food", "Food items", "food", null);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Category> result = repository.getCategoryById(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getCategoryId()).isEqualTo(1L);
    }

    @Test
    void getCategoryById_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList());

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Category> result = repository.getCategoryById(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void findByTrashedId_shouldReturnCategory() {
        Row mockRow = mockRow(1L, "Food", "Food items", "food", null);
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Category> result = repository.findByTrashedId(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
    }

    @Test
    void getCategoryByName_shouldReturnCategoryWhenFound() {
        Row mockRow = mockRow(1L, "Food", "Food items", "food", null);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Category> result = repository.getCategoryByName("Food");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Food");
    }

    @Test
    void getCategoryByNameAndId_shouldReturnCategoryWhenFound() {
        Row mockRow = mockRow(1L, "Food", "Food items", "food", null);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Category> result = repository.getCategoryByNameAndId("Food", 1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Food");
    }
}
