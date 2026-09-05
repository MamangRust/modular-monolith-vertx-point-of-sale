package io.example.role.handler;

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
import io.example.role.domain.response.RoleResponse;
import io.example.role.domain.response.RoleResponseDeleteAt;
import io.example.role.service.RoleQueryService;
import io.vertx.core.Future;
import pb.role.Role.ApiResponseRole;
import pb.role.Role.ApiResponsesRole;
import pb.role.Role.FindAllRoleRequest;
import pb.role.Role.FindByIdRoleRequest;
import pb.role.Role.FindByIdUserRoleRequest;
import pb.role.RoleQuery.ApiResponsePaginationRole;
import pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt;

@ExtendWith(MockitoExtension.class)
class RoleQueryHandlerTest {

    @Mock
    private RoleQueryService queryService;

    private RoleQueryHandler queryHandler;

    @BeforeEach
    void setUp() {
        queryHandler = new RoleQueryHandler(queryService);
    }

    @Test
    void findAllRole_shouldReturnPagedResponse() {
        FindAllRoleRequest request = FindAllRoleRequest.newBuilder()
                .setSearch("")
                .setPage(1)
                .setPageSize(10)
                .build();

        RoleResponse roleResp = new RoleResponse(1, "ADMIN", "2024-01-01", "2024-06-01");
        PagedResult<RoleResponse> paged = new PagedResult<>(List.of(roleResp), 1);

        when(queryService.getAllRoles(request)).thenReturn(Future.succeededFuture(paged));

        Future<ApiResponsePaginationRole> result = queryHandler.findAllRole(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Roles retrieved successfully");
        assertThat(result.result().getDataList()).hasSize(1);
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("ADMIN");
        assertThat(result.result().getPagination().getCurrentPage()).isEqualTo(1);
        assertThat(result.result().getPagination().getPageSize()).isEqualTo(10);
        assertThat(result.result().getPagination().getTotalPages()).isEqualTo(1);
        assertThat(result.result().getPagination().getTotalRecords()).isEqualTo(1);
        verify(queryService).getAllRoles(request);
    }

    @Test
    void findByIdRole_shouldReturnRoleResponse() {
        FindByIdRoleRequest request = FindByIdRoleRequest.newBuilder()
                .setRoleId(1)
                .build();

        RoleResponse roleResp = new RoleResponse(1, "ADMIN", "2024-01-01", "2024-06-01");

        when(queryService.getRoleById(1L)).thenReturn(Future.succeededFuture(roleResp));

        Future<ApiResponseRole> result = queryHandler.findByIdRole(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Role found successfully");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getName()).isEqualTo("ADMIN");
        verify(queryService).getRoleById(1L);
    }

    @Test
    void findByActive_shouldReturnPagedResponse() {
        FindAllRoleRequest request = FindAllRoleRequest.newBuilder()
                .setSearch("")
                .setPage(1)
                .setPageSize(10)
                .build();

        RoleResponseDeleteAt roleDelResp = new RoleResponseDeleteAt(1, "ADMIN", "2024-01-01", "2024-06-01", null);
        PagedResult<RoleResponseDeleteAt> pagedDel = new PagedResult<>(List.of(roleDelResp), 1);

        when(queryService.getActiveRoles(request)).thenReturn(Future.succeededFuture(pagedDel));

        Future<ApiResponsePaginationRoleDeleteAt> result = queryHandler.findByActive(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Active roles retrieved successfully");
        assertThat(result.result().getDataList()).hasSize(1);
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("ADMIN");
        assertThat(result.result().getPagination().getCurrentPage()).isEqualTo(1);
        assertThat(result.result().getPagination().getPageSize()).isEqualTo(10);
        assertThat(result.result().getPagination().getTotalPages()).isEqualTo(1);
        assertThat(result.result().getPagination().getTotalRecords()).isEqualTo(1);
        verify(queryService).getActiveRoles(request);
    }

    @Test
    void findByTrashed_shouldReturnPagedResponse() {
        FindAllRoleRequest request = FindAllRoleRequest.newBuilder()
                .setSearch("")
                .setPage(1)
                .setPageSize(10)
                .build();

        RoleResponseDeleteAt roleDelResp = new RoleResponseDeleteAt(1, "ADMIN", "2024-01-01", "2024-06-01", null);
        PagedResult<RoleResponseDeleteAt> pagedDel = new PagedResult<>(List.of(roleDelResp), 1);

        when(queryService.getTrashedRoles(request)).thenReturn(Future.succeededFuture(pagedDel));

        Future<ApiResponsePaginationRoleDeleteAt> result = queryHandler.findByTrashed(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Trashed roles retrieved successfully");
        assertThat(result.result().getDataList()).hasSize(1);
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("ADMIN");
        assertThat(result.result().getPagination().getCurrentPage()).isEqualTo(1);
        assertThat(result.result().getPagination().getPageSize()).isEqualTo(10);
        assertThat(result.result().getPagination().getTotalPages()).isEqualTo(1);
        assertThat(result.result().getPagination().getTotalRecords()).isEqualTo(1);
        verify(queryService).getTrashedRoles(request);
    }

    @Test
    void findByUserId_shouldReturnRoleList() {
        FindByIdUserRoleRequest request = FindByIdUserRoleRequest.newBuilder()
                .setUserId(1)
                .build();

        RoleResponse roleResp = new RoleResponse(1, "ADMIN", "2024-01-01", "2024-06-01");

        when(queryService.getRolesByUserId(1L)).thenReturn(Future.succeededFuture(List.of(roleResp)));

        Future<ApiResponsesRole> result = queryHandler.findByUserId(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("Roles by user fetched successfully");
        assertThat(result.result().getDataList()).hasSize(1);
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
        assertThat(result.result().getData(0).getName()).isEqualTo("ADMIN");
        verify(queryService).getRolesByUserId(1L);
    }
}
