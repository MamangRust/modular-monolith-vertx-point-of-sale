package io.example.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.example.common.config.AppConfig;
import io.example.common.config.RedisConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.order.handler.OrderCommandHandler;
import io.example.order.handler.OrderQueryHandler;
import io.example.order.repository.CashierQueryRepository;
import io.example.order.repository.MerchantQueryRepository;
import io.example.order.repository.OrderCommandRepository;
import io.example.order.repository.OrderItemCommandRepository;
import io.example.order.repository.OrderItemQueryRepository;
import io.example.order.repository.OrderQueryRepository;
import io.example.order.repository.OrderStatByMerchantRepository;
import io.example.order.repository.OrderStatsRepository;
import io.example.order.repository.ProductCommandRepository;
import io.example.order.repository.ProductQueryRepository;
import io.example.order.repository.impl.CashierQueryRepositoryImpl;
import io.example.order.repository.impl.MerchantQueryRepositoryImpl;
import io.example.order.repository.impl.OrderCommandRepositoryImpl;
import io.example.order.repository.impl.OrderItemCommandRepositoryImpl;
import io.example.order.repository.impl.OrderItemQueryRepositoryImpl;
import io.example.order.repository.impl.OrderQueryRepositoryImpl;
import io.example.order.repository.impl.OrderStatByMerchantRepositoryImpl;
import io.example.order.repository.impl.OrderStatsRepositoryImpl;
import io.example.order.repository.impl.ProductCommandRepositoryImpl;
import io.example.order.repository.impl.ProductQueryRepositoryImpl;
import io.example.order.service.OrderCommandService;
import io.example.order.service.OrderQueryService;
import io.example.order.service.OrderStatByMerchantService;
import io.example.order.service.OrderStatsService;
import io.example.order.service.impl.OrderCommandServiceImpl;
import io.example.order.service.impl.OrderQueryServiceImpl;
import io.example.order.service.impl.OrderStatByMerchantServiceImpl;
import io.example.order.service.impl.OrderStatsServiceImpl;
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
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.redis.client.RedisAPI;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

public class OrderVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(OrderVerticle.class);

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
                .put("grpc_port", 8084)
                .put("service.name", "order-service");

        DeploymentOptions options = new DeploymentOptions().setConfig(config);

        vertx.deployVerticle(new OrderVerticle(), options)
                .onSuccess(id -> {
                    log.info("✅ Order Service successfully deployed! ID: {}", id);
                    log.info("🚀 gRPC Server running on port 8084");
                })
                .onFailure(err -> {
                    log.error("❌ Failed to deploy OrderVerticle", err);
                });
    }

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject rawConfig = config();

        // 1. Initialize Telemetry
        JsonObject telConfig = rawConfig.copy();
        if (!telConfig.containsKey("service.name")) {
            telConfig.put("service.name", "order-service");
        }
        telemetryConfig = new TelemetryConfig(telConfig);
        OpenTelemetry openTelemetry = telemetryConfig.initialize();
        TracingMetrics tracingMetrics = new TracingMetrics(openTelemetry, "order-service");

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

        // 3. Initialize caching (Redis)
        RedisAPI redisAPI = RedisConfig.createClient(vertx);
        RedisService redisService = new RedisService(redisAPI, openTelemetry);

        // 4. Initialize external gRPC clients
        grpcClient = GrpcClient.client(vertx);
        SocketAddress addrCashier = resolveGrpcAddress("CASHIER", "cashier", 50061);
        SocketAddress addrMerchant = resolveGrpcAddress("MERCHANT", "merchant", 8083);
        SocketAddress addrProduct = resolveGrpcAddress("PRODUCT", "product", 50062);

        CashierQueryRepository cashierRepo = new CashierQueryRepositoryImpl(
                new pb.cashier.VertxCashierServiceGrpcClient(grpcClient, addrCashier));
        MerchantQueryRepository merchantRepo = new MerchantQueryRepositoryImpl(
                new pb.merchant.VertxMerchantQueryServiceGrpcClient(grpcClient, addrMerchant));
        ProductQueryRepository productQueryRepo = new ProductQueryRepositoryImpl(
                new pb.product.VertxProductServiceGrpcClient(grpcClient, addrProduct));
        ProductCommandRepository productCmdRepo = new ProductCommandRepositoryImpl(
                new pb.product.VertxProductServiceGrpcClient(grpcClient, addrProduct),
                new pb.product.VertxProductCommandServiceGrpcClient(grpcClient, addrProduct));

        // 5. Initialize internal repositories
        OrderQueryRepository orderQueryRepo = new OrderQueryRepositoryImpl(pool);
        OrderCommandRepository orderCmdRepo = new OrderCommandRepositoryImpl(pool);
        OrderStatsRepository orderStatsRepo = new OrderStatsRepositoryImpl(pool);
        OrderStatByMerchantRepository orderStatByMerchantRepo = new OrderStatByMerchantRepositoryImpl(pool);

        SocketAddress addrOrderItem = resolveGrpcAddress("ORDER_ITEM", "order-item", 8085);
        OrderItemQueryRepository orderItemQueryRepo = new OrderItemQueryRepositoryImpl(
                new pb.order_item.VertxOrderItemServiceGrpcClient(grpcClient, addrOrderItem));
        OrderItemCommandRepository orderItemCmdRepo = new OrderItemCommandRepositoryImpl(
                new pb.order_item.VertxOrderItemCommandServiceGrpcClient(grpcClient, addrOrderItem));

        // 6. Initialize internal services
        OrderQueryService queryService = new OrderQueryServiceImpl(orderQueryRepo, redisService, tracingMetrics);
        OrderStatsService statsService = new OrderStatsServiceImpl(orderStatsRepo, redisService, tracingMetrics);
        OrderStatByMerchantService statByMerchantService = new OrderStatByMerchantServiceImpl(orderStatByMerchantRepo,
                redisService, tracingMetrics);
        OrderCommandService commandService = new OrderCommandServiceImpl(
                orderCmdRepo, orderQueryRepo, orderItemCmdRepo, orderItemQueryRepo,
                merchantRepo, cashierRepo, productQueryRepo, productCmdRepo,
                redisService, tracingMetrics);

        // 7. Initialize Handlers
        OrderQueryHandler queryHandler = new OrderQueryHandler(queryService, statsService, statByMerchantService);
        OrderCommandHandler commandHandler = new OrderCommandHandler(commandService);

        int tempPort = cfg.getGrpcPort();
        String envGrpcPort = System.getenv("GRPC_ORDER_PORT");
        if (envGrpcPort != null) {
            tempPort = Integer.parseInt(envGrpcPort);
        }
        final int port = tempPort > 0 ? tempPort : 8084;

        startGrpcServer(queryHandler, commandHandler, port)
                .onSuccess(v -> {
                    log.info("OrderVerticle fully initialized with CQRS. Listening for gRPC on port {}", port);
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

    private Future<Void> startGrpcServer(OrderQueryHandler queryHandler, OrderCommandHandler cmdHandler, int grpcPort) {
        GrpcServer grpcServer = GrpcServer.server(vertx);

        queryHandler.bindAll(grpcServer);
        cmdHandler.bindAll(grpcServer);

        return vertx.createHttpServer()
                .requestHandler(grpcServer)
                .listen(grpcPort)
                .mapEmpty();
    }
}
