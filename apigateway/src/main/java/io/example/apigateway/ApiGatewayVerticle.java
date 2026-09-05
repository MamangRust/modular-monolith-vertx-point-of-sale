package io.example.apigateway;

import io.example.apigateway.handler.*;
import io.example.apigateway.observability.GatewayMetricsMiddleware;
import io.example.apigateway.resilience.RateLimitHandler;
import io.example.apigateway.resilience.ResilientGrpcClient;
import io.example.apigateway.routes.GatewayRoutes;
import io.example.apigateway.utils.GrpcGatewayUtils;
import io.example.common.config.JwtConfig;
import io.example.common.config.TelemetryConfig;
import io.example.common.exception.api.ApiException;
import io.opentelemetry.api.OpenTelemetry;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.grpc.client.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiGatewayVerticle extends AbstractVerticle {
  private static final Logger log = LoggerFactory.getLogger(ApiGatewayVerticle.class);

  private TelemetryConfig telemetryConfig;
  private GrpcClient grpcClient;

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    JsonObject config = new JsonObject()
        .put("http_port", 8080)
        .put("service.name", "api-gateway");

    DeploymentOptions options = new DeploymentOptions().setConfig(config);

    vertx.deployVerticle(new ApiGatewayVerticle(), options)
        .onSuccess(id -> {
          log.info("✅ API Gateway successfully deployed! ID: {}", id);
          log.info("🚀 Gateway listening on port 8080");
        })
        .onFailure(err -> {
          log.error("❌ Failed to deploy ApiGatewayVerticle", err);
        });
  }

  @Override
  public void start(Promise<Void> startPromise) {
    JsonObject rawConfig = config();

    // 1. Setup OpenTelemetry for edge monitoring
    JsonObject telConfig = rawConfig.copy();
    if (!telConfig.containsKey("service.name")) {
      telConfig.put("service.name", "api-gateway");
    }
    telemetryConfig = new TelemetryConfig(telConfig);
    OpenTelemetry openTelemetry = telemetryConfig.initialize();

    // 2. Instantiate unified gRPC Client pool — decorated so every downstream
    // call propagates the W3C traceparent (gap #24) and is guarded by a
    // per-upstream circuit breaker (gap #25).
    grpcClient = new ResilientGrpcClient(GrpcClient.client(vertx));

    // 3. Define all SocketAddresses for backend microservices using environment
    // variables. Defaults follow the deploy port map (docker.env.example / k8s).
    SocketAddress addrUser = resolveGrpcAddress("USER", "user", 50053);
    SocketAddress addrAuth = resolveGrpcAddress("AUTH", "auth", 50051);
    SocketAddress addrRole = resolveGrpcAddress("ROLE", "role", 50052);
    SocketAddress addrMerchant = resolveGrpcAddress("MERCHANT", "merchant", 50054);
    SocketAddress addrTransaction = resolveGrpcAddress("TRANSACTION", "transaction", 50058);
    SocketAddress addrCashier = resolveGrpcAddress("CASHIER", "cashier", 50061);
    SocketAddress addrCategory = resolveGrpcAddress("CATEGORY", "category", 50062);
    SocketAddress addrOrder = resolveGrpcAddress("ORDER", "order", 50055);
    SocketAddress addrOrderItem = resolveGrpcAddress("ORDER_ITEM", "order_item", 50057);
    SocketAddress addrProduct = resolveGrpcAddress("PRODUCT", "product", 50056);

    // 4. Instantiate client stubs pointing to target address channels
    // User
    var userQuery = new pb.user.VertxUserQueryServiceGrpcClient(grpcClient, addrUser);
    var userCmd = new pb.user.VertxUserCommandServiceGrpcClient(grpcClient, addrUser);

    // Auth
    var authClient = new pb.VertxAuthServiceGrpcClient(grpcClient, addrAuth);

    // Role
    var roleQuery = new pb.role.VertxRoleServiceGrpcClient(grpcClient, addrRole);
    var roleCmd = new pb.role.VertxRoleCommandServiceGrpcClient(grpcClient, addrRole);

    // Merchant
    var merchantQuery = new pb.merchant.VertxMerchantQueryServiceGrpcClient(grpcClient, addrMerchant);
    var merchantCmd = new pb.merchant.VertxMerchantCommandServiceGrpcClient(grpcClient, addrMerchant);
    var merchantDocCmd = new pb.merchant_document.VertxMerchantDocumentCommandServiceGrpcClient(grpcClient,
        addrMerchant);
    var merchantDocQuery = new pb.merchant_document.VertxMerchantDocumentQueryServiceGrpcClient(grpcClient,
        addrMerchant);

    // Transaction
    var txQuery = new pb.transaction.VertxTransactionQueryServiceGrpcClient(grpcClient, addrTransaction);
    var txCmd = new pb.transaction.VertxTransactionCommandServiceGrpcClient(grpcClient, addrTransaction);
    var txStatsMethod = new pb.transaction.stats.VertxTransactionStatsMethodServiceGrpcClient(grpcClient,
        addrTransaction);
    var txStatsStatus = new pb.transaction.stats.VertxTransactionStatsStatusServiceGrpcClient(grpcClient,
        addrTransaction);

    // Cashier
    var cashierQuery = new pb.cashier.VertxCashierServiceGrpcClient(grpcClient, addrCashier);
    var cashierCmd = new pb.cashier.VertxCashierCommandServiceGrpcClient(grpcClient, addrCashier);

    // Category
    var categoryQuery = new pb.category.VertxCategoryServiceGrpcClient(grpcClient, addrCategory);
    var categoryCmd = new pb.category.VertxCategoryCommandServiceGrpcClient(grpcClient, addrCategory);

    // Product
    var productQuery = new pb.product.VertxProductServiceGrpcClient(grpcClient, addrProduct);
    var productCmd = new pb.product.VertxProductCommandServiceGrpcClient(grpcClient, addrProduct);

    // Order
    var orderQuery = new pb.order.VertxOrderQueryServiceGrpcClient(grpcClient, addrOrder);
    var orderCmd = new pb.order.VertxOrderCommandServiceGrpcClient(grpcClient, addrOrder);

    // OrderItem
    var orderItemQuery = new pb.order_item.VertxOrderItemServiceGrpcClient(grpcClient, addrOrderItem);

    // 5. Setup Security Utilities
    JWTAuth jwtAuth = JwtConfig.createProvider(vertx);

    // 6. Instantiate high-performance Proxy Handlers
    var authHandler = new AuthProxyHandler(authClient);
    var userHandler = new UserProxyHandler(userQuery, userCmd);
    var roleHandler = new RoleProxyHandler(roleQuery, roleCmd);
    var merchantHandler = new MerchantProxyHandler(
        merchantQuery, merchantCmd, merchantDocCmd, merchantDocQuery);
    var txHandler = new TransactionProxyHandler(txQuery, txCmd, txStatsMethod, txStatsStatus);
    var cashierHandler = new CashierProxyHandler(cashierQuery, cashierCmd);
    var categoryHandler = new CategoryProxyHandler(categoryQuery, categoryCmd);
    var productHandler = new ProductProxyHandler(productQuery, productCmd);
    var orderHandler = new OrderProxyHandler(orderQuery, orderCmd);
    var orderItemHandler = new OrderItemProxyHandler(orderItemQuery);

    // 7. Configure web routers & launch web interface
    Router baseRouter = Router.router(vertx);

    // Initialize and hook up edge observability (metrics + per-request span)
    baseRouter.route().handler(new GatewayMetricsMiddleware(openTelemetry));

    // Per-client-IP rate limiter (429 envelope) — after metrics so limited
    // requests are still measured, before the domain routes (gap #25).
    baseRouter.route().handler(new RateLimitHandler());

    // Initialize and hook up Chaos Engineering HTTP Middleware
    io.example.common.chaos.ChaosManager chaosManager = new io.example.common.chaos.ChaosManager();
    chaosManager.startWatcher(vertx);
    baseRouter.route().handler(new io.example.common.chaos.ChaosHttpMiddleware(chaosManager));

    Router registeredRouter = GatewayRoutes.register(
        baseRouter,
        jwtAuth,
        merchantQuery,
        authHandler,
        userHandler,
        roleHandler,
        merchantHandler,
        txHandler,
        cashierHandler,
        categoryHandler,
        productHandler,
        orderHandler,
        orderItemHandler, chaosManager);

    // Register the FailureHandler AFTER all routes: Vert.x Web routes are
    // matched in registration order, and after a handler fails the router keeps
    // looking forward for a failure handler — one registered earlier would
    // never fire. This maps locally thrown ApiException (e.g. bad path
    // parameter, required form field) to its real HTTP status instead of a
    // generic 500, and never leaks the internal stack trace.
    registeredRouter.route().failureHandler(failureCtx -> {
      Throwable failure = failureCtx.failure();
      int statusCode = failureCtx.statusCode();
      if (failure instanceof ApiException apiEx) {
        GrpcGatewayUtils.sendError(failureCtx, apiEx.getStatusCode(), apiEx.getMessage());
      } else if (failure instanceof IllegalArgumentException iae) {
        GrpcGatewayUtils.sendError(failureCtx, 400, iae.getMessage());
      } else if (statusCode == 401 || statusCode == 403) {
        // JWT / role middleware failure — respect the status it set (401/403)
        // instead of turning it into a 500.
        GrpcGatewayUtils.sendError(failureCtx, statusCode,
            statusCode == 401 ? "Unauthorized" : "Forbidden");
      } else {
        log.error("Unhandled gateway failure", failure);
        GrpcGatewayUtils.sendError(failureCtx, 500, "Internal server error");
      }
    });

    int port = rawConfig.getInteger("http_port", 8080);
    String envHttpPort = System.getenv("HTTP_PORT");
    if (envHttpPort != null)
      port = Integer.parseInt(envHttpPort);

    final int finalPort = port;

    vertx.createHttpServer(new HttpServerOptions().setCompressionSupported(true))
        .requestHandler(registeredRouter)
        .listen(finalPort)
        .onSuccess(srv -> {
          log.info("====================================================================");
          log.info("🚀 API Gateway HUB successfully started at Port {}", finalPort);
          log.info("📈 OpenTelemetry instrumentation active for tracing.");
          log.info("====================================================================");
          startPromise.complete();
        })
        .onFailure(err -> {
          log.error("❌ CRITICAL: Failed to launch HTTP server for Gateway", err);
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

    // If ADDR contains a colon, split it
    if (host.contains(":")) {
      String[] parts = host.split(":");
      host = parts[0];
      port = Integer.parseInt(parts[1]);
    }

    log.info("📍 Service {} mapped to {}:{}", envPrefix, host, port);
    return SocketAddress.inetSocketAddress(port, host);
  }
}
