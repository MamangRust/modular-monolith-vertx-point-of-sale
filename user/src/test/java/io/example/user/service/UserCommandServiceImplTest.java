package io.example.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import at.favre.lib.crypto.bcrypt.BCrypt;
import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.observability.TracingMetrics.TracingContext;
import io.example.common.service.RedisService;
import io.example.user.domain.requests.CreateUserRequest;
import io.example.user.domain.requests.UpdateUserRequest;
import io.example.user.model.User;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.example.user.repository.UserCommandRepository;
import io.example.user.repository.UserQueryRepository;
import io.example.user.service.impl.UserCommandServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

  @Mock
  private UserCommandRepository repository;
  @Mock
  private UserQueryRepository queryRepository;
  @Mock
  private RedisService redis;
  @Mock
  private TracingMetrics metrics;
  @Mock
  private TracingContext tracingContext;

  private UserCommandServiceImpl service;

  @BeforeEach
  void setUp() {
    lenient().when(metrics.startSpan(anyString())).thenReturn(tracingContext);
    lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
    lenient().when(redis.delete(anyString())).thenReturn(Future.succeededFuture(1L));
    lenient().when(redis.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

    service = new UserCommandServiceImpl(repository, queryRepository, redis, metrics);
  }

  @Test
  void createUser_shouldCreateAndReturnResponse() {
    String rawPassword = "password123";
    User user = User.builder().userId(1).firstname("John").email("john@test.com").build();
    CreateUserRequest req = CreateUserRequest.builder()
        .firstName("John").lastName("Doe").email("john@test.com")
        .password(rawPassword).confirmPassword(rawPassword).build();

    when(repository.createUser(req)).thenReturn(Future.succeededFuture(user));
    when(repository.assignDefaultAdminRole(1)).thenReturn(Future.succeededFuture());

    Future<UserResponse> result = service.createUser(req);

    assertTrue(result.succeeded());
    UserResponse response = result.result();
    assertEquals(1, response.getUserId());
    assertEquals("John", response.getFirstname());
    assertEquals("john@test.com", response.getEmail());

    ArgumentCaptor<CreateUserRequest> captor = ArgumentCaptor.forClass(CreateUserRequest.class);
    verify(repository).createUser(captor.capture());
    String hashedPassword = captor.getValue().getPassword();
    assertTrue(BCrypt.verifyer().verify(rawPassword.toCharArray(), hashedPassword).verified);
    assertNotEquals(rawPassword, hashedPassword);

    verify(repository).assignDefaultAdminRole(1);
  }

  @Test
  void createUser_shouldFailWhenPasswordsDontMatch() {
    CreateUserRequest req = CreateUserRequest.builder()
        .firstName("John").lastName("Doe").email("john@test.com")
        .password("password123").confirmPassword("different").build();

    Future<UserResponse> result = service.createUser(req);

    assertTrue(result.failed());
    assertInstanceOf(BadRequestException.class, result.cause());
    assertEquals("Passwords do not match with confirmation", result.cause().getMessage());
    verify(repository, never()).createUser(any());
    verify(repository, never()).assignDefaultAdminRole(anyInt());
  }

  @Test
  void updateUser_shouldUpdateAndReturn() {
    User user = User.builder().userId(1).firstname("Jane").email("jane@test.com").build();
    UpdateUserRequest updReq = UpdateUserRequest.builder()
        .userId(1).firstName("Jane").build();

    when(repository.updateUser(updReq)).thenReturn(Future.succeededFuture(user));

    Future<UserResponse> result = service.updateUser(updReq);

    assertTrue(result.succeeded());
    UserResponse response = result.result();
    assertEquals(1, response.getUserId());
    assertEquals("Jane", response.getFirstname());
  }

  @Test
  void updateUser_shouldFailWhenPasswordsDontMatch() {
    UpdateUserRequest updReq = UpdateUserRequest.builder()
        .userId(1).firstName("Jane")
        .password("password123").confirmPassword("different").build();

    Future<UserResponse> result = service.updateUser(updReq);

    assertTrue(result.failed());
    assertInstanceOf(BadRequestException.class, result.cause());
    assertEquals("Passwords do not match with confirmation", result.cause().getMessage());
    verify(repository, never()).updateUser(any());
  }

  @Test
  void updateUser_shouldFailWhenUserNotFound() {
    UpdateUserRequest updReq = UpdateUserRequest.builder()
        .userId(1).firstName("Jane").build();

    when(repository.updateUser(updReq)).thenReturn(Future.succeededFuture(null));

    Future<UserResponse> result = service.updateUser(updReq);

    assertTrue(result.failed());
    assertInstanceOf(NotFoundException.class, result.cause());
    assertEquals("User not found", result.cause().getMessage());
  }

  @Test
  void trashUser_shouldTrashAndReturn() {
    User user = User.builder().userId(1).firstname("John").email("john@test.com").build();

    when(repository.trashed(1)).thenReturn(Future.succeededFuture(user));

    Future<UserResponseDeleteAt> result = service.trashUser(1);

    assertTrue(result.succeeded());
    UserResponseDeleteAt response = result.result();
    assertEquals(1, response.getUserId());
    assertEquals("John", response.getFirstname());
  }

  @Test
  void trashUser_shouldFailWhenNotFound() {
    when(repository.trashed(1)).thenReturn(Future.succeededFuture(null));

    Future<UserResponseDeleteAt> result = service.trashUser(1);

    assertTrue(result.failed());
    assertInstanceOf(NotFoundException.class, result.cause());
    assertEquals("User not found", result.cause().getMessage());
  }

  @Test
  void restoreUser_shouldRestore() {
    User user = User.builder().userId(1).firstname("John").email("john@test.com").build();

    when(queryRepository.findByTrashedId(1)).thenReturn(Future.succeededFuture(user));
    when(repository.restore(1)).thenReturn(Future.succeededFuture(user));

    Future<UserResponseDeleteAt> result = service.restoreUser(1);

    assertTrue(result.succeeded());
    UserResponseDeleteAt response = result.result();
    assertEquals(1, response.getUserId());
  }

  @Test
  void restoreUser_shouldFailWhenNotTrashed() {
    when(queryRepository.findByTrashedId(1)).thenReturn(Future.succeededFuture(null));

    Future<UserResponseDeleteAt> result = service.restoreUser(1);

    assertTrue(result.failed());
    assertInstanceOf(BadRequestException.class, result.cause());
    assertTrue(result.cause().getMessage().contains("must be trashed first"));
  }

  @Test
  void deletePermanent_shouldDelete() {
    User user = User.builder().userId(1).firstname("John").email("john@test.com").build();

    when(queryRepository.findByTrashedId(1)).thenReturn(Future.succeededFuture(user));
    when(repository.deletePermanent(1)).thenReturn(Future.succeededFuture(true));

    Future<Void> result = service.deletePermanent(1);

    assertTrue(result.succeeded());
  }

  @Test
  void deletePermanent_shouldFailWhenNotTrashed() {
    when(queryRepository.findByTrashedId(1)).thenReturn(Future.succeededFuture(null));

    Future<Void> result = service.deletePermanent(1);

    assertTrue(result.failed());
    assertInstanceOf(BadRequestException.class, result.cause());
    assertTrue(result.cause().getMessage().contains("must be trashed"));
  }

  @Test
  void restoreAllUsers_shouldRestoreAll() {
    when(repository.restoreAllUsers()).thenReturn(Future.succeededFuture(5));
    when(redis.deleteByPattern("user:list:*")).thenReturn(Future.succeededFuture(1L));

    Future<Void> result = service.restoreAllUsers();

    assertTrue(result.succeeded());
  }

  @Test
  void restoreAllUsers_shouldFailWhenNoneTrashed() {
    when(repository.restoreAllUsers()).thenReturn(Future.succeededFuture(0));

    Future<Void> result = service.restoreAllUsers();

    assertTrue(result.failed());
    assertInstanceOf(NotFoundException.class, result.cause());
    assertEquals("No trashed users found", result.cause().getMessage());
  }

  @Test
  void deleteAllPermanentUsers_shouldDeleteAll() {
    when(repository.deleteAllPermanentUsers()).thenReturn(Future.succeededFuture(3));
    when(redis.deleteByPattern("user:list:*")).thenReturn(Future.succeededFuture(1L));

    Future<Void> result = service.deleteAllPermanentUsers();

    assertTrue(result.succeeded());
  }

  @Test
  void deleteAllPermanentUsers_shouldFailWhenNoneTrashed() {
    when(repository.deleteAllPermanentUsers()).thenReturn(Future.succeededFuture(0));

    Future<Void> result = service.deleteAllPermanentUsers();

    assertTrue(result.failed());
    assertInstanceOf(NotFoundException.class, result.cause());
    assertEquals("No trashed users found", result.cause().getMessage());
  }
}
