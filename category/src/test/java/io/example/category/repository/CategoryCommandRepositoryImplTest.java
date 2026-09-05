package io.example.category.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.category.domain.requests.CreateCategoryRequest;
import io.example.category.domain.requests.UpdateCategoryRequest;
import io.example.category.model.Category;
import io.example.category.repository.impl.CategoryCommandRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class CategoryCommandRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private CategoryCommandRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CategoryCommandRepositoryImpl(client);
    }

    @SuppressWarnings("unchecked")
    private RowSet<Row> mockRowSet(List<Row> rowsList, int rowCount) {
        RowSet<Row> rowSet = mock(RowSet.class);
        io.vertx.sqlclient.RowIterator<Row> rowIterator = mock(io.vertx.sqlclient.RowIterator.class);
        Iterator<Row> iterator = rowsList.iterator();
        org.mockito.Mockito.lenient().when(rowIterator.hasNext()).thenAnswer(inv -> iterator.hasNext());
        org.mockito.Mockito.lenient().when(rowIterator.next()).thenAnswer(inv -> iterator.next());
        org.mockito.Mockito.lenient().when(rowSet.iterator()).thenReturn(rowIterator);
        org.mockito.Mockito.lenient().when(rowSet.rowCount()).thenReturn(rowCount);
        return rowSet;
    }

    private Row mockRow(Long id, String name, String description, String slug) {
        Row row = mock(Row.class);
        when(row.getLong("category_id")).thenReturn(id);
        when(row.getString("name")).thenReturn(name);
        when(row.getString("description")).thenReturn(description);
        when(row.getString("slug_category")).thenReturn(slug);
        
        LocalDateTime now = LocalDateTime.now();
        when(row.get(LocalDateTime.class, "created_at")).thenReturn(now);
        when(row.get(LocalDateTime.class, "updated_at")).thenReturn(now);
        return row;
    }

    @Test
    void createCategory_shouldInsertAndReturnCategory() {
        CreateCategoryRequest req = CreateCategoryRequest.builder().name("Books").description("Book items").slugCategory("books").build();
        Row mockRow = mockRow(1L, "Books", "Book items", "books");
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Category> result = repository.createCategory(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getCategoryId()).isEqualTo(1L);
        assertThat(result.result().getName()).isEqualTo("Books");
    }

    @Test
    void updateCategory_shouldUpdateAndReturnCategory() {
        UpdateCategoryRequest req = UpdateCategoryRequest.builder().categoryId(1).name("Books New").description("New desc").slugCategory("books-new").build();
        Row mockRow = mockRow(1L, "Books New", "New desc", "books-new");
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Category> result = repository.updateCategory(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Books New");
    }

    @Test
    void trashCategory_shouldSoftDeleteAndReturnCategory() {
        Row mockRow = mockRow(1L, "Books", "Books desc", "books");
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Category> result = repository.trashCategory(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
    }

    @Test
    void restoreCategory_shouldRestoreAndReturnCategory() {
        Row mockRow = mockRow(1L, "Books", "Books desc", "books");
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Category> result = repository.restoreCategory(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNull();
    }

    @Test
    void deleteCategoryPermanently_shouldReturnTrueOnSuccess() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Boolean> result = repository.deleteCategoryPermanently(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void restoreAllCategories_shouldReturnAffectedCount() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 12);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<Integer> result = repository.restoreAllCategories();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(12);
    }

    @Test
    void deleteAllPermanentCategories_shouldReturnAffectedCount() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 8);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<Integer> result = repository.deleteAllPermanentCategories();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(8);
    }
}
