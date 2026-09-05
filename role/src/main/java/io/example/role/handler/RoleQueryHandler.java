package io.example.role.handler;

import io.example.common.grpc.GrpcExceptionMapper;
import io.example.role.service.RoleQueryService;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.role.Role.ApiResponseRole;
import pb.role.Role.ApiResponsesRole;
import pb.role.Role.FindAllRoleRequest;
import pb.role.Role.FindByIdRoleRequest;
import pb.role.Role.FindByIdUserRoleRequest;
import pb.role.RoleQuery.ApiResponsePaginationRole;
import pb.role.RoleQuery.ApiResponsePaginationRoleDeleteAt;

@RequiredArgsConstructor
public class RoleQueryHandler implements pb.role.VertxRoleServiceGrpcServer.RoleServiceApi {
  private final RoleQueryService service;

  private pb.common.PaginationMeta toMeta(int page, int pageSize, int totalRecords) {
    int currentPage = page > 0 ? page : 1;
    int size = pageSize > 0 ? pageSize : 10;
    int totalPages = size > 0 ? (int) Math.ceil((double) totalRecords / size) : 0;
    return pb.common.PaginationMeta.newBuilder()
        .setCurrentPage(currentPage)
        .setPageSize(size)
        .setTotalPages(totalPages)
        .setTotalRecords(totalRecords)
        .build();
  }

  @Override
  public Future<ApiResponsePaginationRole> findAllRole(FindAllRoleRequest req) {
    return service.getAllRoles(req)
        .map(resp -> ApiResponsePaginationRole.newBuilder()
            .setStatus("success")
            .setMessage("Roles retrieved successfully")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromRoleResponse).toList())
            .setPagination(toMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponseRole> findByIdRole(FindByIdRoleRequest req) {
    return service.getRoleById((long) req.getRoleId())
        .map(resp -> ApiResponseRole.newBuilder()
            .setStatus("success")
            .setMessage("Role found successfully")
            .setData(ProtoConverter.fromRoleResponse(resp))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationRoleDeleteAt> findByActive(FindAllRoleRequest req) {
    return service.getActiveRoles(req)
        .map(resp -> ApiResponsePaginationRoleDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("Active roles retrieved successfully")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromRoleResponseDeleteAt).toList())
            .setPagination(toMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsePaginationRoleDeleteAt> findByTrashed(FindAllRoleRequest req) {
    return service.getTrashedRoles(req)
        .map(resp -> ApiResponsePaginationRoleDeleteAt.newBuilder()
            .setStatus("success")
            .setMessage("Trashed roles retrieved successfully")
            .addAllData(resp.getData().stream().map(ProtoConverter::fromRoleResponseDeleteAt).toList())
            .setPagination(toMeta(req.getPage(), req.getPageSize(), resp.getTotalRecords()))
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }

  @Override
  public Future<ApiResponsesRole> findByUserId(FindByIdUserRoleRequest req) {
    return service.getRolesByUserId((long) req.getUserId())
        .map(resp -> ApiResponsesRole.newBuilder()
            .setStatus("success")
            .setMessage("Roles by user fetched successfully")
            .addAllData(resp.stream().map(ProtoConverter::fromRoleResponse).toList())
            .build())
        .recover(GrpcExceptionMapper::toFailedFuture);
  }
}