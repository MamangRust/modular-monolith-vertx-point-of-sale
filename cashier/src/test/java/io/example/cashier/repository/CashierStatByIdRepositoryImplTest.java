package io.example.cashier.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.cashier.domain.requests.cashier.MonthCashierIdRequest;
import io.example.cashier.domain.requests.cashier.MonthTotalSalesCashier;
import io.example.cashier.domain.requests.cashier.YearCashierIdRequest;
import io.example.cashier.domain.requests.cashier.YearTotalSalesCashier;
import io.example.cashier.model.CashierMonthSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.impl.CashierStatByIdRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class CashierStatByIdRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private CashierStatByIdRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CashierStatByIdRepositoryImpl(client);
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

    @Test
    void getMonthlyTotalSalesById_shouldReturnMappedList() {
        MonthTotalSalesCashier req = MonthTotalSalesCashier.builder().year(2024).month(6).cashierId(1).build();
        Row r = mock(Row.class);
        when(r.getString("year")).thenReturn("2024");
        when(r.getString("month")).thenReturn("June");
        when(r.getLong("total_sales")).thenReturn(150000L);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<CashierMonthTotalSales>> result = repository.getMonthlyTotalSalesById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalSales()).isEqualTo(150000L);
    }

    @Test
    void getYearlyTotalSalesById_shouldReturnMappedList() {
        YearTotalSalesCashier req = YearTotalSalesCashier.builder().year(2024).cashierId(1).build();
        Row r = mock(Row.class);
        when(r.getString("year")).thenReturn("2024");
        when(r.getLong("total_sales")).thenReturn(2500000L);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<CashierYearTotalSales>> result = repository.getYearlyTotalSalesById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalSales()).isEqualTo(2500000L);
    }

    @Test
    void getMonthlyCashierById_shouldReturnMappedList() {
        MonthCashierIdRequest req = MonthCashierIdRequest.builder().year(2024).cashierId(1).build();
        Row r = mock(Row.class);
        when(r.getString("month")).thenReturn("Jun");
        when(r.getInteger("cashier_id")).thenReturn(1);
        when(r.getString("cashier_name")).thenReturn("John");
        when(r.getInteger("order_count")).thenReturn(20);
        when(r.getLong("total_sales")).thenReturn(80000L);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<CashierMonthSales>> result = repository.getMonthlyCashierById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCashierName()).isEqualTo("John");
    }

    @Test
    void getYearlyCashierById_shouldReturnMappedList() {
        YearCashierIdRequest req = YearCashierIdRequest.builder().year(2024).cashierId(1).build();
        Row r = mock(Row.class);
        when(r.getString("year")).thenReturn("2024");
        when(r.getInteger("cashier_id")).thenReturn(1);
        when(r.getString("cashier_name")).thenReturn("John");
        when(r.getInteger("order_count")).thenReturn(100);
        when(r.getLong("total_sales")).thenReturn(980000L);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<CashierYearSales>> result = repository.getYearlyCashierById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCashierName()).isEqualTo("John");
    }
}
