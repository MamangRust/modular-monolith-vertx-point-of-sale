package io.example.role.service;

import java.util.List;

import io.example.common.domain.PagedResult;
import io.example.role.domain.response.RoleResponse;
import io.example.role.domain.response.RoleResponseDeleteAt;
import io.vertx.core.Future;
import pb.role.Role.FindAllRoleRequest;

public interface RoleQueryService {
    Future<PagedResult<RoleResponse>> getAllRoles(FindAllRoleRequest req);

    Future<PagedResult<RoleResponseDeleteAt>> getActiveRoles(FindAllRoleRequest req);

    Future<PagedResult<RoleResponseDeleteAt>> getTrashedRoles(FindAllRoleRequest req);

    Future<RoleResponse> getRoleById(Long roleId);

    Future<List<RoleResponse>> getRolesByUserId(Long userId);
}
