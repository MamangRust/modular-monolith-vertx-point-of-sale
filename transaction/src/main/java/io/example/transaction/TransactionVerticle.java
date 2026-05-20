package io.example.transaction;

import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.transaction.handler.*;
import io.example.transaction.repository.*;
import io.example.transaction.repository.impl.*;
import io.example.transaction.service.*;
import io.example.transaction.service.impl.*;
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

public class TransactionVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(TransactionVerticle.class);

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
                .put("grpc_port", 8083)
                .put("service.name", "transaction-service");

        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle(new TransactionVerticle(), options)
                .onSuccess(id -> {
                    log.info("✅ Transaction Service successfully deployed! ID: {}", id);
                    log.info("🚀 gRPC Server running on port 8083");
                })
                .onFailure(err -> {
                    log.error("❌ Failed to deploy TransactionVerticle", err);
                });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject rawConfig = config();

        // 1. Initialize Telemetry
        JsonObject telConfig = rawConfig.copy();
        if (!telConfig.containsKey("service.name")) {
            telConfig.put("service.name", "transaction-service");
        }
        telemetryConfig = new TelemetryConfig(telConfig);
        OpenTelemetry openTelemetry = telemetryConfig.initialize();
        TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "transaction-service");

        // 2. Initialize Repositories
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

        TransactionQueryRepository queryRepo = new TransactionQueryRepositoryImpl(pool);
        TransactionCommandRepository cmdRepo = new TransactionCommandRepositoryImpl(pool);
        TransactionStatsRepository statsRepo = new TransactionStatsRepositoryImpl(pool);

        // 3. Initialize Caching
        RedisAPI redisAPI = RedisConfig.createClient(vertx);
        RedisService redisService = new RedisService(redisAPI, openTelemetry);

        // 4. Initialize Services
        TransactionQueryService queryService = new TransactionQueryServiceImpl(queryRepo, redisService, tracingMetrics);
        TransactionCommandService cmdService = new TransactionCommandServiceImpl(cmdRepo, redisService, tracingMetrics);
        TransactionStatsService statsService = new TransactionStatsServiceImpl(statsRepo, redisService, tracingMetrics);

        // 5. Initialize Handlers
        var queryHandler = new TransactionQueryHandler(queryService);
        var cmdHandler = new TransactionCommandHandler(cmdService);
        var statsAmountHandler = new TransactionStatsAmountHandler(statsService);
        var statsMethodHandler = new TransactionStatsMethodHandler(statsService);
        var statsStatusHandler = new TransactionStatsStatusHandler(statsService);

        int initialPort = cfg.getGrpcPort();
        final int port = initialPort == 0 ? 8083 : initialPort;

        startGrpcServer(queryHandler, cmdHandler, statsAmountHandler, statsMethodHandler, statsStatusHandler, port)
                .onSuccess(v -> {
                    log.info("TransactionVerticle fully initialized with CQRS. Listening for gRPC on port {}", port);
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

    private Future<Void> startGrpcServer(
            TransactionQueryHandler queryHandler,
            TransactionCommandHandler cmdHandler,
            TransactionStatsAmountHandler statsAmountHandler,
            TransactionStatsMethodHandler statsMethodHandler,
            TransactionStatsStatusHandler statsStatusHandler,
            int grpcPort) {
        GrpcServer grpcServer = GrpcServer.server(vertx);

        queryHandler.bindAll(grpcServer);
        cmdHandler.bindAll(grpcServer);
        statsAmountHandler.bindAll(grpcServer);
        statsMethodHandler.bindAll(grpcServer);
        statsStatusHandler.bindAll(grpcServer);

        return vertx.createHttpServer()
                .requestHandler(grpcServer)
                .listen(grpcPort)
                .mapEmpty();
    }
}
