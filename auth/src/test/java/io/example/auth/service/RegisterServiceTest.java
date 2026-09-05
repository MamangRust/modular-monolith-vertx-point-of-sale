package io.example.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.auth.model.AuthUser;
import io.example.auth.model.RegisterRequest;
import io.example.auth.model.Role;
import io.example.auth.model.UserRole;
import io.example.auth.repository.RoleRepository;
import io.example.auth.repository.UserRepository;
import io.example.auth.repository.UserRoleRepository;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.KafkaService;
import io.example.common.service.RedisService;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock
    UserRepository userRepository;
    @Mock
    RoleRepository roleRepository;
    @Mock
    UserRoleRepository userRoleRepository;
    @Mock
    RedisService redisService;
    @Mock
    TracingMetrics tracingMetrics;
    @Mock
    TracingMetrics.TracingContext tracingContext;
    @Mock
    KafkaService kafkaService;

    private RegisterService registerService;

    @BeforeEach
    void setUp() {
        registerService = new RegisterService(
                userRepository, roleRepository, userRoleRepository, redisService, tracingMetrics, kafkaService);
    }

    @Test
    void register_shouldCreateUserAndAssignRole() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@test.com")
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .build();

        Role role = Role.builder().roleId(1).roleName("ROLE_ADMIN").build();
        AuthUser createdUser = AuthUser.builder()
                .userId(1)
                .email("test@test.com")
                .firstname("John")
                .lastname("Doe")
                .build();
        UserRole userRole = UserRole.builder().userId(1).roleId(1).build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(userRepository.findByEmail(anyString())).thenReturn(Future.succeededFuture(null));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Future.succeededFuture(role));
        when(userRepository.createUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Future.succeededFuture(createdUser));
        when(userRoleRepository.assignRoleToUser(anyInt(), anyInt())).thenReturn(Future.succeededFuture(userRole));
        when(redisService.set(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Future.succeededFuture("OK"));
        when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture());

        Future<AuthUser> result = registerService.register(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).isEqualTo(createdUser);
        verify(roleRepository).findByName("ROLE_ADMIN");
        verify(userRoleRepository).assignRoleToUser(eq(1), eq(1));
    }

    @Test
    void register_shouldFailWhenEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@test.com")
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .build();

        AuthUser existingUser = AuthUser.builder().userId(99).email("existing@test.com").build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(userRepository.findByEmail(anyString())).thenReturn(Future.succeededFuture(existingUser));

        Future<AuthUser> result = registerService.register(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause().getMessage()).isEqualTo("User with this email already exists");
    }

    @Test
    void register_shouldFailWithBadRequestWhenInputInvalid() {
        RegisterRequest request = RegisterRequest.builder()
                .email("")
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);

        Future<AuthUser> result = registerService.register(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(io.example.common.exception.grpc.BadRequestException.class);
        assertThat(result.cause().getMessage()).isEqualTo("Email is required");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void register_shouldFailWhenRoleNotFound() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@test.com")
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(userRepository.findByEmail(anyString())).thenReturn(Future.succeededFuture(null));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Future.succeededFuture(null));

        Future<AuthUser> result = registerService.register(request);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause().getMessage()).isEqualTo("Default role not found: ROLE_ADMIN");
    }

    @Test
    void register_shouldSendKafkaEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@test.com")
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .build();

        Role role = Role.builder().roleId(1).roleName("ROLE_ADMIN").build();
        AuthUser createdUser = AuthUser.builder()
                .userId(1)
                .email("test@test.com")
                .firstname("John")
                .lastname("Doe")
                .build();
        UserRole userRole = UserRole.builder().userId(1).roleId(1).build();

        when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        when(userRepository.findByEmail(anyString())).thenReturn(Future.succeededFuture(null));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Future.succeededFuture(role));
        when(userRepository.createUser(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Future.succeededFuture(createdUser));
        when(userRoleRepository.assignRoleToUser(anyInt(), anyInt())).thenReturn(Future.succeededFuture(userRole));
        when(redisService.set(anyString(), anyString(), any(Duration.class)))
                .thenReturn(Future.succeededFuture("OK"));
        when(kafkaService.sendMessage(anyString(), anyString(), any(JsonObject.class)))
                .thenReturn(Future.succeededFuture());

        registerService.register(request);

        verify(kafkaService).sendMessage(
                eq("email-service-topic-auth-register"),
                eq("1"),
                any(JsonObject.class));
    }
}
