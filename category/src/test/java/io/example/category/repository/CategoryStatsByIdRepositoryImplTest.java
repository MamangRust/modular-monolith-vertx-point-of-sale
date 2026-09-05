package io.example.category.repository;

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

import io.example.category.domain.requests.MonthTotalPriceCategory;
import io.example.category.domain.requests.YearPriceId;
import io.example.category.domain.requests.YearTotalPriceCategory;
import io.example.category.model.CategoryMonthPrice;
import io.example.category.model.CategoryMonthTotalPrice;
import io.example.category.model.CategoryYearPrice;
import io.example.category.model.CategoryYearTotalPrice;
import io.example.category.repository.impl.CategoryStatsByIdRepositoryImpl;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PreparedQuery;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

@ExtendWith(MockitoExtension.class)
class CategoryStatsByIdRepositoryImplTest {

    @Mock
    private Pool client;

    @Mock
    private PreparedQuery<RowSet<Row>> preparedQuery;

    private CategoryStatsByIdRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new CategoryStatsByIdRepositoryImpl(client);
    }

    @SuppressWarnings("unchecked")
    private RowSet<Row> mockRowSet(List<Row> rowsList) {
        RowSet<Row> rowSet = mock(RowSet.class);
        io.vertx.sqlclient.RowIterator<Row> rowIterator = mock(io.vertx.sqlclient.RowIterator.class);
        Iterator<Row> iterator = rowsList.iterator();
        when(rowIterator.hasNext()).thenAnswer(inv -> iterator.hasNext());
        when(rowIterator.next()).thenAnswer(inv -> iterator.next());
        when(rowSet.iterator()).thenReturn(rowIterator);
        return rowSet;
    }

    @Test
    void getMonthlyTotalPriceById_shouldReturnMappedList() {
        MonthTotalPriceCategory req = MonthTotalPriceCategory.builder().year(2024).month(6).categoryId(1).build();
        Row r = mock(Row.class);
        when(r.getString("year")).thenReturn("2024");
        when(r.getString("month")).thenReturn("June");
        when(r.getLong("total_revenue")).thenReturn(150000L);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<CategoryMonthTotalPrice>> result = repository.getMonthlyTotalPriceById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(150000L);
    }

    @Test
    void getYearlyTotalPriceById_shouldReturnMappedList() {
        YearTotalPriceCategory req = YearTotalPriceCategory.builder().year(2024).categoryId(1).build();
        Row r = mock(Row.class);
        when(r.getString("year")).thenReturn("2024");
        when(r.getLong("total_revenue")).thenReturn(2500000L);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<CategoryYearTotalPrice>> result = repository.getYearlyTotalPriceById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getTotalRevenue()).isEqualTo(2500000L);
    }

    @Test
    void getMonthlyCategoryById_shouldReturnMappedList() {
        YearPriceId req = YearPriceId.builder().year(2024).categoryId(1).build();
        Row r = mock(Row.class);
        when(r.getString("month")).thenReturn("Jun");
        when(r.getInteger("category_id")).thenReturn(1);
        when(r.getString("category_name")).thenReturn("Books");
        when(r.getInteger("order_count")).thenReturn(20);
        when(r.getInteger("items_sold")).thenReturn(40);
        when(r.getLong("total_revenue")).thenReturn(80000L);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<CategoryMonthPrice>> result = repository.getMonthlyCategoryById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCategoryName()).isEqualTo("Books");
    }

    @Test
    void getYearlyCategoryById_shouldReturnMappedList() {
        YearPriceId req = YearPriceId.builder().year(2024).categoryId(1).build();
        Row r = mock(Row.class);
        when(r.getString("year")).thenReturn("2024");
        when(r.getInteger("category_id")).thenReturn(1);
        when(r.getString("category_name")).thenReturn("Books");
        when(r.getInteger("order_count")).thenReturn(100);
        when(r.getInteger("items_sold")).thenReturn(200);
        when(r.getLong("total_revenue")).thenReturn(980000L);
        when(r.getInteger("unique_products_sold")).thenReturn(5);
        RowSet<Row> rowSet = mockRowSet(List.of(r));

        when(client.preparedQuery(anyString())).thenReturn(preparedQuery);
        when(preparedQuery.execute(any(Tuple.class))).thenReturn(Future.succeededFuture(rowSet));

        Future<List<CategoryYearPrice>> result = repository.getYearlyCategoryById(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getCategoryName()).isEqualTo("Books");
    }
}
