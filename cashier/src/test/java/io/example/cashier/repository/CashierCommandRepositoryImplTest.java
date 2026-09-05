package io.example.cashier.repository;

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

import io.example.cashier.domain.requests.cashier.CreateCashierRequest;
import io.example.cashier.domain.requests.cashier.UpdateCashierRequest;
import io.example.cashier.model.Cashier;
import io.example.cashier.repository.impl.CashierCommandRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class CashierCommandRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private CashierCommandRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CashierCommandRepositoryImpl(client);
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

    private Row mockRow(Long id, Long merchantId, Long userId, String name) {
        Row row = mock(Row.class);
        when(row.getLong("cashier_id")).thenReturn(id);
        when(row.getLong("merchant_id")).thenReturn(merchantId);
        when(row.getLong("user_id")).thenReturn(userId);
        when(row.getString("name")).thenReturn(name);
        
        LocalDateTime now = LocalDateTime.now();
        when(row.get(LocalDateTime.class, "created_at")).thenReturn(now);
        when(row.get(LocalDateTime.class, "updated_at")).thenReturn(now);
        return row;
    }

    @Test
    void createCashier_shouldInsertAndReturnCashier() {
        CreateCashierRequest req = CreateCashierRequest.builder().merchantId(1).userId(2).name("John Doe").build();
        Row mockRow = mockRow(10L, 1L, 2L, "John Doe");
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Cashier> result = repository.createCashier(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getCashierId()).isEqualTo(10L);
        assertThat(result.result().getName()).isEqualTo("John Doe");
    }

    @Test
    void updateCashier_shouldUpdateAndReturnCashier() {
        UpdateCashierRequest req = UpdateCashierRequest.builder().cashierId(10).name("John Updated").build();
        Row mockRow = mockRow(10L, 1L, 2L, "John Updated");
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Cashier> result = repository.updateCashier(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getName()).isEqualTo("John Updated");
    }

    @Test
    void trashCashier_shouldSoftDeleteAndReturnCashier() {
        Row mockRow = mockRow(10L, 1L, 2L, "John Doe");
        when(mockRow.get(LocalDateTime.class, "deleted_at")).thenReturn(LocalDateTime.now());
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Cashier> result = repository.trashCashier(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNotNull();
    }

    @Test
    void restoreCashier_shouldRestoreAndReturnCashier() {
        Row mockRow = mockRow(10L, 1L, 2L, "John Doe");
        RowSet<Row> rowSet = mockRowSet(List.of(mockRow), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Cashier> result = repository.restoreCashier(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getDeletedAt()).isNull();
    }

    @Test
    void deleteCashierPermanent_shouldReturnTrueOnSuccess() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 1);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<Boolean> result = repository.deleteCashierPermanent(10L);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isTrue();
    }

    @Test
    void restoreAllCashier_shouldReturnAffectedCount() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 5);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<Integer> result = repository.restoreAllCashier();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(5);
    }

    @Test
    void deleteAllCashierPermanent_shouldReturnAffectedCount() {
        RowSet<Row> rowSet = mockRowSet(List.of(), 3);

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute()).thenReturn(Future.succeededFuture(rowSet));

        Future<Integer> result = repository.deleteAllCashierPermanent();

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(3);
    }
}
