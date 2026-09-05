package io.example.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
class JwtConfigTest {

  @Test
  void providerCreatesTokenThatAuthenticates(Vertx vertx, VertxTestContext ctx) {
    JWTAuth jwt = JwtConfig.createProvider(vertx);
    assertThat(jwt).isNotNull();

    String token = jwt.generateToken(new JsonObject().put("sub", "42").put("role", "ADMIN"));
    assertThat(token).isNotBlank();

    jwt.authenticate(new TokenCredentials(token))
        .onSuccess(user -> {
          ctx.verify(() -> {
            assertThat(user.principal().getString("sub")).isEqualTo("42");
            ctx.completeNow();
          });
        })
        .onFailure(ctx::failNow);
  }

  @Test
  void providerRejectsGarbageToken(Vertx vertx, VertxTestContext ctx) {
    JWTAuth jwt = JwtConfig.createProvider(vertx);

    jwt.authenticate(new TokenCredentials("not.a.jwt"))
        .onSuccess(user -> ctx.failNow(new AssertionError("garbage token must be rejected")))
        .onFailure(err -> ctx.completeNow());
  }
}
