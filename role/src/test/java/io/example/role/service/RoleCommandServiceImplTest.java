package io.example.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.role.domain.response.RoleResponse;
import io.example.role.domain.response.RoleResponseDeleteAt;
import io.example.role.model.Role;
import io.example.role.repository.RoleCommandRepository;
import io.example.role.repository.RoleQueryRepository;
import io.example.role.service.impl.RoleCommandServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;

@ExtendWith(MockitoExtension.class)
class RoleCommandServiceImplTest {

    @Mock
    private RoleCommandRepository commandRepo;

    @Mock
    private RoleQueryRepository queryRepo;

    @Mock
    private RedisService redisService;

    @Mock
    private TracingMetrics tracingMetrics;

    @Mock
    private TracingMetrics.TracingContext tracingContext;

    private RoleCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        lenient().when(tracingMetrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(tracingMetrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        commandService = new RoleCommandServiceImpl(commandRepo, queryRepo, redisService, tracingMetrics);
    }

    private Role createRole() {
        return Role.builder().roleId(1).roleName("ADMIN").build();
    }

    @Test
    void createRole_shouldCreateAndReturnRoleResponse() {
        // Given
        CreateRoleRequest req = CreateRoleRequest.newBuilder().setName("ADMIN").build();
        Role role = createRole();
        when(commandRepo.createRole("ADMIN")).thenReturn(Future.succeededFuture(role));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        // When
        Future<RoleResponse> result = commandService.createRole(req);

        // Then
        assertThat(result.succeeded()).isTrue();
        RoleResponse response = result.result();
        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getName()).isEqualTo("ADMIN");
        verify(commandRepo).createRole("ADMIN");
    }

    @Test
    void createRole_shouldInvalidateListCache() {
        // Given
        CreateRoleRequest req = CreateRoleRequest.newBuilder().setName("ADMIN").build();
        Role role = createRole();
        when(commandRepo.createRole("ADMIN")).thenReturn(Future.succeededFuture(role));
        when(redisService.deleteByPattern("role:list:*")).thenReturn(Future.succeededFuture(1L));

        // When
        commandService.createRole(req);

        // Then
        verify(redisService).deleteByPattern("role:list:*");
    }

