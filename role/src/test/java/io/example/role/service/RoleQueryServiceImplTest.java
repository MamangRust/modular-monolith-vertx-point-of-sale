package io.example.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.role.domain.response.RoleResponse;
import io.example.role.domain.response.RoleResponseDeleteAt;
import io.example.role.model.Role;
import io.example.role.repository.RoleQueryRepository;
import io.example.role.service.impl.RoleQueryServiceImpl;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import pb.role.Role.FindAllRoleRequest;
import pb.role.Role.FindByIdRoleRequest;

@ExtendWith(MockitoExtension.class)
class RoleQueryServiceImplTest {

    @Mock private RoleQueryRepository queryRepo;
    @Mock private RedisService redis;
    @Mock private TracingMetrics metrics;
    @Mock private TracingMetrics.TracingContext tracingContext;

    private RoleQueryServiceImpl queryService;

    @BeforeEach
    void setUp() {
        lenient().when(metrics.startSpan(anyString())).thenReturn(tracingContext);
        lenient().when(metrics.startSpan(anyString(), any(Attributes.class))).thenReturn(tracingContext);
        lenient().when(redis.setJson(anyString(), any(Object.class), any())).thenReturn(Future.succeededFuture("OK"));
        queryService = new RoleQueryServiceImpl(queryRepo, redis, metrics);
    }

    private Role createRole() {
        return Role.builder().roleId(1).roleName("ADMIN").build();
    }

    // ── getAllRoles ────────────────────────────────────────────────────

    @Test
    void getAllRoles_shouldFetchFromDb() {
        var req = FindAllRoleRequest.newBuilder().setSearch("ADMIN").setPage(1).setPageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepo.getRoles(any())).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createRole()), 1)));

        Future<PagedResult<RoleResponse>> result = queryService.getAllRoles(req);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        assertThat(result.result().getData().get(0).getName()).isEqualTo("ADMIN");
        verify(queryRepo).getRoles(any());
    }

    @Test
    void getAllRoles_shouldHandleEmptyResult() {
        var req = FindAllRoleRequest.newBuilder().setSearch("").setPage(1).setPageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepo.getRoles(any())).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(), 0)));

        Future<PagedResult<RoleResponse>> result = queryService.getAllRoles(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).isEmpty();
    }

    // ── getActiveRoles ─────────────────────────────────────────────────

    @Test
    void getActiveRoles_shouldFetchFromDb() {
        var req = FindAllRoleRequest.newBuilder().setPage(1).setPageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepo.getActiveRoles(any())).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createRole()), 1)));

        Future<PagedResult<RoleResponseDeleteAt>> result = queryService.getActiveRoles(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).getActiveRoles(any());
    }

    // ── getTrashedRoles ────────────────────────────────────────────────

    @Test
    void getTrashedRoles_shouldFetchFromDb() {
        var req = FindAllRoleRequest.newBuilder().setSearch("old").setPage(1).setPageSize(10).build();
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepo.getTrashedRoles(any())).thenReturn(Future.succeededFuture(new PagedResult<>(List.of(createRole()), 1)));

        Future<PagedResult<RoleResponseDeleteAt>> result = queryService.getTrashedRoles(req);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getData()).hasSize(1);
        verify(queryRepo).getTrashedRoles(any());
    }

    // ── getRoleById ────────────────────────────────────────────────────

    @Test
    void getRoleById_shouldFetchFromDb() {
        Long roleId = 1L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getRoleById(roleId)).thenReturn(Future.succeededFuture(createRole()));

        Future<RoleResponse> result = queryService.getRoleById(roleId);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result().getId()).isEqualTo(1);
        assertThat(result.result().getName()).isEqualTo("ADMIN");
        verify(queryRepo).getRoleById(roleId);
    }

    @Test
    void getRoleById_shouldReturnFromCache() {
        Long roleId = 1L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(createRole()));

        Future<RoleResponse> result = queryService.getRoleById(roleId);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getId()).isEqualTo(1);
        verify(queryRepo, never()).getRoleById(anyLong());
    }

    @Test
    void getRoleById_shouldFailWhenNotFound() {
        Long roleId = 99L;
        when(redis.getJson(anyString(), any())).thenReturn(Future.succeededFuture(null));
        when(queryRepo.getRoleById(roleId)).thenReturn(Future.succeededFuture(null));

        Future<RoleResponse> result = queryService.getRoleById(roleId);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    // ── getRolesByUserId ──────────────────────────────────────────────

    @Test
    void getRolesByUserId_shouldFetchFromDb() {
        Long userId = 1L;
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepo.getRolesByUserId(userId)).thenReturn(Future.succeededFuture(List.of(createRole())));

        Future<List<RoleResponse>> result = queryService.getRolesByUserId(userId);

        assertThat(result.succeeded()).as("Failed: " + (result.failed() ? result.cause().getMessage() : "")).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getName()).isEqualTo("ADMIN");
        verify(queryRepo).getRolesByUserId(userId);
    }

    @Test
    void getRolesByUserId_shouldFailWhenNotFound() {
        Long userId = 99L;
        when(redis.get(anyString())).thenReturn(Future.succeededFuture((String) null));
        when(queryRepo.getRolesByUserId(userId)).thenReturn(Future.succeededFuture(List.of()));

        Future<List<RoleResponse>> result = queryService.getRolesByUserId(userId);

        assertThat(result.failed()).isTrue();
        assertThat(result.cause()).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getRolesByUserId_shouldReturnFromCache() {
        Long userId = 1L;
        var cachedJson = "[{\"roleId\":1,\"roleName\":\"ADMIN\"}]";
        when(redis.get(anyString())).thenReturn(Future.succeededFuture(cachedJson));

        Future<List<RoleResponse>> result = queryService.getRolesByUserId(userId);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).getName()).isEqualTo("ADMIN");
    }
}
