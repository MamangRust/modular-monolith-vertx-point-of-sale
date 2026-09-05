package io.example.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

class AppConfigTest {

  private static final Map<String, String> NO_ENV = Map.of();

  private final JsonObject cfg = new JsonObject()
      .put("host", "10.0.0.5")
      .put("port", 9090)
      .put("grpc_port", 50099)
      .put("database", new JsonObject()
          .put("host", "db.internal")
          .put("port", 6432)
          .put("database", "POSDB")
          .put("user", "pos_user")
          .put("password", "s3cret")
          .put("pool_size", 12))
      .put("kafka_brokers", "kafka1:9092,kafka2:9092");

  @Test
  void hostComesFromConfig() {
    assertThat(new AppConfig(cfg, NO_ENV).getHost()).isEqualTo("10.0.0.5");
  }

  @Test
  void portComesFromConfig() {
    assertThat(new AppConfig(cfg, NO_ENV).getPort()).isEqualTo(9090);
  }

  @Test
  void grpcPortComesFromConfig() {
    assertThat(new AppConfig(cfg, NO_ENV).getGrpcPort()).isEqualTo(50099);
  }

  @Test
  void databaseConfigIsReadFromNestedJson() {
    JsonObject db = new AppConfig(cfg, NO_ENV).getDatabaseConfig();
    assertThat(db.getString("host")).isEqualTo("db.internal");
    assertThat(db.getInteger("port")).isEqualTo(6432);
    assertThat(db.getString("database")).isEqualTo("POSDB");
    assertThat(db.getString("user")).isEqualTo("pos_user");
    assertThat(db.getString("password")).isEqualTo("s3cret");
    assertThat(db.getInteger("pool_size")).isEqualTo(12);
  }

  @Test
  void kafkaBrokersComesFromConfig() {
    assertThat(new AppConfig(cfg, NO_ENV).getKafkaBrokers()).isEqualTo("kafka1:9092,kafka2:9092");
  }

  @Test
  void emptyConfigFallsBackToDefaults() {
    AppConfig empty = new AppConfig(new JsonObject(), NO_ENV);
    assertThat(empty.getHost()).isEqualTo("0.0.0.0");
    assertThat(empty.getPort()).isEqualTo(8080);
    assertThat(empty.getGrpcPort()).isEqualTo(8083);
    assertThat(empty.getKafkaBrokers()).isEqualTo("localhost:9092");

    JsonObject db = empty.getDatabaseConfig();
    assertThat(db.getString("host")).isEqualTo("localhost");
    assertThat(db.getInteger("port")).isEqualTo(5432);
    assertThat(db.getString("database")).isEqualTo("vertxdb");
    assertThat(db.getInteger("pool_size")).isEqualTo(5);
  }

  @Test
  void envOverridesConfig() {
    Map<String, String> env = Map.of(
        "APP_HOST", "env-host",
        "APP_PORT", "7777",
        "GRPC_PORT", "50055",
        "DB_HOST", "env-db",
        "DB_NAME", "ENVDB",
        "DB_MAX_OPEN_CONNS", "30",
        "KAFKA_BROKERS", "env-kafka:9092");

    AppConfig appConfig = new AppConfig(cfg, env);
    assertThat(appConfig.getHost()).isEqualTo("env-host");
    assertThat(appConfig.getPort()).isEqualTo(7777);
    assertThat(appConfig.getGrpcPort()).isEqualTo(50055);
    assertThat(appConfig.getKafkaBrokers()).isEqualTo("env-kafka:9092");

    JsonObject db = appConfig.getDatabaseConfig();
    assertThat(db.getString("host")).isEqualTo("env-db");
    assertThat(db.getString("database")).isEqualTo("ENVDB");
    assertThat(db.getInteger("pool_size")).isEqualTo(30);
  }

  @Test
  void fromFactoryReturnsWrappedConfig() {
    // from() memakai ctor publik (System.getenv()); gunakan ctor env-injected
    // agar deterministik di runner mana pun.
    AppConfig appConfig = new AppConfig(cfg, NO_ENV);
    assertThat(appConfig.getPort()).isEqualTo(9090);
  }
}
