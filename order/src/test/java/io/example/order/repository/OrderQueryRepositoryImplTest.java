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

import io.example.common.domain.PagedResult;
import io.example.order.domain.requests.FindAllOrderMerchant;
import io.example.order.domain.requests.FindAllOrders;
import io.example.order.model.Order;
import io.example.order.repository.impl.OrderQueryRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class OrderQueryRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private OrderQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new OrderQueryRepositoryImpl(client);
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

    private Row mockOrderRow(Long orderId, Long merchantId, Long cashierId, Long totalPrice, Integer totalCount) {
        Row row = mock(Row.class);
        when(row.getLong("order_id")).thenReturn(orderId);
        when(row.getLong("merchant_id")).thenReturn(merchantId);
        when(row.getLong("cashier_id")).thenReturn(cashierId);
        when(row.getLong("total_price")).thenReturn(totalPrice);

        LocalDateTime now = LocalDateTime.now();
        when(row.get(LocalDateTime.class, "created_at")).thenReturn(now);
        when(row.get(LocalDateTime.class, "updated_at")).thenReturn(now);

        if (totalCount != null) {
            when(row.getInteger("total_count")).thenReturn(totalCount);
        }
        return row;
    }

    private Row mockOrderRowWithDeleted(Long orderId, Long merchantId, Long cashierId, Long totalPrice,
            Integer totalCount) {
        Row row = mockOrderRow(orderId, merchantId, cashierId, totalPrice, totalCount);
        when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        return row;
    }

    @Test
    void findAllOrders_shouldReturnPagedResult() {
        FindAllOrders req = FindAllOrders.builder().page(1).pageSize(10).search("test").build();
        Row mockRow = mockOrderRow(1L, 2L, 3L, 50000L, 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Order>> result = repository.findAllOrders(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getTotalRecords()).isEqualTo(1);
        assertThat(result.result().getData().get(0).getOrderId()).isEqualTo(1L);
    }

    @Test
    void findById_shouldReturnOrderWhenFound() {
        Row mockRow = mockOrderRow(1L, 2L, 3L, 50000L, null);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.findById(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getOrderId()).isEqualTo(1L);
    }

    @Test
    void findById_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList());

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.findById(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void findByTrashedId_shouldReturnOrderWhenFound() {
        Row mockRow = mockOrderRowWithDeleted(1L, 2L, 3L, 50000L, null);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.findByTrashedId(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
    }

    @Test
    void findAllTrashed_shouldReturnAllTrashedOrders() {
        Row row1 = mockOrderRowWithDeleted(1L, 2L, 3L, 50000L, null);
        Row row2 = mockOrderRowWithDeleted(2L, 2L, 3L, 30000L, null);
        RowSet<Row> rowSet = mockRowSet(List.of(row1, row2));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<List<Order>> result = repository.findAllTrashed();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(2);
        assertThat(result.result().get(0).getOrderId()).isEqualTo(1L);
        assertThat(result.result().get(1).getOrderId()).isEqualTo(2L);
        assertThat(result.result().get(0).getDeletedAt()).isNotNull();
    }

    @Test
    void findAllTrashed_shouldReturnEmptyWhenNone() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList());

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<List<Order>> result = repository.findAllTrashed();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEmpty();
    }

    @Test
    void findByTrashedId_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList());

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Order> result = repository.findByTrashedId(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void findByActive_shouldReturnPagedResult() {
        FindAllOrders req = FindAllOrders.builder().page(1).pageSize(10).build();
        Row mockRow = mockOrderRow(1L, 2L, 3L, 50000L, 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Order>> result = repository.findByActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getOrderId()).isEqualTo(1L);
    }

    @Test
    void findByTrashed_shouldReturnPagedResult() {
        FindAllOrders req = FindAllOrders.builder().page(1).pageSize(10).build();
        Row mockRow = mockOrderRowWithDeleted(1L, 2L, 3L, 50000L, 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Order>> result = repository.findByTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getDeletedAt()).isNotNull();
    }

    @Test
    void findByMerchant_shouldReturnPagedResult() {
        FindAllOrderMerchant req = FindAllOrderMerchant.builder()
                .merchantId(2L)
                .page(1)
                .pageSize(10)
                .build();

        Row mockRow = mockOrderRow(1L, 2L, 3L, 50000L, 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Order>> result = repository.findByMerchant(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getMerchantId()).isEqualTo(2L);
    }
}
