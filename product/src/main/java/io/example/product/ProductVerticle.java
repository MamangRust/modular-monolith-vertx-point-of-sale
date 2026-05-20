package io.example.product;

import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.product.handler.ProductCommandHandler;
import io.example.product.handler.ProductQueryHandler;
import io.example.product.repository.CategoryQueryRepository;
import io.example.product.repository.MerchantQueryRepository;
import io.example.product.repository.ProductCommandRepository;
import io.example.product.repository.ProductQueryRepository;
import io.example.product.repository.impl.CategoryQueryRepositoryImpl;
import io.example.product.repository.impl.MerchantQueryRepositoryImpl;
import io.example.product.repository.impl.ProductCommandRepositoryImpl;
import io.example.product.repository.impl.ProductQueryRepositoryImpl;
import io.example.product.service.ProductCommandService;
import io.example.product.service.ProductQueryService;
import io.example.product.service.impl.ProductCommandServiceImpl;
import io.example.product.service.impl.ProductQueryServiceImpl;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProductVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ProductVerticle.class);

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
                .put("grpc_port", 8086)
                .put("service.name", "product-service");

        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle(new ProductVerticle(), options)
                .onSuccess(id -> {
                    log.info("✅ Product Service successfully deployed! ID: {}", id);
                    log.info("🚀 gRPC Server running on port 8086");
                })
                .onFailure(err -> {
                    log.error("❌ Failed to deploy ProductVerticle", err);
                });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject rawConfig = config();

        // 1. Initialize Telemetry
        JsonObject telConfig = rawConfig.copy();
        if (!telConfig.containsKey("service.name")) {
            telConfig.put("service.name", "product-service");
        }
        telemetryConfig = new TelemetryConfig(telConfig);
        OpenTelemetry openTelemetry = telemetryConfig.initialize();
        TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "product-service");

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

        // 4. Initialize external gRPC client stubs
        grpcClient = GrpcClient.client(vertx);
        SocketAddress addrMerchant = resolveGrpcAddress("MERCHANT", "merchant", 8083);
        SocketAddress addrCategory = resolveGrpcAddress("CATEGORY", "category", 8082);

        CategoryQueryRepository categoryRepo = new CategoryQueryRepositoryImpl(
                new pb.category.VertxCategoryServiceGrpcClient(grpcClient, addrCategory)
        );
        MerchantQueryRepository merchantRepo = new MerchantQueryRepositoryImpl(
                new pb.merchant.VertxMerchantQueryServiceGrpcClient(grpcClient, addrMerchant)
        );

        // 5. Initialize internal repositories
        ProductQueryRepository queryRepo = new ProductQueryRepositoryImpl(pool);
        ProductCommandRepository cmdRepo = new ProductCommandRepositoryImpl(pool);

        // 6. Initialize Services
        ProductQueryService queryService = new ProductQueryServiceImpl(queryRepo, redisService, tracingMetrics);
        ProductCommandService cmdService = new ProductCommandServiceImpl(cmdRepo, categoryRepo, merchantRepo, redisService, tracingMetrics);

        // 7. Initialize Handlers
        var queryHandler = new ProductQueryHandler(queryService);
        var cmdHandler = new ProductCommandHandler(cmdService);

        int port = cfg.getGrpcPort();

        startGrpcServer(queryHandler, cmdHandler, port)
                .onSuccess(v -> {
                    log.info("ProductVerticle fully initialized with CQRS. Listening for gRPC on port {}", port);
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

    private Future<Void> startGrpcServer(ProductQueryHandler queryHandler, ProductCommandHandler cmdHandler, int grpcPort) {
        GrpcServer grpcServer = GrpcServer.server(vertx);

        queryHandler.bindAll(grpcServer);
        cmdHandler.bindAll(grpcServer);

        return vertx.createHttpServer()
                .requestHandler(grpcServer)
                .listen(grpcPort)
                .mapEmpty();
    }
}
