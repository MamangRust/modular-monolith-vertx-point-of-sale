package io.example.cashier.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.example.cashier.domain.requests.cashier.MonthTotalSales;
import io.example.cashier.model.CashierMonthTotalSales;
import io.example.cashier.model.CashierYearTotalSales;
import io.example.cashier.repository.impl.CashierStatsRepositoryImpl;
import io.example.common.config.FlywayConfig;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;

/**
 * Integration test running the real stats SQL against a disposable PostgreSQL
 * container (Testcontainers). Proves the stats queries work against a real
 * engine with the actual V1-V14 schema — no mocks involved.
 */
@Testcontainers(disabledWithoutDocker = true)
class CashierStatsRepositoryIntegrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
      .withDatabaseName("POINT_OF_SALE")
      .withUsername("DRAGON")
      .withPassword("DRAGON");

  private static Vertx vertx;
  private static PgPool pool;
  private static CashierStatsRepositoryImpl statsRepo;

  private static long cashierId;

  @BeforeAll
  static void setUp() {
    vertx = Vertx.vertx();
    PgConnectOptions connect = new PgConnectOptions()
        .setHost(POSTGRES.getHost())
        .setPort(POSTGRES.getMappedPort(5432))
        .setDatabase(POSTGRES.getDatabaseName())
        .setUser(POSTGRES.getUsername())
        .setPassword(POSTGRES.getPassword());
    pool = PgPool.pool(vertx, connect, new PoolOptions().setMaxSize(5));

    // Apply the real V1-V14 migrations on the fresh volume.
    FlywayConfig.runMigrations(connect);

    seed();
    statsRepo = new CashierStatsRepositoryImpl(pool);
  }

  private static void seed() {
    long userId = pool.withTransaction(conn -> conn
        .preparedQuery(
            "INSERT INTO users (firstname, lastname, email, password) VALUES ('IT', 'Admin', 'it-admin@example.com', 'x') RETURNING user_id")
        .execute()
        .map(rs -> rs.iterator().next().getLong("user_id")))
        .toCompletionStage().toCompletableFuture().join();

    long merchantId = pool.withTransaction(conn -> conn
        .preparedQuery(
            "INSERT INTO merchants (user_id, name, status) VALUES ($1, 'IT Merchant', 'active') RETURNING merchant_id")
        .execute(Tuple.of(userId))
        .map(rs -> rs.iterator().next().getLong("merchant_id")))
        .toCompletionStage().toCompletableFuture().join();

    cashierId = pool.withTransaction(conn -> conn
        .preparedQuery(
            "INSERT INTO cashiers (merchant_id, user_id, name) VALUES ($1, $2, 'IT Cashier') RETURNING cashier_id")
        .execute(Tuple.of(merchantId, userId))
        .map(rs -> rs.iterator().next().getLong("cashier_id")))
        .toCompletionStage().toCompletableFuture().join();

    // Two orders in Aug 2026: total = 30000 + 20000 = 50000.
    // One order in Aug 2025 (same month, previous year) must NOT inflate 2026.
    pool.withTransaction(conn -> conn
        .preparedQuery(
            "INSERT INTO orders (merchant_id, cashier_id, total_price, created_at) VALUES "
                + "($1, $2, 30000, '2026-08-05 10:00:00'), "
                + "($1, $2, 20000, '2026-08-20 14:30:00'), "
                + "($1, $2, 99999, '2025-08-15 09:00:00'), "
                + "($1, $2, 777, '2026-07-01 08:00:00')")
        .execute(Tuple.of(merchantId, cashierId)))
        .toCompletionStage().toCompletableFuture().join();
  }

  @AfterAll
  static void tearDown() {
    if (pool != null) {
      pool.close().toCompletionStage().toCompletableFuture().join();
    }
    if (vertx != null) {
      vertx.close().toCompletionStage().toCompletableFuture().join();
    }
  }

  @Test
  void getMonthlyTotalSales_shouldSumOnlyOrdersInRequestedMonth() {
    List<CashierMonthTotalSales> result = statsRepo
        .getMonthlyTotalSales(MonthTotalSales.builder().year(2026).month(8).build())
        .toCompletionStage().toCompletableFuture().join();

    // Filter by year only — month-name rendering (TO_CHAR FMMonth) is
    // locale-dependent, so never assert on it.
    CashierMonthTotalSales y2026 = result.stream()
        .filter(r -> "2026".equals(r.getYear()))
        .findFirst().orElseThrow();

    assertThat(y2026.getTotalSales()).isEqualTo(50000L);
    // The 2025-08 order must land on its own row, not inflate 2026.
    CashierMonthTotalSales y2025 = result.stream()
        .filter(r -> "2025".equals(r.getYear()))
        .findFirst().orElseThrow();
    assertThat(y2025.getTotalSales()).isEqualTo(99999L);
  }

  @Test
  void getYearlyTotalSales_shouldSumByCalendarYear() {
    List<CashierYearTotalSales> result = statsRepo
        .getYearlyTotalSales(2026)
        .toCompletionStage().toCompletableFuture().join();

    // 2026: 50000 (Aug) + 777 (Jul) = 50777; 2025: 99999.
    CashierYearTotalSales y2026 = result.stream()
        .filter(r -> "2026".equals(r.getYear()))
        .findFirst().orElseThrow();
    CashierYearTotalSales y2025 = result.stream()
        .filter(r -> "2025".equals(r.getYear()))
        .findFirst().orElseThrow();

    assertThat(y2026.getTotalSales()).isEqualTo(50777L);
    assertThat(y2025.getTotalSales()).isEqualTo(99999L);
  }
}
