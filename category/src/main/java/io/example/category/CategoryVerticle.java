package io.example.category;

import io.example.category.repository.*;
import io.example.category.repository.impl.*;
import io.example.category.service.*;
import io.example.category.service.impl.*;
import io.example.category.handler.*;
import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategoryVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(CategoryVerticle.class);

  private TelemetryConfig telemetryConfig;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    JsonObject config = new JsonObject()
        .put("database", new JsonObject()
            .put("host", "localhost")
            .put("port", 5432)
            .put("database", "vertxdb")
            .put("user", "vertx")
            .put("password", "vertx")
            .put("pool_size", 5))
        .put("grpc_port", 50062)
        .put("service.name", "category-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new CategoryVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Category Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 50062");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy CategoryVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Initialize Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "category-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "category-service");

    // 2. Initialize Database Pool
    AppConfig cfg = AppConfig.from(rawConfig);
    var dbCfg = cfg.getDatabaseConfig();

    PgConnectOptions connectOptions = new PgConnectOptions()
        .setHost(dbCfg.getString("host", "localhost"))
        .setPort(dbCfg.getInteger("port", 5432))
        .setDatabase(dbCfg.getString("database", "vertxdb"))
        .setUser(dbCfg.getString("user", "vertx"))
        .setPassword(dbCfg.getString("password", "vertx"));

    PoolOptions poolOptions = new PoolOptions()
        .setMaxSize(dbCfg.getInteger("pool_size", 5));

    Pool pool = Pool.pool(vertx, connectOptions, poolOptions);

    // 3. Initialize caching (Redis)
    RedisAPI redisAPI = RedisConfig.createClient(vertx);
    RedisService redisService = new RedisService(redisAPI, openTelemetry);

    // 4. Initialize internal repositories
    CategoryStatsRepository statsRepo = new CategoryStatsRepositoryImpl(pool);
    CategoryStatsByIdRepository statByIdRepo = new CategoryStatsByIdRepositoryImpl(pool);
    CategoryStatsByMerchantRepository statByMerchantRepo = new CategoryStatsByMerchantRepositoryImpl(pool);
    CategoryQueryRepository queryRepo = new CategoryQueryRepositoryImpl(pool);
    CategoryCommandRepository commandRepo = new CategoryCommandRepositoryImpl(pool);

    // 5. Initialize internal services
    CategoryStatsService statsService = new CategoryStatsServiceImpl(statsRepo, redisService, tracingMetrics);
    CategoryStatsByIdService statsByIdService = new CategoryStatsByIdServiceImpl(statByIdRepo, redisService, tracingMetrics);
    CategoryStatsByMerchantService statsByMerchantService = new CategoryStatsByMerchantServiceImpl(statByMerchantRepo, redisService, tracingMetrics);
    CategoryQueryService queryService = new CategoryQueryServiceImpl(queryRepo, redisService, tracingMetrics);
    CategoryCommandService commandService = new CategoryCommandServiceImpl(commandRepo, queryRepo, redisService, tracingMetrics);

    // 6. Initialize Segmented Handlers
    CategoryQueryHandler queryHandler = new CategoryQueryHandler(statsService, statsByIdService, statsByMerchantService, queryService);
    CategoryCommandHandler cmdHandler = new CategoryCommandHandler(commandService);

    int tempPort = cfg.getGrpcPort() > 0 ? cfg.getGrpcPort() : 50062;
    String envGrpcPort = System.getenv("GRPC_CATEGORY_PORT");
    if (envGrpcPort != null) {
      tempPort = Integer.parseInt(envGrpcPort);
    }
    final int port = tempPort;

    startGrpcServer(queryHandler, cmdHandler, port)
        .onSuccess(v -> {
          log.info("CategoryVerticle fully initialized. Listening for gRPC on port {}", port);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Category gRPC server", err);
          startPromise.fail(err);
        });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (telemetryConfig != null) {
      telemetryConfig.shutdown();
    }
    stopPromise.complete();
  }

  private Future<Void> startGrpcServer(CategoryQueryHandler queryHandler, CategoryCommandHandler cmdHandler, int grpcPort) {
    GrpcServer grpcServer = GrpcServer.server(vertx);

    // Bind both handlers onto server
    queryHandler.bindAll(grpcServer);
    cmdHandler.bindAll(grpcServer);

    return vertx.createHttpServer()
        .requestHandler(grpcServer)
        .listen(grpcPort)
        .mapEmpty();
  }
}
