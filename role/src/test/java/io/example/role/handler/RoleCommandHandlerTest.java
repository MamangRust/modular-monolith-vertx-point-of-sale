package io.example.role.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;

import io.example.role.domain.response.RoleResponse;
import io.example.role.domain.response.RoleResponseDeleteAt;
import io.example.role.service.RoleCommandService;
import io.vertx.core.Future;
import pb.role.Role.ApiResponseRole;
import pb.role.Role.ApiResponseRoleDeleteAt;
import pb.role.Role.FindByIdRoleRequest;
import pb.role.RoleCommand.ApiResponseRoleAll;
import pb.role.RoleCommand.ApiResponseRoleDelete;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;

@ExtendWith(MockitoExtension.class)
class RoleCommandHandlerTest {

    @Mock
    private RoleCommandService commandService;

    private RoleCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new RoleCommandHandler(commandService);
    }

    @Test
    void createRole_shouldReturnApiResponse() {
        CreateRoleRequest request = CreateRoleRequest.newBuilder()
                .setName("ADMIN")
                .build();

        RoleResponse roleResp = new RoleResponse(1, "ADMIN", "2024-01-01", "2024-06-01");

        when(commandService.createRole(request)).thenReturn(Future.succeededFuture(roleResp));

        Future<ApiResponseRole> result = commandHandler.createRole(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Role created successfully");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getName()).isEqualTo("ADMIN");
        verify(commandService).createRole(request);
    }

    @Test
    void updateRole_shouldReturnApiResponse() {
        UpdateRoleRequest request = UpdateRoleRequest.newBuilder()
                .setId(1)
                .setName("USER")
                .build();

        RoleResponse roleResp = new RoleResponse(1, "USER", "2024-01-01", "2024-06-01");

        when(commandService.updateRole(request)).thenReturn(Future.succeededFuture(roleResp));

        Future<ApiResponseRole> result = commandHandler.updateRole(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Role updated successfully");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getName()).isEqualTo("USER");
        verify(commandService).updateRole(request);
    }

    @Test
    void trashRole_shouldReturnApiResponse() {
        FindByIdRoleRequest request = FindByIdRoleRequest.newBuilder()
                .setRoleId(1)
                .build();

        RoleResponseDeleteAt roleDelResp = new RoleResponseDeleteAt(1, "ADMIN", "2024-01-01", "2024-06-01", null);

        when(commandService.trashRole(1L)).thenReturn(Future.succeededFuture(roleDelResp));

        Future<ApiResponseRoleDeleteAt> result = commandHandler.trashedRole(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Role trashed successfully");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getName()).isEqualTo("ADMIN");
        verify(commandService).trashRole(1L);
    }

    @Test
    void restoreRole_shouldReturnApiResponse() {
        FindByIdRoleRequest request = FindByIdRoleRequest.newBuilder()
                .setRoleId(1)
                .build();

        RoleResponseDeleteAt roleDelResp = new RoleResponseDeleteAt(1, "ADMIN", "2024-01-01", "2024-06-01", null);

        when(commandService.restoreRole(1L)).thenReturn(Future.succeededFuture(roleDelResp));

        Future<ApiResponseRoleDeleteAt> result = commandHandler.restoreRole(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Role restored successfully");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getName()).isEqualTo("ADMIN");
        verify(commandService).restoreRole(1L);
    }

    @Test
    void deleteRolePermanent_shouldReturnApiResponse() {
        FindByIdRoleRequest request = FindByIdRoleRequest.newBuilder()
                .setRoleId(1)
                .build();

        when(commandService.deletePermanent(1L)).thenReturn(Future.succeededFuture());

        Future<ApiResponseRoleDelete> result = commandHandler.deleteRolePermanent(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Role permanently deleted successfully");
        verify(commandService).deletePermanent(1L);
    }

    @Test
    void restoreAllRole_shouldReturnApiResponse() {
        when(commandService.restoreAllRoles()).thenReturn(Future.succeededFuture());

        Future<ApiResponseRoleAll> result = commandHandler.restoreAllRole(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("All roles restored successfully");
        verify(commandService).restoreAllRoles();
    }

    @Test
    void deleteAllRolePermanent_shouldReturnApiResponse() {
        when(commandService.deleteAllPermanentRoles()).thenReturn(Future.succeededFuture());

        Future<ApiResponseRoleAll> result = commandHandler.deleteAllRolePermanent(Empty.getDefaultInstance());

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("All roles permanently deleted successfully");
        verify(commandService).deleteAllPermanentRoles();
    }
}
