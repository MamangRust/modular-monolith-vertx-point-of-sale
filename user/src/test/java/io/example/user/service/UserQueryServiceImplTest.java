package io.example.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.user.domain.requests.FindAllUsers;
import io.example.user.model.User;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.example.user.repository.UserQueryRepository;
import io.example.user.service.impl.UserQueryServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceImplTest {

  @Mock
  private UserQueryRepository repository;
  @Mock
  private RedisService redis;
  @Mock
  private TracingMetrics metrics;
  @Mock
  private TracingContext tracingContext;

  private UserQueryServiceImpl service;

  @BeforeEach
  void setUp() {
    lenient().when(metrics.startSpan(anyString())).thenReturn(tracingContext);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
    lenient().when(redis.get(anyString())).thenReturn(Future.succeededFuture(null));
    lenient().when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
    lenient().when(redis.setJson(anyString(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));

    service = new UserQueryServiceImpl(repository, redis, metrics);
  }

  @Test
  void getUsers_shouldFetchFromDb() {
    User user = User.builder().userId(1).firstname("John").email("john@test.com").build();
    PagedResult<User> paged = new PagedResult<>(List.of(user), 1);
    FindAllUsers req = new FindAllUsers();

    when(repository.getUsers(any(FindAllUsers.class))).thenReturn(Future.succeededFuture(paged));

    Future<PagedResult<UserResponse>> result = service.getUsers(req);

    assertTrue(result.succeeded());
    PagedResult<UserResponse> pageResult = result.result();
    assertEquals(1, pageResult.getTotalRecords());
    assertEquals(1, pageResult.getData().size());
    assertEquals("John", pageResult.getData().getFirst().getFirstname());

    verify(redis).get(anyString());
    verify(repository).getUsers(any(FindAllUsers.class));
    verify(redis).setJson(anyString(), any(Object.class), any(Duration.class));
  }

  @Test
  void getUsers_shouldHandleEmptyResult() {
    PagedResult<User> emptyPaged = new PagedResult<>(List.of(), 0);
    FindAllUsers req = new FindAllUsers();

    when(repository.getUsers(any(FindAllUsers.class))).thenReturn(Future.succeededFuture(emptyPaged));

    Future<PagedResult<UserResponse>> result = service.getUsers(req);

    assertTrue(result.succeeded());
    PagedResult<UserResponse> pageResult = result.result();
    assertEquals(0, pageResult.getTotalRecords());
    assertTrue(pageResult.getData().isEmpty());
  }

  @Test
  void getActiveUsers_shouldFetchFromDb() {
    User user = User.builder().userId(1).firstname("Alice").email("alice@test.com").build();
    PagedResult<User> paged = new PagedResult<>(List.of(user), 1);
    FindAllUsers req = new FindAllUsers();

    when(repository.getActiveUsers(any(FindAllUsers.class))).thenReturn(Future.succeededFuture(paged));

    Future<PagedResult<UserResponseDeleteAt>> result = service.getActiveUsers(req);

    assertTrue(result.succeeded());
    PagedResult<UserResponseDeleteAt> pageResult = result.result();
    assertEquals(1, pageResult.getTotalRecords());
    assertEquals("Alice", pageResult.getData().getFirst().getFirstname());
  }

  @Test
  void getTrashedUsers_shouldFetchFromDb() {
    User user = User.builder().userId(1).firstname("Bob").email("bob@test.com").build();
    PagedResult<User> paged = new PagedResult<>(List.of(user), 1);
    FindAllUsers req = new FindAllUsers();

    when(repository.getTrashedUsers(any(FindAllUsers.class))).thenReturn(Future.succeededFuture(paged));

    Future<PagedResult<UserResponseDeleteAt>> result = service.getTrashedUsers(req);

    assertTrue(result.succeeded());
    PagedResult<UserResponseDeleteAt> pageResult = result.result();
    assertEquals(1, pageResult.getTotalRecords());
    assertEquals("Bob", pageResult.getData().getFirst().getFirstname());
  }

  @Test
  void getUserById_shouldFetchFromDb() {
    User user = User.builder().userId(1).firstname("Charlie").email("charlie@test.com").build();

    when(repository.getUserById(1)).thenReturn(Future.succeededFuture(user));

    Future<UserResponse> result = service.getUserById(1);

    assertTrue(result.succeeded());
    UserResponse response = result.result();
    assertEquals(1, response.getUserId());
    assertEquals("Charlie", response.getFirstname());
  }

  @Test
  void getUserById_shouldReturnFromCache() {
    User cachedUser = User.builder().userId(1).firstname("CachedUser").email("cached@test.com").build();

    when(redis.getJson(eq("user:1"), any())).thenReturn(Future.succeededFuture(cachedUser));

    Future<UserResponse> result = service.getUserById(1);

    assertTrue(result.succeeded());
    UserResponse response = result.result();
    assertEquals(1, response.getUserId());
    assertEquals("CachedUser", response.getFirstname());

    verify(repository, never()).getUserById(anyInt());
  }

  @Test
  void getUserById_shouldFailWhenNotFound() {
    when(repository.getUserById(1)).thenReturn(Future.succeededFuture(null));

    Future<UserResponse> result = service.getUserById(1);

    assertTrue(result.failed());
    assertInstanceOf(NotFoundException.class, result.cause());
    assertEquals("User not found", result.cause().getMessage());
  }
}
