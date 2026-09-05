package io.example.common.service;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongCounter;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.RedisAPI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RedisService {
  private static final Logger logger = LoggerFactory.getLogger(RedisService.class.getName());

  private final RedisAPI redisAPI;
  private final Tracer tracer;
  private final Meter meter;
  private final LongCounter cacheHitCounter;
  private final LongCounter cacheMissCounter;
  private final LongCounter cacheSetCounter;

  public RedisService(RedisAPI redisAPI, OpenTelemetry openTelemetry) {
    this.redisAPI = redisAPI;
    this.tracer = openTelemetry.getTracer(Objects.requireNonNull(RedisService.class.getName()));
    this.meter = openTelemetry.getMeter(Objects.requireNonNull(RedisService.class.getName()));

    this.cacheHitCounter = meter.counterBuilder("redis.cache.hits")
        .setDescription("Number of cache hits")
        .setUnit("1")
        .build();

    this.cacheMissCounter = meter.counterBuilder("redis.cache.misses")
        .setDescription("Number of cache misses")
        .setUnit("1")
        .build();

    this.cacheSetCounter = meter.counterBuilder("redis.cache.sets")
        .setDescription("Number of cache sets")
        .setUnit("1")
        .build();
  }

  public Future<String> get(String key) {
    try {
      Span span = tracer.spanBuilder("redis.get")
          .setAttribute("redis.key", Objects.requireNonNull(key))
          .startSpan();

      return redisAPI.get(key)
          .onSuccess(response -> {
            if (response != null && !response.toString().isEmpty()) {
              cacheHitCounter.add(1);
              logger.debug("Cache hit for key: {}", key);
            } else {
              cacheMissCounter.add(1);
              logger.debug("Cache miss for key: {}", key);
            }
          })
          .onFailure(err -> {
            logger.warn("Redis GET error for key {}: {}", key, err.getMessage());
            span.recordException(err);
          })
          .recover(err -> Future.succeededFuture(null))
          .map(response -> response != null ? response.toString() : null)
          .onComplete(ar -> span.end());
    } catch (Throwable t) {
      logger.warn("RedisService.get() unavailable for key {}: {}", key, t.getMessage());
      return Future.succeededFuture(null);
    }
  }

  public Future<String> set(String key, String value) {
    return set(key, value, null);
  }

  public Future<String> set(String key, String value, Duration ttl) {
    try {
      Span span = tracer.spanBuilder("redis.set")
          .setAttribute("redis.key", Objects.requireNonNull(key))
          .setAttribute("redis.ttl_seconds", ttl != null ? ttl.getSeconds() : 0)
          .startSpan();

      List<String> args = ttl != null
          ? Arrays.asList(key, value, "EX", String.valueOf(ttl.getSeconds()))
          : Arrays.asList(key, value);

      return redisAPI.set(args)
          .onSuccess(response -> {
            cacheSetCounter.add(1);
            logger.debug("Cache set for key: {} with TTL: {} seconds", key,
                ttl != null ? ttl.getSeconds() : "none");
          })
          .onFailure(err -> {
            logger.warn("Redis SET error for key {}: {}", key, err.getMessage());
            span.recordException(err);
          })
          .recover(err -> Future.succeededFuture(null))
          .map(response -> response != null ? response.toString() : "OK")
          .onComplete(ar -> span.end());
    } catch (Throwable t) {
      logger.warn("RedisService.set() unavailable for key {}: {}", key, t.getMessage());
      return Future.succeededFuture("OK");
    }
  }

  public Future<Boolean> setIfAbsent(String key, String value, Duration ttl) {
    try {
      Span span = tracer.spanBuilder("redis.setIfAbsent")
          .setAttribute("redis.key", Objects.requireNonNull(key))
          .setAttribute("redis.ttl_seconds", Objects.requireNonNull(ttl).getSeconds())
          .startSpan();

      List<String> args = Arrays.asList(key, value, "NX", "EX", String.valueOf(ttl.getSeconds()));

      return redisAPI.set(args)
          .map(response -> response != null)
          .onSuccess(created -> {
            if (Boolean.TRUE.equals(created)) {
              logger.debug("SET NX succeeded for key: {}", key);
            } else {
              logger.debug("SET NX rejected, key already exists: {}", key);
            }
          })
          .onFailure(err -> {
            logger.warn("Redis SET NX error for key {}: {}", key, err.getMessage());
            span.recordException(err);
          })
          .recover(err -> Future.succeededFuture(false))
          .onComplete(ar -> span.end());
    } catch (Throwable t) {
      logger.warn("RedisService.setIfAbsent() unavailable for key {}: {}", key, t.getMessage());
      return Future.succeededFuture(false);
    }
  }

  public Future<Long> ttl(String key) {
    try {
      Span span = tracer.spanBuilder("redis.ttl")
          .setAttribute("redis.key", Objects.requireNonNull(key))
          .startSpan();

      return redisAPI.ttl(key)
          .map(response -> response != null ? response.toLong() : -2L)
          .onFailure(err -> {
            logger.warn("Redis TTL error for key {}: {}", key, err.getMessage());
            span.recordException(err);
          })
          .recover(err -> Future.succeededFuture(-2L))
          .onComplete(ar -> span.end());
    } catch (Throwable t) {
      logger.warn("RedisService.ttl() unavailable for key {}: {}", key, t.getMessage());
      return Future.succeededFuture(-2L);
    }
  }

  public Future<Long> delete(String key) {
    try {
      Span span = tracer.spanBuilder("redis.delete")
          .setAttribute("redis.key", Objects.requireNonNull(key))
          .startSpan();

      return redisAPI.del(List.of(key))
          .onSuccess(response -> logger.debug("Deleted key: {}", key))
          .onFailure(err -> {
            logger.warn("Redis DELETE error for key {}: {}", key, err.getMessage());
            span.recordException(err);
          })
          .recover(err -> Future.succeededFuture(null))
          .map(response -> response != null ? response.toLong() : 0L)
          .onComplete(ar -> span.end());
    } catch (Throwable t) {
      logger.warn("RedisService.delete() unavailable for key {}: {}", key, t.getMessage());
      return Future.succeededFuture(0L);
    }
  }

  public Future<Long> deleteByPattern(String pattern) {
    try {
      Span span = tracer.spanBuilder("redis.deleteByPattern")
          .setAttribute("redis.pattern", Objects.requireNonNull(pattern))
          .startSpan();

      return redisAPI.keys(pattern)
          .compose(response -> {
            if (response == null || response.size() == 0) {
              return Future.succeededFuture(0L);
            }
            List<String> keys = new ArrayList<>();
            for (int i = 0; i < response.size(); i++) {
              keys.add(response.get(i).toString());
            }
            return redisAPI.del(keys).map(res -> res != null ? res.toLong() : 0L);
          })
          .onSuccess(count -> logger.debug("Deleted {} keys matching pattern: {}", count, pattern))
          .onFailure(err -> {
            logger.warn("Redis DELETE BY PATTERN error for pattern {}: {}", pattern, err.getMessage());
            span.recordException(err);
          })
          .recover(err -> Future.succeededFuture(0L))
          .onComplete(ar -> span.end());
    } catch (Throwable t) {
      logger.warn("RedisService.deleteByPattern() unavailable for pattern {}: {}", pattern, t.getMessage());
      return Future.succeededFuture(0L);
    }
  }

  public Future<Boolean> exists(String key) {
    try {
      Span span = tracer.spanBuilder("redis.exists")
          .setAttribute("redis.key", Objects.requireNonNull(key))
          .startSpan();

      return redisAPI.exists(List.of(key))
          .onSuccess(response -> logger.debug("Exists check for key: {} = {}", key, response.toLong() > 0))
          .onFailure(err -> {
            logger.warn("Redis EXISTS error for key {}: {}", key, err.getMessage());
            span.recordException(err);
          })
          .recover(err -> Future.succeededFuture(null))
          .map(response -> response != null && response.toLong() > 0)
          .onComplete(ar -> span.end());
    } catch (Throwable t) {
      logger.warn("RedisService.exists() unavailable for key {}: {}", key, t.getMessage());
      return Future.succeededFuture(false);
    }
  }

  public <T> Future<List<T>> getJsonList(String key, Class<T> clazz) {
    try {
      Span span = tracer.spanBuilder("redis.getJsonList")
          .setAttribute("redis.key", Objects.requireNonNull(key))
          .startSpan();

      return redisAPI.exists(List.of(key))
          .compose(existsResult -> {
            if (existsResult.toInteger() == 0) {
              cacheMissCounter.add(1);
              return Future.succeededFuture(new ArrayList<T>());
            }
            return redisAPI.lrange(key, "0", "-1")
                .map(response -> {
                  List<T> result = new ArrayList<>();
                  if (response != null) {
                    for (int i = 0; i < response.size(); i++) {
                      try {
                        T item = Json.decodeValue(response.get(i).toString(), clazz);
                        result.add(item);
                      } catch (Exception e) {
                        logger.warn("Failed to parse JSON item from list key {}: {}", key, e.getMessage());
                      }
                    }
                  }
                  return result;
                });
          })
          .onSuccess(result -> {
            if (!result.isEmpty()) cacheHitCounter.add(1);
          })
          .onFailure(err -> logger.warn("Redis GET JSON LIST error for key {}: {}", key, err.getMessage()))
          .recover(err -> Future.succeededFuture(new ArrayList<>()))
          .onComplete(ar -> span.end());
    } catch (Throwable t) {
      logger.warn("RedisService.getJsonList() unavailable for key {}: {}", key, t.getMessage());
      return Future.succeededFuture(new ArrayList<>());
    }
  }

  public <T> Future<Void> setJsonList(String key, List<T> values, Duration ttl) {
    try {
      Span span = tracer.spanBuilder("redis.setJsonList")
          .setAttribute("redis.key", Objects.requireNonNull(key))
          .setAttribute("redis.list_size", values.size())
          .setAttribute("redis.ttl_seconds", ttl != null ? ttl.getSeconds() : 0)
          .startSpan();

      if (values.isEmpty()) {
        span.end();
        return Future.succeededFuture();
      }

      return redisAPI.del(List.of(key))
          .<Void>compose(delResult -> {
            List<String> jsonValues = new ArrayList<>();
            for (T value : values) {
              try {
                jsonValues.add(Json.encode(value));
              } catch (Exception e) {
                return Future.succeededFuture();
              }
            }
            List<String> rpushArgs = new ArrayList<>();
            rpushArgs.add(key);
            rpushArgs.addAll(jsonValues);
            return redisAPI.rpush(rpushArgs)
                .compose(pushResult -> {
                  if (ttl != null) {
                    return redisAPI.expire(List.of(key, String.valueOf(ttl.getSeconds()))).mapEmpty();
                  }
                  return Future.succeededFuture();
                });
          })
          .onSuccess(v -> cacheSetCounter.add(1))
          .onFailure(err -> logger.warn("Redis SET JSON LIST error for key {}: {}", key, err.getMessage()))
          .recover(err -> Future.succeededFuture(null))
          .onComplete(ar -> span.end());
    } catch (Throwable t) {
      logger.warn("RedisService.setJsonList() unavailable for key {}: {}", key, t.getMessage());
      return Future.succeededFuture();
    }
  }

  public Future<String> setJson(String key, JsonObject value, Duration ttl) {
    return set(key, value.encode(), ttl);
  }

  public Future<JsonObject> getJson(String key) {
    return get(key)
        .compose(jsonStr -> {
          if (jsonStr == null || jsonStr.isEmpty()) {
            return Future.succeededFuture(null);
          }
          try {
            return Future.succeededFuture(new JsonObject(jsonStr));
          } catch (Exception e) {
            logger.warn("Cache deserialization failed for key {}: {}", key, e.getMessage());
            return Future.succeededFuture(null);
          }
        });
  }

  public <T> Future<T> getJson(String key, Class<T> clazz) {
    return get(key)
        .compose(jsonStr -> {
          if (jsonStr == null || jsonStr.isEmpty()) {
            return Future.succeededFuture(null);
          }
          try {
            return Future.succeededFuture(Json.decodeValue(jsonStr, clazz));
          } catch (Throwable e) {
            logger.warn("Cache deserialization failed for class {} key {}: {}", clazz.getSimpleName(), key, e.getMessage());
            return Future.succeededFuture(null);
          }
        });
  }

  public Future<String> setJson(String key, Object value, Duration ttl) {
    try {
      return set(key, Json.encode(value), ttl);
    } catch (Throwable e) {
      logger.warn("Cache encode failed for key {}: {}", key, e.getMessage());
      return Future.succeededFuture("SKIP");
    }
  }

  public Future<Long> incr(String key) {
    try {
      Span span = tracer.spanBuilder("redis.incr")
          .setAttribute("redis.key", Objects.requireNonNull(key))
          .startSpan();

      return redisAPI.incr(key)
          .onSuccess(response -> logger.debug("Incremented key: {}", key))
          .onFailure(err -> {
            logger.warn("Redis INCR error for key {}: {}", key, err.getMessage());
            span.recordException(err);
          })
          .map(response -> response.toLong())
          .recover(err -> Future.succeededFuture(0L))
          .onComplete(ar -> span.end());
    } catch (Throwable t) {
      logger.warn("RedisService.incr() unavailable for key {}: {}", key, t.getMessage());
      return Future.succeededFuture(0L);
    }
  }

  public Future<Void> expire(String key, Duration ttl) {
    try {
      Span span = tracer.spanBuilder("redis.expire")
          .setAttribute("redis.key", Objects.requireNonNull(key))
          .setAttribute("redis.ttl_seconds", ttl.getSeconds())
          .startSpan();

      return redisAPI.expire(List.of(key, String.valueOf(ttl.getSeconds())))
          .onSuccess(response -> logger.debug("Set expiration for key: {} to {} seconds", key, ttl.getSeconds()))
          .onFailure(err -> {
            logger.warn("Redis EXPIRE error for key {}: {}", key, err.getMessage());
            span.recordException(err);
          })
          .recover(err -> Future.succeededFuture(null))
          .onComplete(ar -> span.end())
          .map(v -> (Void) null);
    } catch (Throwable t) {
      logger.warn("RedisService.expire() unavailable for key {}: {}", key, t.getMessage());
      return Future.succeededFuture();
    }
  }

  public Future<String> ping() {
    Span span = tracer.spanBuilder("redis.ping").startSpan();

    return redisAPI.ping(Collections.emptyList())
        .onSuccess(response -> logger.debug("Redis PING response: {}", response.toString()))
        .onFailure(err -> {
          logger.error("Redis PING error: {}", err.getMessage());
          span.recordException(err);
        })
        .map(response -> response.toString())
        .onComplete(ar -> span.end());
  }

  public Future<String> flushAll() {
    Span span = tracer.spanBuilder("redis.flushAll").startSpan();

    return redisAPI.flushall(Collections.emptyList())
        .onSuccess(response -> logger.debug("Redis FLUSHALL response: {}", response.toString()))
        .onFailure(err -> {
          logger.error("Redis FLUSHALL error: {}", err.getMessage());
          span.recordException(err);
        })
        .map(response -> response.toString())
        .onComplete(ar -> span.end());
  }
}
