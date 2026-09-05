package io.example.merchant;

import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.example.merchant.handler.MerchantCommandHandler;
import io.example.merchant.handler.MerchantQueryHandler;
import io.example.merchant.handler.MerchantDocumentCommandHandler;
import io.example.merchant.handler.MerchantDocumentQueryHandler;
import io.example.merchant.repository.MerchantCommandRepository;
import io.example.merchant.repository.MerchantQueryRepository;
import io.example.merchant.repository.MerchantDocumentCommandRepository;
import io.example.merchant.repository.MerchantDocumentQueryRepository;
import io.example.merchant.repository.UserQueryRepository;
import io.example.merchant.repository.impl.MerchantCommandRepositoryImpl;
import io.example.merchant.repository.impl.MerchantQueryRepositoryImpl;
import io.example.merchant.repository.impl.MerchantDocumentCommandRepositoryImpl;
import io.example.merchant.repository.impl.MerchantDocumentQueryRepositoryImpl;
import io.example.merchant.repository.impl.UserQueryRepositoryImpl;
import io.example.merchant.service.MerchantCommandService;
import io.example.merchant.service.MerchantQueryService;
import io.example.merchant.service.MerchantDocumentCommandService;
import io.example.merchant.service.MerchantDocumentQueryService;
import io.example.merchant.service.impl.MerchantCommandServiceImpl;
import io.example.merchant.service.impl.MerchantQueryServiceImpl;
import io.example.merchant.service.impl.MerchantDocumentCommandServiceImpl;
import io.example.merchant.service.impl.MerchantDocumentQueryServiceImpl;
import io.example.common.chaos.ChaosGrpcServerInterceptor;
import io.example.common.chaos.ChaosManager;
import io.example.common.grpc.GrpcServerBinder;
import io.example.common.chaos.ChaosSqlProxy;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class MerchantVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(MerchantVerticle.class);

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
                .put("service.name", "merchant-service");

        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle(new MerchantVerticle(), options)
                .onSuccess(id -> {
                    log.info("✅ Merchant Service successfully deployed! ID: {}", id);
                    log.info("🚀 gRPC Server running on port 8083");
                })
                .onFailure(err -> {
                    log.error("❌ Failed to deploy MerchantVerticle", err);
                });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject rawConfig = config();

        // 1. Initialize Telemetry
        JsonObject telConfig = rawConfig.copy();
        if (!telConfig.containsKey("service.name")) {
            telConfig.put("service.name", "merchant-service");
        }
        telemetryConfig = new TelemetryConfig(telConfig);
        OpenTelemetry openTelemetry = telemetryConfig.initialize();
        TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "merchant-service");

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
        ChaosManager chaosManager = new ChaosManager();
        chaosManager.startWatcher(vertx);
        Pool chaosPool = ChaosSqlProxy.wrap(pool, chaosManager, vertx);

        // 3. Initialize caching (Redis)
        RedisAPI redisAPI = RedisConfig.createClient(vertx);
        RedisService redisService = new RedisService(redisAPI, openTelemetry);

        // 4. Initialize Kafka Service
        Map<String, String> kafkaConfig = new HashMap<>();
        kafkaConfig.put("bootstrap.servers", cfg.getKafkaBrokers());
        kafkaConfig.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaConfig.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaConfig.put("acks", "1");
        KafkaProducer<String, String> producer = KafkaProducer.create(vertx, kafkaConfig);
        this.kafkaService = new KafkaService(producer);

        // 5. Initialize external gRPC client to communicate with User service
        grpcClient = GrpcClient.client(vertx);
        SocketAddress addrUser = resolveGrpcAddress("USER", "user", 8083);

        UserQueryRepository userQueryRepository = new UserQueryRepositoryImpl(
                new pb.user.VertxUserQueryServiceGrpcClient(grpcClient, addrUser)
        );

        // 5. Initialize internal repositories
        MerchantQueryRepository merchantQueryRepository = new MerchantQueryRepositoryImpl(chaosPool);
        MerchantCommandRepository merchantCommandRepository = new MerchantCommandRepositoryImpl(chaosPool);
        MerchantDocumentQueryRepository documentQueryRepository = new MerchantDocumentQueryRepositoryImpl(chaosPool);
        MerchantDocumentCommandRepository documentCommandRepository = new MerchantDocumentCommandRepositoryImpl(chaosPool);

        // 6. Initialize internal services
        MerchantQueryService merchantQueryService = new MerchantQueryServiceImpl(merchantQueryRepository, redisService, tracingMetrics);
        MerchantCommandService merchantCommandService = new MerchantCommandServiceImpl(merchantCommandRepository, merchantQueryRepository, userQueryRepository, redisService, tracingMetrics, kafkaService);
        MerchantDocumentQueryService documentQueryService = new MerchantDocumentQueryServiceImpl(documentQueryRepository, redisService, tracingMetrics);
        MerchantDocumentCommandService documentCommandService = new MerchantDocumentCommandServiceImpl(documentCommandRepository, documentQueryRepository, merchantQueryRepository, redisService, tracingMetrics, kafkaService);

        // 7. Initialize Segmented Handlers
        MerchantQueryHandler merchantQueryHandler = new MerchantQueryHandler(merchantQueryService);
        MerchantCommandHandler merchantCommandHandler = new MerchantCommandHandler(merchantCommandService);
        MerchantDocumentQueryHandler documentQueryHandler = new MerchantDocumentQueryHandler(documentQueryService);
        MerchantDocumentCommandHandler documentCommandHandler = new MerchantDocumentCommandHandler(documentCommandService);

        int tempPort = cfg.getGrpcPort();
        String envGrpcPort = System.getenv("GRPC_MERCHANT_PORT");
        if (envGrpcPort != null) {
            tempPort = Integer.parseInt(envGrpcPort);
        }
        final int port = tempPort;

        startGrpcServer(merchantQueryHandler, merchantCommandHandler, documentQueryHandler, documentCommandHandler, port, chaosManager)
                .onSuccess(v -> {
                    log.info("MerchantVerticle fully initialized with CQRS. Listening for gRPC on port {}", port);
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
        if (grpcClient != null) {
            grpcClient.close();
        }
        if (kafkaService != null) {
            kafkaService.close();
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

    private Future<Void> startGrpcServer(
            MerchantQueryHandler queryHandler,
            MerchantCommandHandler cmdHandler,
            MerchantDocumentQueryHandler docQueryHandler,
            MerchantDocumentCommandHandler docCmdHandler,
            int grpcPort,
            ChaosManager chaosManager) {
        GrpcServer grpcServer = GrpcServer.server(vertx);

        // Bind all 4 handlers on the unified GrpcServer
        GrpcServerBinder.bindAll(grpcServer, queryHandler);
        GrpcServerBinder.bindAll(grpcServer, cmdHandler);
        GrpcServerBinder.bindAll(grpcServer, docQueryHandler);
        GrpcServerBinder.bindAll(grpcServer, docCmdHandler);

        Handler<HttpServerRequest> chaosHandler =
            new ChaosGrpcServerInterceptor(grpcServer, chaosManager, vertx);

        return vertx.createHttpServer()
                .requestHandler(chaosHandler)
                .listen(grpcPort)
                .mapEmpty();
    }
}
