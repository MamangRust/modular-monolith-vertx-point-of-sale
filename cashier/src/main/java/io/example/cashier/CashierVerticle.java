package io.example.cashier;
import io.example.cashier.repository.*;
import io.example.cashier.repository.impl.*;
import io.example.cashier.service.*;
import io.example.cashier.service.impl.*;
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
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CashierVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(CashierVerticle.class);

  private TelemetryConfig telemetryConfig;
  private GrpcClient grpcClient;

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
        .put("grpc_port", 50061)
        .put("service.name", "cashier-service");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new CashierVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ Cashier Service successfully deployed! ID: {}", id);
          log.info("🚀 gRPC Server running on port 50061");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy CashierVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Initialize Telemetry
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "cashier-service");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();
    TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "cashier-service");

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

    // 4. Initialize external gRPC Clients
    grpcClient = GrpcClient.client(vertx);
    SocketAddress addrUser = resolveGrpcAddress("USER", "user", 50055);
    SocketAddress addrMerchant = resolveGrpcAddress("MERCHANT", "merchant", 50054);

    UserQueryRepository userQueryRepository = new UserQueryRepositoryImpl(
        new pb.user.VertxUserQueryServiceGrpcClient(grpcClient, addrUser)
    );
    MerchantQueryRepository merchantQueryRepository = new MerchantQueryRepositoryImpl(
        new pb.merchant.VertxMerchantQueryServiceGrpcClient(grpcClient, addrMerchant)
    );

    // 5. Initialize internal repositories
    CashierStatsRepository statsRepo = new CashierStatsRepositoryImpl(pool);
    CashierStatByIdRepository statByIdRepo = new CashierStatByIdRepositoryImpl(pool);
    CashierStatByMerchantRepository statByMerchantRepo = new CashierStatByMerchantRepositoryImpl(pool);
    CashierQueryRepository queryRepo = new CashierQueryRepositoryImpl(pool);
    CashierCommandRepository commandRepo = new CashierCommandRepositoryImpl(pool);

    // 6. Initialize internal services
    CashierStatsService statsService = new CashierStatsServiceImpl(statsRepo, redisService, tracingMetrics);
    CashierStatsByIdService statsByIdService = new CashierStatsByIdServiceImpl(statByIdRepo, redisService, tracingMetrics);
    CashierStatsByMerchant statsByMerchantService = new CashierStatsByMerchantImpl(statByMerchantRepo, redisService, tracingMetrics);
    CashierQueryService queryService = new CashierQueryServiceImpl(queryRepo, redisService, tracingMetrics);
    CashierCommandService commandService = new CashierCommandServiceImpl(commandRepo, merchantQueryRepository, userQueryRepository, redisService, tracingMetrics);

    // 7. Initialize Segmented Handlers
    io.example.cashier.handler.CashierQueryHandler queryHandler = new io.example.cashier.handler.CashierQueryHandler(statsService, statsByIdService, statsByMerchantService, queryService);
    io.example.cashier.handler.CashierCommandHandler cmdHandler = new io.example.cashier.handler.CashierCommandHandler(commandService);

    int tempPort = cfg.getGrpcPort();
    String envGrpcPort = System.getenv("GRPC_CASHIER_PORT");
    if (envGrpcPort != null) {
      tempPort = Integer.parseInt(envGrpcPort);
    }
    final int port = tempPort;

    startGrpcServer(queryHandler, cmdHandler, port)
        .onSuccess(v -> {
          log.info("CashierVerticle fully initialized. Listening for gRPC on port {}", port);
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("Failed to bind Cashier gRPC server", err);
          startPromise.fail(err);
        });
  }

  @Override
  public void stop(Promise<Void> stopPromise) {
    if (telemetryConfig != null) {
      telemetryConfig.shutdown();
    }
    if (grpcClient != null) {
      grpcClient.close();
    }
    stopPromise.complete();
  }

  private SocketAddress resolveGrpcAddress(String envPrefix, String defaultHost, int defaultPort) {
    String host = System.getenv().getOrDefault("GRPC_" + envPrefix + "_ADDR", defaultHost);
    int port = System.getenv("GRPC_" + envPrefix + "_PORT") != null
        ? Integer.parseInt(System.getenv("GRPC_" + envPrefix + "_PORT"))
        : defaultPort;

    if (host.contains(":")) {
      String[] parts = host.split(":");
      host = parts[0];
      port = Integer.parseInt(parts[1]);
    }

    log.info("📍 Service {} mapped to {}:{}", envPrefix, host, port);
    return SocketAddress.inetSocketAddress(port, host);
  }

  private Future<Void> startGrpcServer(io.example.cashier.handler.CashierQueryHandler queryHandler, io.example.cashier.handler.CashierCommandHandler cmdHandler, int grpcPort) {
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