    @Test
    void updateRole_shouldUpdateAndReturnRoleResponse() {
        // Given
        UpdateRoleRequest req = UpdateRoleRequest.newBuilder().setId(1).setName("USER").build();
        Role updatedRole = Role.builder().roleId(1).roleName("USER").build();
        when(commandRepo.updateRole(1, "USER")).thenReturn(Future.succeededFuture(updatedRole));
        when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        // When
        Future<RoleResponse> result = commandService.updateRole(req);

        // Then
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getId()).isEqualTo(1);
        assertThat(result.result().getName()).isEqualTo("USER");
        verify(commandRepo).updateRole(1, "USER");
    }

    @Test
    void updateRole_shouldFailWhenRoleNotFound() {
        // Given
        UpdateRoleRequest req = UpdateRoleRequest.newBuilder().setId(99).setName("USER").build();
        when(commandRepo.updateRole(99, "USER")).thenReturn(Future.succeededFuture(null));

        // When
        Future<RoleResponse> result = commandService.updateRole(req);

        // Then
        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).isEqualTo("Role not found");
    }

    @Test
    void trashRole_shouldTrashAndReturnResponse() {
        // Given
        Long roleId = 1L;
        Role trashed = Role.builder().roleId(1).roleName("ADMIN").build();
        when(commandRepo.trashed(roleId)).thenReturn(Future.succeededFuture(trashed));
        when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        // When
        Future<RoleResponseDeleteAt> result = commandService.trashRole(roleId);

        // Then
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getId()).isEqualTo(1);
        assertThat(result.result().getName()).isEqualTo("ADMIN");
        verify(commandRepo).trashed(roleId);
    }

    @Test
    void trashRole_shouldFailWhenNotFound() {
        // Given
        Long roleId = 99L;
        when(commandRepo.trashed(roleId)).thenReturn(Future.succeededFuture(null));

        // When
        Future<RoleResponseDeleteAt> result = commandService.trashRole(roleId);

        // Then
        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).contains("Role not found");
    }

    @Test
    void restoreRole_shouldRestoreTrashedRole() {
        // Given
        Long roleId = 1L;
        Role trashed = Role.builder().roleId(1).roleName("ADMIN").build();
        Role restored = Role.builder().roleId(1).roleName("ADMIN").build();
        when(queryRepo.findByTrashedId(roleId)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepo.restore(roleId)).thenReturn(Future.succeededFuture(restored));
        when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        // When
        Future<RoleResponseDeleteAt> result = commandService.restoreRole(roleId);

        // Then
        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getId()).isEqualTo(1);
        assertThat(result.result().getName()).isEqualTo("ADMIN");
        verify(queryRepo).findByTrashedId(roleId);
        verify(commandRepo).restore(roleId);
    }

    @Test
    void restoreRole_shouldFailWhenNotTrashed() {
        // Given
        Long roleId = 99L;
        when(queryRepo.findByTrashedId(roleId)).thenReturn(Future.succeededFuture(null));

        // When
        Future<RoleResponseDeleteAt> result = commandService.restoreRole(roleId);

        // Then
        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("must be trashed first");
    }

    @Test
    void deletePermanent_shouldDeleteTrashedRole() {
        // Given
        Long roleId = 1L;
        Role trashed = Role.builder().roleId(1).roleName("ADMIN").build();
        when(queryRepo.findByTrashedId(roleId)).thenReturn(Future.succeededFuture(trashed));
        when(commandRepo.deletePermanent(roleId)).thenReturn(Future.succeededFuture(true));
        when(redisService.delete(anyString())).thenReturn(Future.succeededFuture(1L));
        lenient().when(redisService.deleteByPattern(anyString())).thenReturn(Future.succeededFuture(1L));

        // When
        Future<Void> result = commandService.deletePermanent(roleId);

        // Then
        assertThat(result.succeeded()).isTrue();
        verify(queryRepo).findByTrashedId(roleId);
        verify(commandRepo).deletePermanent(roleId);
        verify(redisService, atLeastOnce()).delete(anyString());
    }

    @Test
    void deletePermanent_shouldFailWhenNotTrashed() {
        // Given
        Long roleId = 99L;
        when(queryRepo.findByTrashedId(roleId)).thenReturn(Future.succeededFuture(null));

        // When
        Future<Void> result = commandService.deletePermanent(roleId);

        // Then
        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(BadRequestException.class);
        assertThat(result.cause().getMessage()).contains("must be trashed before permanent deletion");
    }

    @Test
    void restoreAllRoles_shouldRestoreAll() {
        // Given
        when(commandRepo.restoreAllRoles()).thenReturn(Future.succeededFuture(5));
        when(redisService.deleteByPattern("role:list:*")).thenReturn(Future.succeededFuture(1L));

        // When
        Future<Void> result = commandService.restoreAllRoles();

        // Then
        assertThat(result.succeeded()).isTrue();
        verify(commandRepo).restoreAllRoles();
        verify(redisService).deleteByPattern("role:list:*");
    }

    @Test
    void restoreAllRoles_shouldFailWhenNoneTrashed() {
        // Given
        when(commandRepo.restoreAllRoles()).thenReturn(Future.succeededFuture(0));

        // When
        Future<Void> result = commandService.restoreAllRoles();

        // Then
        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).isEqualTo("No trashed roles found");
    }

    @Test
    void deleteAllPermanentRoles_shouldDeleteAll() {
        // Given
        when(commandRepo.deleteAllPermanentRoles()).thenReturn(Future.succeededFuture(3));
        when(redisService.deleteByPattern("role:list:*")).thenReturn(Future.succeededFuture(1L));

        // When
        Future<Void> result = commandService.deleteAllPermanentRoles();

        // Then
        assertThat(result.succeeded()).isTrue();
        verify(commandRepo).deleteAllPermanentRoles();
        verify(redisService).deleteByPattern("role:list:*");
    }

    @Test
    void deleteAllPermanentRoles_shouldFailWhenNoneTrashed() {
        // Given
        when(commandRepo.deleteAllPermanentRoles()).thenReturn(Future.succeededFuture(0));

        // When
        Future<Void> result = commandService.deleteAllPermanentRoles();

        // Then
        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
        assertThat(result.cause().getMessage()).isEqualTo("No trashed roles found");
    }
}
