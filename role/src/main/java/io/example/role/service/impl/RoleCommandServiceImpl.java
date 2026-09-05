package io.example.role.service.impl;

import io.example.common.exception.grpc.BadRequestException;
import io.example.common.exception.grpc.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.role.domain.response.RoleResponse;
import io.example.role.domain.response.RoleResponseDeleteAt;
import io.example.role.model.Role;
import io.example.role.repository.RoleCommandRepository;
import io.example.role.repository.RoleQueryRepository;
import io.example.role.service.RoleCommandService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;

@Slf4j
@RequiredArgsConstructor
public class RoleCommandServiceImpl implements RoleCommandService {
  private final RoleCommandRepository commandRepository;
  private final RoleQueryRepository queryRepository;
  private final RedisService redisService;
  private final TracingMetrics tracingMetrics;

  private static final String CACHE_PREFIX = "role:";
  private static final String CACHE_LIST_PREFIX = "role:list:";

  @Override
  public Future<RoleResponse> createRole(CreateRoleRequest req) {
    String roleName = req.getName();
    if (roleName == null || roleName.isBlank()) {
      return Future.failedFuture(new BadRequestException("Role name is required"));
    }
    var ctx = tracingMetrics.startSpan("RoleCommandService.createRole",
        Attributes.builder().put("role.name", roleName).build());

    return commandRepository.createRole(roleName)
        .compose(created -> invalidateListCache().<Role>map(v -> created))
        .map(RoleResponse::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "create", "Role created successfully"))
        .onFailure(err -> {
          log.error("Failed to create role", err);
          tracingMetrics.completeSpanError(ctx, "create", err.getMessage());
        });
  }

  @Override
  public Future<RoleResponse> updateRole(UpdateRoleRequest req) {
    Integer roleId = req.getId();
    var ctx = tracingMetrics.startSpan("RoleCommandService.updateRole",
        Attributes.builder().put("role.id", roleId).build());

    return commandRepository.updateRole(roleId, req.getName())
        .compose(updatedRole -> {
          if (updatedRole == null) {
            return Future.failedFuture(new NotFoundException("Role not found"));
          }
          return invalidateCache(roleId).<Role>map(v -> updatedRole);
        })
        .map(RoleResponse::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "update", "Role updated successfully"))
        .onFailure(err -> {
          log.error("Failed to update role: {}", roleId, err);
          tracingMetrics.completeSpanError(ctx, "update", err.getMessage());
        });
  }

  @Override
  public Future<RoleResponseDeleteAt> trashRole(Long roleId) {
    var ctx = tracingMetrics.startSpan("RoleCommandService.trashRole",
        Attributes.builder().put("role.id", roleId).build());

    return commandRepository.trashed(roleId)
        .compose(role -> {
          if (role == null) {
            return Future.failedFuture(new NotFoundException("Role not found with id: " + roleId));
          }
          return invalidateCache(roleId.intValue()).<Role>map(v -> role);
        })
        .map(RoleResponseDeleteAt::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "trash", "Role trashed successfully"))
        .onFailure(err -> {
          log.error("Failed to trash role: {}", roleId, err);
          tracingMetrics.completeSpanError(ctx, "trash", err.getMessage());
        });
  }

  @Override
  public Future<RoleResponseDeleteAt> restoreRole(Long roleId) {
    var ctx = tracingMetrics.startSpan("RoleCommandService.restoreRole",
        Attributes.builder().put("role.id", roleId).build());

    return queryRepository.findByTrashedId(roleId)
        .compose(trashed -> {
          if (trashed == null) {
            return Future.failedFuture(new BadRequestException("Role not found or must be trashed first"));
          }
          return commandRepository.restore(roleId);
        })
        .compose(r -> {
          if (r == null) {
            return Future.<Role>failedFuture(new NotFoundException("Role not found with id: " + roleId));
          }
          return invalidateCache(roleId.intValue()).<Role>map(v -> r);
        })
        .map(RoleResponseDeleteAt::from)
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restoreRole", "Success"))
        .onFailure(e -> tracingMetrics.completeSpanError(ctx, "restoreRole", e.getMessage()));
  }

  @Override
  public Future<Void> deletePermanent(Long roleId) {
    var ctx = tracingMetrics.startSpan("RoleCommandService.deletePermanent",
        Attributes.builder().put("role.id", roleId).build());

    return queryRepository.findByTrashedId(roleId)
        .compose(trashed -> {
          if (trashed == null) {
            return Future.<Void>failedFuture(
                new BadRequestException("Role not found or must be trashed before permanent deletion"));
          }
          return commandRepository.deletePermanent(roleId)
              .compose(v -> invalidateCache(roleId.intValue()));
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "deletePermanent", "Role permanently deleted"))
        .onFailure(err -> tracingMetrics.completeSpanError(ctx, "deletePermanent", err.getMessage()));
  }

  @Override
  public Future<Void> restoreAllRoles() {
    var ctx = tracingMetrics.startSpan("RoleCommandService.restoreAllRoles");

    return commandRepository.restoreAllRoles()
        .compose(count -> {
          if (count == 0) {
            return Future.failedFuture(new NotFoundException("No trashed roles found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "restore_all", "All roles restored successfully"))
        .onFailure(err -> {
          log.error("Failed to restore all roles", err);
          tracingMetrics.completeSpanError(ctx, "restore_all", err.getMessage());
        });
  }

  @Override
  public Future<Void> deleteAllPermanentRoles() {
    var ctx = tracingMetrics.startSpan("RoleCommandService.deleteAllPermanentRoles");

    return commandRepository.deleteAllPermanentRoles()
        .compose(count -> {
          if (count == 0) {
            return Future.failedFuture(new NotFoundException("No trashed roles found"));
          }
          return invalidateListCache();
        })
        .onSuccess(v -> tracingMetrics.completeSpanSuccess(ctx, "delete_all", "All roles permanently deleted"))
        .onFailure(err -> {
          log.error("Failed to permanently delete all roles", err);
          tracingMetrics.completeSpanError(ctx, "delete_all", err.getMessage());
        });
  }

  private Future<Void> invalidateCache(Integer roleId) {
    return redisService.delete(CACHE_PREFIX + "id:" + roleId)
        .compose(v -> redisService.deleteByPattern(CACHE_LIST_PREFIX + "*"))
        .<Void>mapEmpty();
  }

  private Future<Void> invalidateListCache() {
    return redisService.deleteByPattern(CACHE_LIST_PREFIX + "*").<Void>mapEmpty();
  }
}