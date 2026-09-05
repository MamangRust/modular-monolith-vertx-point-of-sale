package io.example.common.config;

import java.util.Map;

import io.vertx.core.json.JsonObject;

public class AppConfig {

  private final JsonObject config;
  private final Map<String, String> env;

  public AppConfig(JsonObject config) {
    this(config, System.getenv());
  }

  /**
   * Package-private constructor with an injectable environment map, so unit
   * tests can control/clear env values deterministically.
   */
  AppConfig(JsonObject config, Map<String, String> env) {
    this.config = config;
    this.env = env;
  }

  public String getHost() {
    return env.getOrDefault("APP_HOST", config.getString("host", "0.0.0.0"));
  }

  public int getPort() {
    String envPort = env.get("APP_PORT");
    if (envPort != null) return Integer.parseInt(envPort);
    return config.getInteger("port", 8080);
  }

  public int getGrpcPort() {
    String envPort = env.get("GRPC_PORT");
    if (envPort != null) return Integer.parseInt(envPort);
    return config.getInteger("grpc_port", 8083);
  }

  public JsonObject getDatabaseConfig() {
    JsonObject db = config.getJsonObject("database", new JsonObject());

    String host = env.getOrDefault("DB_HOST", db.getString("host", "localhost"));
    int port = env.get("DB_PORT") != null ? Integer.parseInt(env.get("DB_PORT")) : db.getInteger("port", 5432);
    String database = env.getOrDefault("DB_NAME", db.getString("database", "vertxdb"));
    String user = env.getOrDefault("DB_USERNAME", db.getString("user", "vertx"));
    String password = env.getOrDefault("DB_PASSWORD", db.getString("password", "vertx"));
    int poolSize = env.get("DB_MAX_OPEN_CONNS") != null ? Integer.parseInt(env.get("DB_MAX_OPEN_CONNS")) : db.getInteger("pool_size", 5);

    return new JsonObject()
      .put("host", host)
      .put("port", port)
      .put("database", database)
      .put("user", user)
      .put("password", password)
      .put("pool_size", poolSize);
  }

  public String getKafkaBrokers() {
    return env.getOrDefault("KAFKA_BROKERS", config.getString("kafka_brokers", "localhost:9092"));
  }

  public static AppConfig from(JsonObject config) {
    return new AppConfig(config);
  }
}
