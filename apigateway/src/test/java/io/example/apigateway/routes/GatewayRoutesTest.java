package io.example.apigateway.routes;

import io.example.apigateway.handler.AuthProxyHandler;
import io.example.apigateway.handler.CashierProxyHandler;
import io.example.apigateway.handler.CategoryProxyHandler;
import io.example.apigateway.handler.MerchantProxyHandler;
import io.example.apigateway.handler.OrderItemProxyHandler;
import io.example.apigateway.handler.OrderProxyHandler;
import io.example.apigateway.handler.ProductProxyHandler;
import io.example.apigateway.handler.RoleProxyHandler;
import io.example.apigateway.handler.TransactionProxyHandler;
import io.example.apigateway.handler.UserProxyHandler;
import io.example.common.chaos.ChaosManager;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
class GatewayRoutesTest {

    @Mock
    JWTAuth jwtAuth;
    @Mock
    ChaosManager chaosManager;
    @Mock
    AuthProxyHandler auth;
    @Mock
    UserProxyHandler user;
    @Mock
    RoleProxyHandler role;
    @Mock
    MerchantProxyHandler merchant;
    @Mock
    TransactionProxyHandler transaction;
    @Mock
    CashierProxyHandler cashier;
    @Mock
    CategoryProxyHandler category;
    @Mock
    ProductProxyHandler product;
    @Mock
    OrderProxyHandler order;
    @Mock
    OrderItemProxyHandler orderItem;
    @Mock
    pb.merchant.VertxMerchantQueryServiceGrpcClient merchantQueryClient;

    private int port;

    @BeforeEach
    void setUp(Vertx vertx, VertxTestContext ctx) {
        port = 0;
        var router = Router.router(vertx);

        GatewayRoutes.register(
                router, jwtAuth, merchantQueryClient,
                auth, user, role, merchant, transaction,
                cashier, category, product, order, orderItem,
                chaosManager
        );

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(0)
                .onComplete(ctx.succeeding(server -> {
                    port = server.actualPort();
                    ctx.completeNow();
                }));
    }

    @Test
    void healthEndpoint_shouldReturn200(Vertx vertx, VertxTestContext ctx) {
        WebClient client = WebClient.create(vertx);
        client.get(port, "localhost", "/health")
                .send()
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() -> {
                        assertThat(resp.statusCode()).isEqualTo(200);
                        assertThat(resp.bodyAsJsonObject().getString("status")).isEqualTo("UP");
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void protectedRoutes_shouldReturn401WhenNoToken(Vertx vertx, VertxTestContext ctx) {
        WebClient client = WebClient.create(vertx);
        client.get(port, "localhost", "/users")
                .send()
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() ->
                            assertThat(resp.statusCode()).isEqualTo(401)
                    );
                    ctx.completeNow();
                }));
    }

    @Test
    void healthEndpoint_shouldReturnJsonContentType(Vertx vertx, VertxTestContext ctx) {
        WebClient client = WebClient.create(vertx);
        client.get(port, "localhost", "/health")
                .send()
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() -> {
                        assertThat(resp.getHeader("Content-Type")).contains("application/json");
                    });
                    ctx.completeNow();
                }));
    }

    @Test
    void adminRoutes_shouldRequireAuthentication(Vertx vertx, VertxTestContext ctx) {
        WebClient client = WebClient.create(vertx);
        client.get(port, "localhost", "/roles")
                .send()
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() ->
                            assertThat(resp.statusCode()).isEqualTo(401)
                    );
                    ctx.completeNow();
                }));
    }

    @Test
    void merchantApiRoutes_shouldRequireAuthentication(Vertx vertx, VertxTestContext ctx) {
        WebClient client = WebClient.create(vertx);
        client.get(port, "localhost", "/api/merchants")
                .send()
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() ->
                            assertThat(resp.statusCode()).isEqualTo(401)
                    );
                    ctx.completeNow();
                }));
    }

    @Test
    void transactionRoutes_shouldRequireAuthentication(Vertx vertx, VertxTestContext ctx) {
        WebClient client = WebClient.create(vertx);
        client.get(port, "localhost", "/transactions")
                .send()
                .onComplete(ctx.succeeding(resp -> {
                    ctx.verify(() ->
                            assertThat(resp.statusCode()).isEqualTo(401)
                    );
                    ctx.completeNow();
                }));
    }
}
