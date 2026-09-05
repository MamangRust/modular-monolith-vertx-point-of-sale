package io.example.role.repository;

import java.util.List;
import io.example.common.domain.PagedResult;
import io.example.role.domain.requests.role.FindAllRoles;
import io.example.role.model.Role;
import io.vertx.core.Future;

public interface RoleQueryRepository {
    Future<PagedResult<Role>> getRoles(FindAllRoles req);

    Future<PagedResult<Role>> getActiveRoles(FindAllRoles req);

    Future<PagedResult<Role>> getTrashedRoles(FindAllRoles req);

    Future<Role> getRoleById(Long roleId);

    Future<Role> findByTrashedId(Long roleId);

    Future<Role> getRoleByName(String roleName);

    Future<List<Role>> getRolesByUserId(Long userId);
}
