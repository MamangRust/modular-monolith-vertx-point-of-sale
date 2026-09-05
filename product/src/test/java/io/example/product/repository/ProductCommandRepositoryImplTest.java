package io.example.product.repository;

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

import io.example.product.domain.requests.CreateProductRequest;
import io.example.product.domain.requests.UpdateProductRequest;
import io.example.product.model.Product;
import io.example.product.repository.impl.ProductCommandRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class ProductCommandRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private ProductCommandRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ProductCommandRepositoryImpl(client);
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

    private Row mockRow(Long id, String name, Long merchantId, Long categoryId, Integer price, Integer stock) {
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
        return row;
    }

    @Test
    void createProduct_shouldInsertAndReturnProduct() {
        CreateProductRequest req = CreateProductRequest.builder()
                .merchantId(1)
                .categoryId(1)
                .name("Test Product")
                .description("Desc")
                .price(50000)
                .countInStock(10)
                .brand("Brand")
                .weight(500)
                .slugProduct("test-product")
                .imageProduct("test.jpg")
                .build();

        Row mockRow = mockRow(1L, "Test Product", 1L, 1L, 50000, 10);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.createProduct(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getProductId()).isEqualTo(1L);
        assertThat(result.result().getName()).isEqualTo("Test Product");
        assertThat(result.result().getPrice()).isEqualTo(50000);
    }

    @Test
    void updateProduct_shouldUpdateAndReturnProduct() {
        UpdateProductRequest req = UpdateProductRequest.builder()
                .productId(1)
                .categoryId(1)
                .name("Updated Product")
                .description("Updated Desc")
                .price(60000)
                .countInStock(20)
                .brand("UpdatedBrand")
                .weight(600)
                .imageProduct("updated.jpg")
                .build();

        Row mockRow = mockRow(1L, "Updated Product", 1L, 1L, 60000, 20);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.updateProduct(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Updated Product");
        assertThat(result.result().getPrice()).isEqualTo(60000);
    }

    @Test
    void updateProduct_shouldReturnNullWhenNotFound() {
        UpdateProductRequest req = UpdateProductRequest.builder()
                .productId(999)
                .name("Non existent")
                .build();

        RowSet<Row> rowSet = mockRowSet(List.of(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.updateProduct(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void trashProduct_shouldSoftDeleteAndReturnProduct() {
        Row mockRow = mockRow(1L, "Test", 1L, 1L, 50000, 10);
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.trashProduct(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
    }

    @Test
    void trashProduct_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.trashProduct(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void restoreProduct_shouldRestoreAndReturnProduct() {
        Row mockRow = mockRow(1L, "Test", 1L, 1L, 50000, 10);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.restoreProduct(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNull();
    }

    @Test
    void restoreProduct_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.restoreProduct(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void incrementStock_shouldAddQuantityAndReturnProduct() {
        Row mockRow = mockRow(1L, "Test Product", 1L, 1L, 50000, 15);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.incrementStock(1L, 5);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getProductId()).isEqualTo(1L);
        assertThat(result.result().getCountInStock()).isEqualTo(15);
    }

    @Test
    void incrementStock_shouldReturnNullWhenProductMissing() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Product> result = repository.incrementStock(999L, 5);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void deleteProductPermanently_shouldReturnTrueOnSuccess() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Boolean> result = repository.deleteProductPermanently(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void deleteProductPermanently_shouldReturnFalseWhenNoRowsAffected() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Boolean> result = repository.deleteProductPermanently(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void restoreAllProducts_shouldReturnAffectedCount() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 5);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<Integer> result = repository.restoreAllProducts();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(5);
    }

    @Test
    void deleteAllPermanentProducts_shouldReturnAffectedCount() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 3);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<Integer> result = repository.deleteAllPermanentProducts();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(3);
    }
}
