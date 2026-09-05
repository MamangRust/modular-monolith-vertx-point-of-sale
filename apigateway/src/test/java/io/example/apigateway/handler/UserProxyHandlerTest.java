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

import pb.user.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProxyHandlerTest {
    @Mock VertxUserQueryServiceGrpcClient queryClient;
    @Mock VertxUserCommandServiceGrpcClient commandClient;
    @Mock RoutingContext ctx;
    @Mock io.vertx.core.http.HttpServerResponse response;
    @Mock io.vertx.ext.web.RequestBody body;
    private UserProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UserProxyHandler(queryClient, commandClient);
    }

    @Test
    void findAll_shouldBuildRequestFromQueryParams() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("search","user").add("page","1").add("pageSize","10"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findAll(any())).thenReturn(Future.succeededFuture(UserQuery.ApiResponsePaginationUser.getDefaultInstance()));
        handler.findAll(ctx);
        verify(queryClient).findAll(any(User.FindAllUserRequest.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void findById_shouldBuildRequestFromPathParam() {
        when(ctx.pathParam("id")).thenReturn("42");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findById(any())).thenReturn(Future.succeededFuture(User.ApiResponseUser.getDefaultInstance()));
        handler.findById(ctx);
        var captor = ArgumentCaptor.forClass(User.FindByIdUserRequest.class);
        verify(queryClient).findById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(42);
        verify(response).setStatusCode(200);
    }

    @Test
    void update_shouldBuildRequestFromBodyAndPath() {
        when(ctx.pathParam("id")).thenReturn("7");
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(new JsonObject().put("firstname","John").put("lastname","Doe").put("email","john@test.com"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(commandClient.update(any())).thenReturn(Future.succeededFuture(User.ApiResponseUser.getDefaultInstance()));
        handler.update(ctx);
        verify(commandClient).update(any(UserCommand.UpdateUserRequest.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void restoreAllUsers_shouldCallGrpc() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(commandClient.restoreAllUser(any())).thenReturn(Future.succeededFuture(UserCommand.ApiResponseUserAll.getDefaultInstance()));
        handler.restoreAllUsers(ctx);
        verify(commandClient).restoreAllUser(any(com.google.protobuf.Empty.class));
        verify(response).setStatusCode(200);
    }
}
