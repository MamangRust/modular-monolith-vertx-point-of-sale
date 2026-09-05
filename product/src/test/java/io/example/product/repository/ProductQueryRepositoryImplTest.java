package io.example.product.repository;

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

import io.example.common.domain.PagedResult;
import io.example.product.domain.requests.FindAllProducts;
import io.example.product.model.Product;
import io.example.product.repository.impl.ProductQueryRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class ProductQueryRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private ProductQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ProductQueryRepositoryImpl(client);
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

    private Row mockRow(Long id, String name, Long merchantId, Long categoryId, Integer price, Integer stock,
            Integer totalCount) {
        Row row = mock(Row.class);
        when(row.getLong("product_id")).thenReturn(id);
        when(row.getLong("merchant_id")).thenReturn(merchantId);
        when(row.getLong("category_id")).thenReturn(categoryId);
        when(row.getString("name")).thenReturn(name);
        when(row.getString("description")).thenReturn("Description");
        when(row.getInteger("price")).thenReturn(price);
        when(row.getInteger("count_in_stock")).thenReturn(stock);
        when(row.getString("brand")).thenReturn("TestBrand");
        when(row.getInteger("weight")).thenReturn(500);
        when(row.getString("slug_product")).thenReturn("test-product");
        when(row.getString("image_product")).thenReturn("test.jpg");
        when(row.getString("barcode")).thenReturn("12345");

        LocalDateTime now = LocalDateTime.now();
        when(row.get(LocalDateTime.class, "created_at")).thenReturn(now);
        when(row.get(LocalDateTime.class, "updated_at")).thenReturn(now);

        if (totalCount != null) {
            when(row.getInteger("total_count")).thenReturn(totalCount);
        }
        return row;
    }

    @Test
    void getProducts_shouldReturnPagedResult() {
        FindAllProducts req = FindAllProducts.builder().search("test").page(1).pageSize(10).build();
        Row mockRow = mockRow(1L, "Test Product", 1L, 1L, 50000, 10, 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Product>> result = repository.getProducts(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getTotalRecords()).isEqualTo(1);
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Test Product");
    }

    @Test
    void getProductsActive_shouldReturnPagedResult() {
        FindAllProducts req = FindAllProducts.builder().page(1).pageSize(10).build();
        Row mockRow = mockRow(1L, "Active Product", 1L, 1L, 50000, 10, 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Product>> result = repository.getProductsActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Active Product");
    }

    @Test
    void getProductsTrashed_shouldReturnPagedResult() {
        FindAllProducts req = FindAllProducts.builder().page(1).pageSize(10).build();
        Row mockRow = mockRow(1L, "Trashed Product", 1L, 1L, 50000, 10, 1);
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Product>> result = repository.getProductsTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getDeletedAt()).isNotNull();
    }

    @Test
    void getProductById_shouldReturnProductWhenFound() {
        Row mockRow = mockRow(1L, "Test Product", 1L, 1L, 50000, 10, null);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.getProductById(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getProductId()).isEqualTo(1L);
    }

    @Test
    void getProductById_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList());

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.getProductById(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void findByTrashedId_shouldReturnProductWhenFound() {
        Row mockRow = mockRow(1L, "Trashed", 1L, 1L, 50000, 10, null);
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.findByTrashedId(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
    }

    @Test
    void findByTrashedId_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList());

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.findByTrashedId(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }
}
