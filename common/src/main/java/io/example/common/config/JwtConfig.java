package io.example.common.config;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

import io.vertx.ext.auth.PubSecKeyOptions;

public class JwtConfig {

  /**
   * Dev-only fallback. In any real environment (docker/k8s) SECRET_KEY must be
   * provided — see docker.env.example / deployments/kubernetes/base/common/secret.yaml.
   */
  static final String DEFAULT_SECRET = "my-secret-key";

  /**
   * Resolves the HS256 signing secret with precedence: env {@code SECRET_KEY} >
   * dev default. Both the auth service (signer) and the API gateway (verifier)
   * MUST resolve the same value or issued tokens will be rejected.
   */
  public static String resolveSecret() {
    String env = System.getenv("SECRET_KEY");
    return (env == null || env.isBlank()) ? DEFAULT_SECRET : env;
  }

  public static JWTAuth createProvider(Vertx vertx) {
    JWTAuthOptions config = new JWTAuthOptions()
        .addPubSecKey(new PubSecKeyOptions()
            .setAlgorithm("HS256")
            .setBuffer(resolveSecret()));

    return JWTAuth.create(vertx, config);
  }
}
