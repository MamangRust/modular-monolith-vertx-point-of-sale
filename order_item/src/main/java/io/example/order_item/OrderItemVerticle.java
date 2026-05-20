package io.example.order_item;

import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.order_item.handler.OrderItemCommandHandler;
import io.example.order_item.handler.OrderItemQueryHandler;
import io.example.order_item.repository.OrderItemCommandRepository;
import io.example.order_item.repository.OrderItemQueryRepository;
import io.example.order_item.repository.impl.OrderItemCommandRepositoryImpl;
import io.example.order_item.repository.impl.OrderItemQueryRepositoryImpl;
import io.example.order_item.service.OrderItemCommandService;
import io.example.order_item.service.OrderItemQueryService;
import io.example.order_item.service.impl.OrderItemCommandServiceImpl;
import io.example.order_item.service.impl.OrderItemQueryServiceImpl;
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

public class OrderItemVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(OrderItemVerticle.class);

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
                .put("grpc_port", 8085)
                .put("service.name", "order-item-service");

        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle(new OrderItemVerticle(), options)
                .onSuccess(id -> {
                    log.info("✅ OrderItem Service successfully deployed! ID: {}", id);
                    log.info("🚀 gRPC Server running on port 8085");
                })
                .onFailure(err -> {
                    log.error("❌ Failed to deploy OrderItemVerticle", err);
                });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject rawConfig = config();

        // 1. Initialize Telemetry
        JsonObject telConfig = rawConfig.copy();
        if (!telConfig.containsKey("service.name")) {
            telConfig.put("service.name", "order-item-service");
        }
        telemetryConfig = new TelemetryConfig(telConfig);
        OpenTelemetry openTelemetry = telemetryConfig.initialize();
        TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "order-item-service");

        // 2. Initialize Repositories (Database)
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

        OrderItemQueryRepository queryRepo = new OrderItemQueryRepositoryImpl(pool);
        OrderItemCommandRepository cmdRepo = new OrderItemCommandRepositoryImpl(pool);

        // 3. Initialize caching (Redis)
        RedisAPI redisAPI = RedisConfig.createClient(vertx);
        RedisService redisService = new RedisService(redisAPI, openTelemetry);

        // 4. Initialize Services
        OrderItemQueryService queryService = new OrderItemQueryServiceImpl(queryRepo, redisService, tracingMetrics);
        OrderItemCommandService cmdService = new OrderItemCommandServiceImpl(cmdRepo, redisService, tracingMetrics);

        // 5. Initialize Handlers
        OrderItemQueryHandler queryHandler = new OrderItemQueryHandler(queryService);
        OrderItemCommandHandler cmdHandler = new OrderItemCommandHandler(cmdService);

        int tempPort = cfg.getGrpcPort();
        String envGrpcPort = System.getenv("GRPC_ORDER_ITEM_PORT");
        if (envGrpcPort != null) {
            tempPort = Integer.parseInt(envGrpcPort);
        }
        final int port = tempPort > 0 ? tempPort : 8085;

        startGrpcServer(queryHandler, cmdHandler, port)
                .onSuccess(v -> {
                    log.info("OrderItemVerticle fully initialized with CQRS. Listening for gRPC on port {}", port);
                    startPromise.complete();
                })
                .onFailure(err -> {
                    log.error("Failed to bind gRPC server", err);
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

    private Future<Void> startGrpcServer(OrderItemQueryHandler queryHandler, OrderItemCommandHandler cmdHandler, int grpcPort) {
        GrpcServer grpcServer = GrpcServer.server(vertx);

        queryHandler.bindAll(grpcServer);
        cmdHandler.bindAll(grpcServer);

        return vertx.createHttpServer()
                .requestHandler(grpcServer)
                .listen(grpcPort)
                .mapEmpty();
    }
}
