package io.example.apigateway.middleware;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleMiddlewareTest {

    @Mock
    RoutingContext ctx;
    @Mock
    User user;
    @Mock
    io.vertx.core.http.HttpServerResponse response;

    @Test
    void requireRole_shouldCallNextWhenUserHasRole() {
        var principal = new JsonObject().put("roleNames", new JsonArray().add("ADMIN"));
        when(ctx.user()).thenReturn(user);
        when(user.principal()).thenReturn(principal);

        RoleMiddleware.requireRole("ADMIN").handle(ctx);

        verify(ctx).next();
    }

    @Test
    void requireRole_shouldReturn401WhenUserIsNull() {
        when(ctx.user()).thenReturn(null);
        when(ctx.response()).thenReturn(response);
        when(response.setStatusCode(401)).thenReturn(response);

        var handler = RoleMiddleware.requireRole("ADMIN");
        handler.handle(ctx);

        verify(response).setStatusCode(401);
        verify(response).end("Unauthorized");
    }

    @Test
    void requireRole_shouldReturn401WhenPrincipalIsNull() {
        when(ctx.user()).thenReturn(user);
        when(user.principal()).thenReturn(null);
        when(ctx.response()).thenReturn(response);
        when(response.setStatusCode(401)).thenReturn(response);

        RoleMiddleware.requireRole("ADMIN").handle(ctx);

        verify(response).setStatusCode(401);
        verify(response).end("Unauthorized");
    }

    @Test
    void requireRole_shouldReturn403WhenRoleNamesIsNull() {
        var principal = new JsonObject(); // no roleNames key
        when(ctx.user()).thenReturn(user);
        when(user.principal()).thenReturn(principal);
        when(ctx.response()).thenReturn(response);
        when(response.setStatusCode(403)).thenReturn(response);

        RoleMiddleware.requireRole("ADMIN").handle(ctx);

        verify(response).setStatusCode(403);
        verify(response).end("Forbidden");
    }

    @Test
    void requireRole_shouldReturn403WhenUserLacksRole() {
        var principal = new JsonObject().put("roleNames", new JsonArray().add("USER"));
        when(ctx.user()).thenReturn(user);
        when(user.principal()).thenReturn(principal);
        when(ctx.response()).thenReturn(response);
        when(response.setStatusCode(403)).thenReturn(response);

        RoleMiddleware.requireRole("ADMIN").handle(ctx);

        verify(response).setStatusCode(403);
        verify(response).end("Forbidden");
    }
}
