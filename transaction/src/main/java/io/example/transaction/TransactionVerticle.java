package io.example.transaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.common.chaos.ChaosGrpcServerInterceptor;
import io.example.common.chaos.ChaosManager;
import io.example.common.grpc.GrpcServerBinder;
import io.example.common.chaos.ChaosSqlProxy;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.example.transaction.handler.TransactionCommandHandler;
import io.example.transaction.handler.TransactionQueryHandler;
import io.example.transaction.handler.TransactionStatsMethodHandler;
import io.example.transaction.handler.TransactionStatsStatusHandler;
import io.example.transaction.repository.MerchantQueryRepository;
import io.example.transaction.repository.TransactionCommandRepository;
import io.example.transaction.repository.TransactionQueryRepository;
import io.example.transaction.repository.TransactionStatsRepository;
import io.example.transaction.repository.impl.MerchantQueryRepositoryImpl;
import io.example.transaction.repository.impl.TransactionCommandRepositoryImpl;
import io.example.transaction.repository.impl.TransactionQueryRepositoryImpl;
import io.example.transaction.repository.impl.TransactionStatsRepositoryImpl;
import io.example.transaction.service.TransactionCommandService;
import io.example.transaction.service.TransactionQueryService;
import io.example.transaction.service.TransactionStatsService;
import io.example.transaction.service.impl.TransactionCommandServiceImpl;
import io.example.transaction.service.impl.TransactionQueryServiceImpl;
import io.example.transaction.service.impl.TransactionStatsServiceImpl;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class TransactionVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(TransactionVerticle.class);

    private TelemetryConfig telemetryConfig;
    private GrpcClient grpcClient;
    private KafkaService kafkaService;

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
        ChaosManager chaosManager = new ChaosManager();
        chaosManager.startWatcher(vertx);
        Pool chaosPool = ChaosSqlProxy.wrap(pool, chaosManager, vertx);

        TransactionQueryRepository queryRepo = new TransactionQueryRepositoryImpl(chaosPool);
        TransactionCommandRepository cmdRepo = new TransactionCommandRepositoryImpl(chaosPool);
        TransactionStatsRepository statsRepo = new TransactionStatsRepositoryImpl(chaosPool);

        // 3. Initialize Caching
        RedisAPI redisAPI = RedisConfig.createClient(vertx);
        RedisService redisService = new RedisService(redisAPI, openTelemetry);

        // 4. Initialize Kafka Service and external gRPC clients
        Map<String, String> kafkaConfig = new HashMap<>();
        kafkaConfig.put("bootstrap.servers", cfg.getKafkaBrokers());
        kafkaConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaConfig.put("acks", "1");
        KafkaProducer<String, String> producer = KafkaProducer.create(vertx, kafkaConfig);
        this.kafkaService = new KafkaService(producer);

        grpcClient = GrpcClient.client(vertx);
        SocketAddress addrMerchant = SocketAddress.inetSocketAddress(
                Integer.parseInt(System.getenv().getOrDefault("GRPC_MERCHANT_PORT", "50054")),
                System.getenv().getOrDefault("GRPC_MERCHANT_ADDR", "merchant"));
        MerchantQueryRepository merchantQueryRepo = new MerchantQueryRepositoryImpl(
                new pb.merchant.VertxMerchantQueryServiceGrpcClient(grpcClient, addrMerchant));

        // 5. Initialize Services
        TransactionQueryService queryService = new TransactionQueryServiceImpl(queryRepo, redisService, tracingMetrics);
        TransactionCommandService cmdService = new TransactionCommandServiceImpl(cmdRepo, queryRepo, redisService, tracingMetrics, kafkaService, merchantQueryRepo);
        TransactionStatsService statsService = new TransactionStatsServiceImpl(statsRepo, redisService, tracingMetrics);

        // 6. Initialize Handlers
        var queryHandler = new TransactionQueryHandler(queryService);
        var cmdHandler = new TransactionCommandHandler(cmdService);
        var statsMethodHandler = new TransactionStatsMethodHandler(statsService);
        var statsStatusHandler = new TransactionStatsStatusHandler(statsService);

        int initialPort = cfg.getGrpcPort();
        final int port = initialPort == 0 ? 8083 : initialPort;

        startGrpcServer(queryHandler, cmdHandler, statsMethodHandler, statsStatusHandler, port, chaosManager)
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
        if (kafkaService != null) {
            kafkaService.close();
        }
        if (grpcClient != null) {
            grpcClient.close();
        }
        stopPromise.complete();
    }

    private Future<Void> startGrpcServer(
            TransactionQueryHandler queryHandler,
            TransactionCommandHandler cmdHandler,
            TransactionStatsMethodHandler statsMethodHandler,
            TransactionStatsStatusHandler statsStatusHandler,
            int grpcPort,
            ChaosManager chaosManager) {
        GrpcServer grpcServer = GrpcServer.server(vertx);

        GrpcServerBinder.bindAll(grpcServer, queryHandler);
        GrpcServerBinder.bindAll(grpcServer, cmdHandler);
        GrpcServerBinder.bindAll(grpcServer, statsMethodHandler);
        GrpcServerBinder.bindAll(grpcServer, statsStatusHandler);

        Handler<HttpServerRequest> chaosHandler =
            new ChaosGrpcServerInterceptor(grpcServer, chaosManager, vertx);

        return vertx.createHttpServer()
                .requestHandler(chaosHandler)
                .listen(grpcPort)
                .mapEmpty();
    }
}
