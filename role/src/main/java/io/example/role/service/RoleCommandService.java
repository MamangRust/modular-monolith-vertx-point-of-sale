package io.example.role.service;

import io.example.role.domain.response.RoleResponse;
import io.example.role.domain.response.RoleResponseDeleteAt;
import io.vertx.core.Future;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;

public interface RoleCommandService {
    Future<RoleResponse> createRole(CreateRoleRequest req);

    Future<RoleResponse> updateRole(UpdateRoleRequest req);

    Future<RoleResponseDeleteAt> trashRole(Long roleId);

    Future<RoleResponseDeleteAt> restoreRole(Long roleId);

    Future<Void> deletePermanent(Long roleId);

    Future<Void> restoreAllRoles();

    Future<Void> deleteAllPermanentRoles();
}
