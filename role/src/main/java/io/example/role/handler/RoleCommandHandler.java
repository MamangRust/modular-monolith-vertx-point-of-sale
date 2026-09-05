package io.example.role.handler;

import com.google.protobuf.Empty;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.role.service.RoleCommandService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.role.Role.ApiResponseRole;
import pb.role.Role.ApiResponseRoleDeleteAt;
import pb.role.Role.FindByIdRoleRequest;
import pb.role.RoleCommand.ApiResponseRoleAll;
import pb.role.RoleCommand.ApiResponseRoleDelete;
import pb.role.RoleCommand.CreateRoleRequest;
import pb.role.RoleCommand.UpdateRoleRequest;

@RequiredArgsConstructor
public class RoleCommandHandler implements pb.role.VertxRoleCommandServiceGrpcServer.RoleCommandServiceApi {
  private final RoleCommandService service;

  @Override
  public Future<ApiResponseRole> createRole(CreateRoleRequest req) {
    return service.createRole(req)
        .map(resp -> ApiResponseRole.newBuilder()
            .setStatus("success")
            .setMessage("Role created successfully")
            .setData(ProtoConverter.fromRoleResponse(resp))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRole> updateRole(UpdateRoleRequest req) {
    return service.updateRole(req)
        .map(resp -> ApiResponseRole.newBuilder()
            .setStatus("success")
            .setMessage("Role updated successfully")
            .setData(ProtoConverter.fromRoleResponse(resp))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRoleDeleteAt> trashedRole(FindByIdRoleRequest req) {
    return service.trashRole((long) req.getRoleId())
        .map(resp -> ApiResponseRoleDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("Role trashed successfully")
            .setData(ProtoConverter.fromRoleResponseDeleteAt(resp))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRoleDeleteAt> restoreRole(FindByIdRoleRequest req) {
    return service.restoreRole((long) req.getRoleId())
        .map(resp -> ApiResponseRoleDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("Role restored successfully")
            .setData(ProtoConverter.fromRoleResponseDeleteAt(resp))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRoleDelete> deleteRolePermanent(FindByIdRoleRequest req) {
    return service.deletePermanent((long) req.getRoleId())
        .map(resp -> ApiResponseRoleDelete.newBuilder()
            .setStatus("success")
            .setMessage("Role permanently deleted successfully")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRoleAll> restoreAllRole(Empty req) {
    return service.restoreAllRoles()
        .map(resp -> ApiResponseRoleAll.newBuilder()
            .setStatus("success")
            .setMessage("All roles restored successfully")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRoleAll> deleteAllRolePermanent(Empty req) {
    return service.deleteAllPermanentRoles()
        .map(resp -> ApiResponseRoleAll.newBuilder()
            .setStatus("success")
            .setMessage("All roles permanently deleted successfully")
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}