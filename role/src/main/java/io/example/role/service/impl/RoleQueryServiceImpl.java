package io.example.role.service.impl;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.example.common.domain.PagedResult;
import io.example.common.exception.api.NotFoundException;
import io.example.common.observability.TracingMetrics;
import io.example.common.service.RedisService;
import io.example.role.domain.requests.role.FindAllRoles;
import io.example.role.domain.response.RoleResponse;
import io.example.role.domain.response.RoleResponseDeleteAt;
import io.example.role.model.Role;
import io.example.role.repository.RoleQueryRepository;
import io.example.role.service.RoleQueryService;
import io.opentelemetry.api.common.Attributes;
import io.vertx.core.Future;
import lombok.RequiredArgsConstructor;
import pb.role.Role.FindAllRoleRequest;

@RequiredArgsConstructor
public class RoleQueryServiceImpl implements RoleQueryService {
  private static final Logger log = LoggerFactory.getLogger(RoleQueryServiceImpl.class);
  private static final ObjectMapper mapper = new ObjectMapper();

  private final RoleQueryRepository queryRepository;
  private final RedisService redis;
  private final TracingMetrics metrics;

  private static final String CACHE_PREFIX = "role:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);

  private PagedResult<RoleResponse> mapPagination(PagedResult<Role> res) {
    List<RoleResponse> data = res.getData().stream().map(RoleResponse::from).toList();
    return new PagedResult<>(data, res.getTotalRecords());
  }

  private PagedResult<RoleResponseDeleteAt> mapPaginationDeleteAt(PagedResult<Role> res) {
    List<RoleResponseDeleteAt> data = res.getData().stream().map(RoleResponseDeleteAt::from).toList();
    return new PagedResult<>(data, res.getTotalRecords());
  }

  @Override
  public Future<PagedResult<RoleResponse>> getAllRoles(FindAllRoleRequest req) {
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    var ctx = metrics.startSpan("RoleQueryService.getAllRoles");
    String cacheKey = CACHE_PREFIX + "list:all:" + keyword + ":" + page + ":" + pageSize;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              PagedResult<Role> typedCached = mapper.readValue(jsonStr,
                  new TypeReference<PagedResult<Role>>() {
                  });
              return Future.succeededFuture(mapPagination(typedCached));
            } catch (Exception e) {
              log.warn("Failed to deserialize cached roles: {}", e.getMessage());
            }
          }
          FindAllRoles domainReq = new FindAllRoles();
          domainReq.setPage(page);
          domainReq.setPageSize(pageSize);
          domainReq.setSearch(keyword);
          return queryRepository.getActiveRoles(domainReq)
              .map(res -> { redis.setJson(cacheKey, (Object) res, CACHE_TTL); return mapPagination(res); });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getAllRoles", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getAllRoles", e.getMessage()));
  }

  @Override
  public Future<PagedResult<RoleResponseDeleteAt>> getActiveRoles(FindAllRoleRequest req) {
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    var ctx = metrics.startSpan("RoleQueryService.getActiveRoles");
    String cacheKey = CACHE_PREFIX + "list:active:" + keyword + ":" + page + ":" + pageSize;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              PagedResult<Role> typedCached = mapper.readValue(jsonStr,
                  new TypeReference<PagedResult<Role>>() {
                  });
              return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
            } catch (Exception e) {
              log.warn("Failed to deserialize cached active roles: {}", e.getMessage());
            }
          }
          FindAllRoles domainReq = new FindAllRoles();
          domainReq.setPage(page);
          domainReq.setPageSize(pageSize);
          domainReq.setSearch(keyword);
          return queryRepository.getActiveRoles(domainReq)
              .map(res -> { redis.setJson(cacheKey, (Object) res, CACHE_TTL); return mapPaginationDeleteAt(res); });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getActiveRoles", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getActiveRoles", e.getMessage()));
  }

  @Override
  public Future<PagedResult<RoleResponseDeleteAt>> getTrashedRoles(FindAllRoleRequest req) {
    int page = req.getPage() > 0 ? req.getPage() : 1;
    int pageSize = req.getPageSize() > 0 ? req.getPageSize() : 10;
    String keyword = (req.getSearch() != null && !req.getSearch().isEmpty()) ? req.getSearch() : "";

    var ctx = metrics.startSpan("RoleQueryService.getTrashedRoles");
    String cacheKey = CACHE_PREFIX + "list:trashed:" + keyword + ":" + page + ":" + pageSize;

    return redis.get(cacheKey)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              PagedResult<Role> typedCached = mapper.readValue(jsonStr,
                  new TypeReference<PagedResult<Role>>() {
                  });
              return Future.succeededFuture(mapPaginationDeleteAt(typedCached));
            } catch (Exception e) {
              log.warn("Failed to deserialize cached trashed roles: {}", e.getMessage());
            }
          }
          FindAllRoles domainReq = new FindAllRoles();
          domainReq.setPage(page);
          domainReq.setPageSize(pageSize);
          domainReq.setSearch(keyword);
          return queryRepository.getTrashedRoles(domainReq)
              .map(res -> { redis.setJson(cacheKey, (Object) res, CACHE_TTL); return mapPaginationDeleteAt(res); });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getTrashedRoles", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getTrashedRoles", e.getMessage()));
  }

  @Override
  public Future<RoleResponse> getRoleById(Long roleId) {
    var ctx = metrics.startSpan("RoleQueryService.getRoleById",
        Attributes.builder().put("role.id", roleId).build());
    String key = CACHE_PREFIX + "id:" + roleId;

    return redis.getJson(key, Role.class)
        .compose(cached -> {
          if (cached != null) {
            return Future.succeededFuture(RoleResponse.from(cached));
          }
          return queryRepository.getRoleById(roleId)
              .compose(db -> {
                if (db == null) {
                  return Future.<Role>failedFuture(new NotFoundException("Role not found"));
                }
                return redis.setJson(key, db, CACHE_TTL).<Role>map(v -> db);
              })
              .map(RoleResponse::from);
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getRoleById", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getRoleById", e.getMessage()));
  }

  @Override
  public Future<List<RoleResponse>> getRolesByUserId(Long userId) {
    var ctx = metrics.startSpan("RoleQueryService.getRolesByUserId",
        Attributes.builder().put("user.id", userId).build());
    String key = CACHE_PREFIX + "user:" + userId;

    return redis.get(key)
        .compose(jsonStr -> {
          if (jsonStr != null && !jsonStr.isEmpty()) {
            try {
              List<Role> cachedList = mapper.readValue(jsonStr, new TypeReference<List<Role>>() {
              });
              return Future.succeededFuture(cachedList.stream().map(RoleResponse::from).toList());
            } catch (Exception e) {
              log.warn("Failed to deserialize cached roles by user: {}", e.getMessage());
            }
          }
          return queryRepository.getRolesByUserId(userId)
              .compose(dbList -> {
                if (dbList == null || dbList.isEmpty()) {
                  return Future.failedFuture(new NotFoundException("Roles not found for user id: " + userId));
                }
                return redis.setJson(key, dbList, CACHE_TTL)
                    .map(v -> dbList.stream().map(RoleResponse::from).toList());
              });
        })
        .onSuccess(r -> metrics.completeSpanSuccess(ctx, "getRolesByUserId", "Success"))
        .onFailure(e -> metrics.completeSpanError(ctx, "getRolesByUserId", e.getMessage()));
  }
}