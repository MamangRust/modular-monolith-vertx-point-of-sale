package io.example.apigateway.handler;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.role.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleProxyHandlerTest {
    @Mock VertxRoleServiceGrpcClient queryClient;
    @Mock VertxRoleCommandServiceGrpcClient commandClient;
    @Mock RoutingContext ctx;
    @Mock io.vertx.core.http.HttpServerResponse response;
    @Mock io.vertx.ext.web.RequestBody body;
    private RoleProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RoleProxyHandler(queryClient, commandClient);
    }

    @Test
    void findAll_shouldBuildRequestFromQueryParams() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("search","admin").add("page","2").add("pageSize","20"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findAllRole(any())).thenReturn(Future.succeededFuture(RoleQuery.ApiResponsePaginationRole.getDefaultInstance()));
        handler.findAll(ctx);
        verify(queryClient).findAllRole(any(Role.FindAllRoleRequest.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void findById_shouldBuildRequestFromPathParam() {
        when(ctx.pathParam("id")).thenReturn("42");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findByIdRole(any())).thenReturn(Future.succeededFuture(Role.ApiResponseRole.getDefaultInstance()));
        handler.findById(ctx);
        var captor = ArgumentCaptor.forClass(Role.FindByIdRoleRequest.class);
        verify(queryClient).findByIdRole(captor.capture());
        assertThat(captor.getValue().getRoleId()).isEqualTo(42);
        verify(response).setStatusCode(200);
    }

    @Test
    void create_shouldBuildRequestFromBody() {
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(new JsonObject().put("name","Admin").put("description","Admin role"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(commandClient.createRole(any())).thenReturn(Future.succeededFuture(Role.ApiResponseRole.getDefaultInstance()));
        handler.create(ctx);
        verify(commandClient).createRole(any(RoleCommand.CreateRoleRequest.class));
        verify(response).setStatusCode(201);
    }

    @Test
    void restoreAll_shouldCallGrpc() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(commandClient.restoreAllRole(any())).thenReturn(Future.succeededFuture(RoleCommand.ApiResponseRoleAll.getDefaultInstance()));
        handler.restoreAllRoles(ctx);
        verify(commandClient).restoreAllRole(any(com.google.protobuf.Empty.class));
        verify(response).setStatusCode(200);
    }
}
