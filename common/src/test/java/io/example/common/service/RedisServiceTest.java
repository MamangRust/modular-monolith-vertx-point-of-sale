package io.example.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.Response;

@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

  @Mock
  private RedisAPI redisAPI;

  private RedisService service;

  @BeforeEach
  void setUp() {
    service = new RedisService(redisAPI, OpenTelemetrySdk.builder().build());
  }

  @Test
  void getReturnsValueOnHit() {
    Response resp = mock(Response.class);
    when(resp.toString()).thenReturn("cached-value");
    when(redisAPI.get("user:1")).thenReturn(Future.succeededFuture(resp));

    assertThat(service.get("user:1").result()).isEqualTo("cached-value");
  }

  @Test
  void getReturnsNullOnMiss() {
    when(redisAPI.get("user:1")).thenReturn(Future.succeededFuture(null));

    assertThat(service.get("user:1").result()).isNull();
  }

  @Test
  void setWithoutTtlSendsPlainSet() {
    when(redisAPI.set(List.of("k", "v"))).thenReturn(Future.succeededFuture(mock(Response.class)));

    service.set("k", "v").result();

    verify(redisAPI).set(List.of("k", "v"));
  }

  @Test
  void setWithTtlAppendsExSeconds() {
    when(redisAPI.set(anyList())).thenReturn(Future.succeededFuture(mock(Response.class)));

    service.set("k", "v", Duration.ofSeconds(300)).result();

    verify(redisAPI).set(List.of("k", "v", "EX", "300"));
  }

  @Test
  void deleteReturnsAffectedRowCount() {
    Response resp = mock(Response.class);
    when(resp.toLong()).thenReturn(1L);
    when(redisAPI.del(List.of("user:1"))).thenReturn(Future.succeededFuture(resp));

    assertThat(service.delete("user:1").result()).isEqualTo(1L);
    verify(redisAPI).del(List.of("user:1"));
  }

  @Test
  void deleteByPatternKeysThenDeletesMatching() {
    Response keysResp = mock(Response.class);
    Response key1 = mock(Response.class);
    Response key2 = mock(Response.class);
    when(key1.toString()).thenReturn("user:1");
    when(key2.toString()).thenReturn("user:2");
    when(keysResp.size()).thenReturn(2);
    when(keysResp.get(0)).thenReturn(key1);
    when(keysResp.get(1)).thenReturn(key2);
    when(redisAPI.keys("user:*")).thenReturn(Future.succeededFuture(keysResp));

    Response delResp = mock(Response.class);
    when(delResp.toLong()).thenReturn(2L);
    when(redisAPI.del(List.of("user:1", "user:2"))).thenReturn(Future.succeededFuture(delResp));

    assertThat(service.deleteByPattern("user:*").result()).isEqualTo(2L);
    verify(redisAPI).keys("user:*");
    verify(redisAPI).del(List.of("user:1", "user:2"));
  }

  @Test
  void deleteByPatternWithNoMatchesDoesNotCallDel() {
    Response empty = mock(Response.class);
    when(empty.size()).thenReturn(0);
    when(redisAPI.keys("nope:*")).thenReturn(Future.succeededFuture(empty));

    assertThat(service.deleteByPattern("nope:*").result()).isZero();
    verify(redisAPI).keys("nope:*");
    verify(redisAPI, never()).del(anyList());
  }

  @Test
  void existsReturnsTrueWhenKeyPresent() {
    Response resp = mock(Response.class);
    when(resp.toLong()).thenReturn(1L);
    when(redisAPI.exists(List.of("k"))).thenReturn(Future.succeededFuture(resp));

    assertThat(service.exists("k").result()).isTrue();
  }

  @Test
  void existsReturnsFalseWhenKeyAbsent() {
    Response resp = mock(Response.class);
    when(resp.toLong()).thenReturn(0L);
    when(redisAPI.exists(List.of("k"))).thenReturn(Future.succeededFuture(resp));

    assertThat(service.exists("k").result()).isFalse();
  }

  @Test
  void setIfAbsentReturnsTrueWhenKeyWasSet() {
    when(redisAPI.set(anyList())).thenReturn(Future.succeededFuture(mock(Response.class)));

    assertThat(service.setIfAbsent("k", "v", Duration.ofSeconds(60)).result()).isTrue();
    verify(redisAPI).set(List.of("k", "v", "NX", "EX", "60"));
  }

  @Test
  void setIfAbsentReturnsFalseWhenKeyAlreadyExists() {
    // SET NX on an existing key replies NIL, which the client surfaces as a null Response.
    when(redisAPI.set(anyList())).thenReturn(Future.succeededFuture(null));

    assertThat(service.setIfAbsent("k", "v", Duration.ofSeconds(60)).result()).isFalse();
    verify(redisAPI).set(List.of("k", "v", "NX", "EX", "60"));
  }

  @Test
  void ttlReturnsRemainingSeconds() {
    Response resp = mock(Response.class);
    when(resp.toLong()).thenReturn(42L);
    when(redisAPI.ttl("k")).thenReturn(Future.succeededFuture(resp));

    assertThat(service.ttl("k").result()).isEqualTo(42L);
  }

  @Test
  void ttlReturnsNegativeOneWhenNoExpiry() {
    Response resp = mock(Response.class);
    when(resp.toLong()).thenReturn(-1L);
    when(redisAPI.ttl("k")).thenReturn(Future.succeededFuture(resp));

    assertThat(service.ttl("k").result()).isEqualTo(-1L);
  }
}
