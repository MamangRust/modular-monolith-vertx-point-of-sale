package io.example.role.repository;

import io.example.role.model.Role;
import io.vertx.core.Future;

public interface RoleCommandRepository {
    Future<Role> createRole(String name);

    Future<Role> updateRole(Integer roleId, String name);

    Future<Role> trashed(Long roleId);

    Future<Role> restore(Long roleId);

    Future<Boolean> deletePermanent(Long roleId);

    Future<Integer> restoreAllRoles();

    Future<Integer> deleteAllPermanentRoles();
}
