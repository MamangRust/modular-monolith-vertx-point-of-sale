package io.example.order_item.repository;

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

import io.example.order_item.domain.requests.CreateOrderItemRequest;
import io.example.order_item.domain.requests.UpdateOrderItemRequest;
import io.example.order_item.model.OrderItem;
import io.example.order_item.repository.impl.OrderItemCommandRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class OrderItemCommandRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private OrderItemCommandRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new OrderItemCommandRepositoryImpl(client);
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

    private Row mockRow(Long id, Long orderId, Long productId, Integer quantity, Integer price) {
        Row row = mock(Row.class);
        when(row.getLong("order_item_id")).thenReturn(id);
        when(row.getLong("order_id")).thenReturn(orderId);
        when(row.getLong("product_id")).thenReturn(productId);
        when(row.getInteger("quantity")).thenReturn(quantity);
        when(row.getInteger("price")).thenReturn(price);

        LocalDateTime now = LocalDateTime.now();
        when(row.get(LocalDateTime.class, "created_at")).thenReturn(now);
        when(row.get(LocalDateTime.class, "updated_at")).thenReturn(now);
        return row;
    }

    @Test
    void createOrderItem_shouldInsertAndReturnOrderItem() {
        CreateOrderItemRequest req = CreateOrderItemRequest.builder()
                .orderId(1L)
                .productId(2L)
                .quantity(3)
                .price(5000)
                .build();

        Row mockRow = mockRow(10L, 1L, 2L, 3, 5000);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<OrderItem> result = repository.createOrderItem(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getOrderItemId()).isEqualTo(10L);
        assertThat(result.result().getOrderId()).isEqualTo(1L);
        assertThat(result.result().getProductId()).isEqualTo(2L);
        assertThat(result.result().getQuantity()).isEqualTo(3);
        assertThat(result.result().getPrice()).isEqualTo(5000);
    }

    @Test
    void updateOrderItem_shouldUpdateAndReturnOrderItem() {
        UpdateOrderItemRequest req = UpdateOrderItemRequest.builder()
                .orderItemId(10L)
                .orderId(1L)
                .productId(2L)
                .quantity(5)
                .price(7500)
                .build();

        Row mockRow = mockRow(10L, 1L, 2L, 5, 7500);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<OrderItem> result = repository.updateOrderItem(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getQuantity()).isEqualTo(5);
        assertThat(result.result().getPrice()).isEqualTo(7500);
    }

    @Test
    void updateOrderItem_shouldReturnNullWhenNotFound() {
        UpdateOrderItemRequest req = UpdateOrderItemRequest.builder()
                .orderItemId(999L)
                .orderId(1L)
                .productId(2L)
                .quantity(5)
                .price(7500)
                .build();

        RowSet<Row> rowSet = mockRowSet(List.of(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<OrderItem> result = repository.updateOrderItem(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void trashOrderItem_shouldSoftDeleteAndReturnOrderItem() {
        Row mockRow = mockRow(10L, 1L, 2L, 3, 5000);
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<OrderItem> result = repository.trashOrderItem(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
    }

    @Test
    void trashOrderItem_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<OrderItem> result = repository.trashOrderItem(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void restoreOrderItem_shouldRestoreAndReturnOrderItem() {
        Row mockRow = mockRow(10L, 1L, 2L, 3, 5000);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<OrderItem> result = repository.restoreOrderItem(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNull();
    }

    @Test
    void restoreOrderItem_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<OrderItem> result = repository.restoreOrderItem(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void deleteOrderItemPermanently_shouldReturnTrueOnSuccess() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Boolean> result = repository.deleteOrderItemPermanently(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void deleteOrderItemPermanently_shouldReturnFalseWhenNoRowsAffected() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Boolean> result = repository.deleteOrderItemPermanently(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void restoreAllOrdersItem_shouldReturnAffectedCount() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 5);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<Integer> result = repository.restoreAllOrdersItem();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(5);
    }

    @Test
    void deleteAllPermanentOrdersItem_shouldReturnAffectedCount() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 3);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<Integer> result = repository.deleteAllPermanentOrdersItem();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(3);
    }
}
