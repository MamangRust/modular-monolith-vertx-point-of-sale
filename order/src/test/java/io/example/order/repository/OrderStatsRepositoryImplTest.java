package io.example.order.repository;

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

import io.example.order.domain.requests.MonthTotalRevenue;
import io.example.order.model.OrderMonth;
import io.example.order.model.OrderMonthTotalRevenue;
import io.example.order.model.OrderYear;
import io.example.order.model.OrderYearTotalRevenue;
import io.example.order.repository.impl.OrderStatsRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class OrderStatsRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private OrderStatsRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new OrderStatsRepositoryImpl(client);
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
    void getMonthlyTotalRevenue_shouldReturnMappedList() {
        MonthTotalRevenue req = new MonthTotalRevenue(2024, 6);
        Row r = mock(Row.class);
        when(r.getString("year")).thenReturn("2024");
        when(r.getString("month")).thenReturn("June");
        when(r.getInteger("total_revenue")).thenReturn(150000);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<OrderMonthTotalRevenue>> result = repository.getMonthlyTotalRevenue(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(150000);
    }

    @Test
    void getYearlyTotalRevenue_shouldReturnMappedList() {
        Row r = mock(Row.class);
        when(r.getString("year")).thenReturn("2024");
        when(r.getInteger("total_revenue")).thenReturn(2500000);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<OrderYearTotalRevenue>> result = repository.getYearlyTotalRevenue(2024);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(2500000);
    }

    @Test
    void getMonthlyOrder_shouldReturnMappedList() {
        Row r = mock(Row.class);
        when(r.getString("month")).thenReturn("Jan");
        when(r.getInteger("order_count")).thenReturn(20);
        when(r.getLong("total_revenue")).thenReturn(80000L);
        when(r.getInteger("total_items_sold")).thenReturn(45);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<OrderMonth>> result = repository.getMonthlyOrder(2024);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getMonth()).isEqualTo("Jan");
        assertThat(result.result().get(0).getOrderCount()).isEqualTo(20);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(80000L);
        assertThat(result.result().get(0).getTotalItemsSold()).isEqualTo(45);
    }

    @Test
    void getYearlyOrder_shouldReturnMappedList() {
        Row r = mock(Row.class);
        when(r.getString("year")).thenReturn("2024");
        when(r.getInteger("order_count")).thenReturn(100);
        when(r.getLong("total_revenue")).thenReturn(980000L);
        when(r.getInteger("total_items_sold")).thenReturn(250);
        when(r.getInteger("active_cashiers")).thenReturn(5);
        when(r.getInteger("unique_products_sold")).thenReturn(20);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<OrderYear>> result = repository.getYearlyOrder(2024);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getYear()).isEqualTo("2024");
        assertThat(result.result().get(0).getOrderCount()).isEqualTo(100);
        assertThat(result.result().get(0).getActiveCashiers()).isEqualTo(5);
        assertThat(result.result().get(0).getUniqueProductsSold()).isEqualTo(20);
    }
}
