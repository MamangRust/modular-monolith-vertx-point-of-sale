package io.example.cashier.repository;

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

import io.example.cashier.domain.requests.cashier.FindAllCashierMerchant;
import io.example.cashier.domain.requests.cashier.FindAllCashiers;
import io.example.cashier.model.Cashier;
import io.example.cashier.repository.impl.CashierQueryRepositoryImpl;
import io.example.common.domain.PagedResult;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class CashierQueryRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private CashierQueryRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CashierQueryRepositoryImpl(client);
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

    private Row mockRow(Long id, Long merchantId, Long userId, String name, Integer totalCount) {
        Row row = mock(Row.class);
        when(row.getLong("cashier_id")).thenReturn(id);
        when(row.getLong("merchant_id")).thenReturn(merchantId);
        when(row.getLong("user_id")).thenReturn(userId);
        when(row.getString("name")).thenReturn(name);
        
        LocalDateTime now = LocalDateTime.now();
        when(row.get(LocalDateTime.class, "created_at")).thenReturn(now);
        when(row.get(LocalDateTime.class, "updated_at")).thenReturn(now);
        
        if (totalCount != null) {
            when(row.getInteger("total_count")).thenReturn(totalCount);
        }
        return row;
    }

    @Test
    void findAllCashiers_shouldReturnPagedResult() {
        FindAllCashiers req = FindAllCashiers.builder().page(1).pageSize(10).search("Test").build();
        Row mockRow = mockRow(1L, 2L, 3L, "Test Cashier", 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Cashier>> result = repository.findAllCashiers(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getTotalRecords()).isEqualTo(1);
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Test Cashier");
    }

    @Test
    void findById_shouldReturnCashierWhenFound() {
        Row mockRow = mockRow(1L, 2L, 3L, "Test Cashier", null);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Cashier> result = repository.findById(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNotNull();
        assertThat(result.result().getCashierId()).isEqualTo(1L);
    }

    @Test
    void findById_shouldReturnNullWhenNotFound() {
        RowSet<Row> rowSet = mockRowSet(Collections.emptyList());

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Cashier> result = repository.findById(999L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isNull();
    }

    @Test
    void findByName_shouldReturnCashierWhenFound() {
        Row mockRow = mockRow(1L, 2L, 3L, "Test Cashier", null);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Cashier> result = repository.findByName("Test Cashier");

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("Test Cashier");
    }

    @Test
    void findByTrashedId_shouldReturnCashier() {
        Row mockRow = mockRow(1L, 2L, 3L, "Test Cashier", null);
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Cashier> result = repository.findByTrashedId(1L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
    }

    @Test
    void findByActive_shouldReturnPagedResult() {
        FindAllCashiers req = FindAllCashiers.builder().page(1).pageSize(10).build();
        Row mockRow = mockRow(1L, 2L, 3L, "Active Cashier", 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Cashier>> result = repository.findByActive(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData().get(0).getName()).isEqualTo("Active Cashier");
    }

    @Test
    void findByTrashed_shouldReturnPagedResult() {
        FindAllCashiers req = FindAllCashiers.builder().page(1).pageSize(10).build();
        Row mockRow = mockRow(1L, 2L, 3L, "Trashed Cashier", 1);
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Cashier>> result = repository.findByTrashed(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData().get(0).getDeletedAt()).isNotNull();
    }

    @Test
    void findByMerchant_shouldReturnPagedResult() {
        FindAllCashierMerchant req = FindAllCashierMerchant.builder().merchantId(1).page(1).pageSize(10).build();
        Row mockRow = mockRow(1L, 1L, 3L, "Merchant Cashier", 1);
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<PagedResult<Cashier>> result = repository.findByMerchant(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData().get(0).getMerchantId()).isEqualTo(1L);
    }
}
