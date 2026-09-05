package io.example.order.repository;

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

import io.example.order.domain.requests.CreateOrderRecordRequest;
import io.example.order.domain.requests.UpdateOrderRecordRequest;
import io.example.order.model.Order;
import io.example.order.repository.impl.OrderCommandRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class OrderCommandRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private OrderCommandRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new OrderCommandRepositoryImpl(client);
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

    private Row mockOrderRow(Long orderId, Long merchantId, Long cashierId, Long totalPrice) {
        Row row = mock(Row.class);
        when(row.getLong("order_id")).thenReturn(orderId);
        when(row.getLong("merchant_id")).thenReturn(merchantId);
        when(row.getLong("cashier_id")).thenReturn(cashierId);
        when(row.getLong("total_price")).thenReturn(totalPrice);

        LocalDateTime now = LocalDateTime.now();
        when(row.get(LocalDateTime.class, "created_at")).thenReturn(now);
        when(row.get(LocalDateTime.class, "updated_at")).thenReturn(now);
        return row;
    }

    private Row mockOrderRowWithDeleted(Long orderId, Long merchantId, Long cashierId, Long totalPrice) {
        Row row = mockOrderRow(orderId, merchantId, cashierId, totalPrice);
        when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        return row;
    }

    @Test
    void createOrder_shouldInsertAndReturnOrder() {
        CreateOrderRecordRequest req = CreateOrderRecordRequest.builder()
                .merchantId(1L)
                .cashierId(2L)
                .totalPrice(50000)
                .build();

        Row mockRow = mockOrderRow(10L, 1L, 2L, 50000L);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.createOrder(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getOrderId()).isEqualTo(10L);
        assertThat(result.result().getTotalPrice()).isEqualTo(50000L);
    }

    @Test
    void updateOrder_shouldUpdateAndReturnOrder() {
        UpdateOrderRecordRequest req = UpdateOrderRecordRequest.builder()
                .orderId(10L)
                .totalPrice(60000)
                .build();

        Row mockRow = mockOrderRow(10L, 1L, 2L, 60000L);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.updateOrder(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getTotalPrice()).isEqualTo(60000L);
    }

    @Test
    void updateOrder_shouldReturnNullWhenNotFound() {
        UpdateOrderRecordRequest req = UpdateOrderRecordRequest.builder()
                .orderId(999L)
                .totalPrice(60000)
                .build();

        RowSet<Row> rowSet = mockRowSet(Collections.emptyList(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.updateOrder(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void trashedOrder_shouldSoftDeleteAndReturnOrder() {
        Row mockRow = mockOrderRowWithDeleted(10L, 1L, 2L, 50000L);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.trashedOrder(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
    }

    @Test
    void trashedOrder_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.trashedOrder(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void restoreOrder_shouldRestoreAndReturnOrder() {
        Row mockRow = mockOrderRow(10L, 1L, 2L, 50000L);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.restoreOrder(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNull();
    }

    @Test
    void restoreOrder_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.restoreOrder(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void deleteOrderPermanent_shouldReturnTrueOnSuccess() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList(), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Boolean> result = repository.deleteOrderPermanent(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void deleteOrderPermanent_shouldReturnFalseWhenNoRowsAffected() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList(), 0);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Boolean> result = repository.deleteOrderPermanent(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isFalse();
    }

    @Test
    void deleteAllOrderPermanent_shouldReturnAffectedCount() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList(), 3);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<Integer> result = repository.deleteAllOrderPermanent();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(3);
    }
}
