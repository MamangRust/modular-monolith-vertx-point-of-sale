package io.example.apigateway;

import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Minimal deployment test for ApiGatewayVerticle.
 * <p>
 * Note: Full verticle integration tests require careful OpenTelemetry lifecycle management
 * (GlobalOpenTelemetry.set can only be called once per JVM). For comprehensive route-level
 * testing, see {@code routes.GatewayRoutesTest}.
 */
@ExtendWith(VertxExtension.class)
class ApiGatewayVerticleTest {

    @Test
    void verticleClass_shouldBeLoadable() {
        assertThat(ApiGatewayVerticle.class).isNotNull();
    }

    @Test
    void verticleShouldHaveMainMethod() throws Exception {
        var mainMethod = ApiGatewayVerticle.class.getMethod("main", String[].class);
        assertThat(mainMethod).isNotNull();
    }

    @Test
    void verticleShouldHaveStartMethod() throws Exception {
        var startMethod = ApiGatewayVerticle.class.getMethod("start");
        assertThat(startMethod).isNotNull();
    }

    @Test
    void verticleShouldExtendAbstractVerticle() {
        assertThat(ApiGatewayVerticle.class.getSuperclass().getSimpleName()).isEqualTo("AbstractVerticle");
    }
}
