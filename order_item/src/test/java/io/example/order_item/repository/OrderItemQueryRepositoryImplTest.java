package io.example.order_item.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

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
import io.example.order_item.domain.requests.FindAllOrderItems;
import io.example.order_item.model.OrderItem;
import io.example.order_item.repository.impl.OrderItemQueryRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class OrderItemQueryRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private OrderItemQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new OrderItemQueryRepositoryImpl(client);
    }

    @SuppressWarnings("unchecked")
    private RowSet<Row> mockRowSet(List<Row> rowsList) {
        RowSet<Row> rowSet = mock(RowSet.class);
        io.vertx.sqlclient.RowIterator<Row> rowIterator = mock(io.vertx.sqlclient.RowIterator.class);
        Iterator<Row> iterator = rowsList.iterator();
        org.mockito.Mockito.lenient().when(rowIterator.hasNext()).thenAnswer(inv -> iterator.hasNext());
        org.mockito.Mockito.lenient().when(rowIterator.next()).thenAnswer(inv -> iterator.next());
        org.mockito.Mockito.lenient().when(rowSet.iterator()).thenReturn(rowIterator);
        org.mockito.Mockito.lenient().doAnswer(inv -> {
            Consumer<Row> consumer = inv.getArgument(0);
            rowsList.forEach(consumer);
            return null;
        }).when(rowSet).forEach(any(Consumer.class));
        return rowSet;
    }

    private Row mockRow(Long id, Long orderId, Long productId, Integer quantity, Integer price, Integer totalCount) {
        Row row = mock(Row.class);
        when(row.getLong("order_item_id")).thenReturn(id);
        when(row.getLong("order_id")).thenReturn(orderId);
        when(row.getLong("product_id")).thenReturn(productId);
        when(row.getInteger("quantity")).thenReturn(quantity);
        when(row.getInteger("price")).thenReturn(price);

        LocalDateTime now = LocalDateTime.now();
        when(row.get(LocalDateTime.class, "created_at")).thenReturn(now);
        when(row.get(LocalDateTime.class, "updated_at")).thenReturn(now);

        if (totalCount != null) {
            when(row.getInteger("total_count")).thenReturn(totalCount);
        }
        return row;
    }

    @Test
    void getOrderItems_shouldReturnPagedResult() {
        FindAllOrderItems req = FindAllOrderItems.builder().page(1).pageSize(10).search("test").build();
        Row mockRow = mockRow(1L, 10L, 100L, 2, 5000, 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<OrderItem>> result = repository.getOrderItems(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getTotalRecords()).isEqualTo(1);
        assertThat(result.result().getData().get(0).getOrderItemId()).isEqualTo(1L);
    }

    @Test
    void getOrderItemsActive_shouldReturnPagedResult() {
        FindAllOrderItems req = FindAllOrderItems.builder().page(1).pageSize(10).build();
        Row mockRow = mockRow(1L, 10L, 100L, 2, 5000, 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<OrderItem>> result = repository.getOrderItemsActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getOrderItemId()).isEqualTo(1L);
    }

    @Test
    void getOrderItemsTrashed_shouldReturnPagedResult() {
        FindAllOrderItems req = FindAllOrderItems.builder().page(1).pageSize(10).build();
        Row mockRow = mockRow(1L, 10L, 100L, 2, 5000, 1);
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<OrderItem>> result = repository.getOrderItemsTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getDeletedAt()).isNotNull();
    }

    @Test
    void getOrderItemsByOrder_shouldReturnList() {
        Row mockRow = mockRow(1L, 10L, 100L, 2, 5000, null);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<OrderItem>> result = repository.getOrderItemsByOrder(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getOrderId()).isEqualTo(10L);
    }

    @Test
    void getOrderItemsByOrder_shouldReturnEmptyListWhenNoData() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList());

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<OrderItem>> result = repository.getOrderItemsByOrder(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEmpty();
    }

    @Test
    void findByTrashedId_shouldReturnOrderItemWhenFound() {
        Row mockRow = mockRow(1L, 10L, 100L, 2, 5000, null);
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<OrderItem> result = repository.findByTrashedId(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
    }

    @Test
    void findByTrashedId_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList());

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<OrderItem> result = repository.findByTrashedId(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }
}
