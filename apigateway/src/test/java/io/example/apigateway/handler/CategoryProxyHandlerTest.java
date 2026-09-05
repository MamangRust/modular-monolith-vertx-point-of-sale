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

import pb.category.Category;
import pb.category.CategoryQuery;
import pb.category.CategoryCommand;
import pb.category.VertxCategoryServiceGrpcClient;
import pb.category.VertxCategoryCommandServiceGrpcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryProxyHandlerTest {
    @Mock VertxCategoryServiceGrpcClient queryClient;
    @Mock VertxCategoryCommandServiceGrpcClient commandClient;
    @Mock RoutingContext ctx;
    @Mock io.vertx.core.http.HttpServerResponse response;
    @Mock io.vertx.ext.web.RequestBody body;
    private CategoryProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CategoryProxyHandler(queryClient, commandClient);
    }

    @Test
    void findAll_shouldBuildRequestFromQueryParams() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("search","test").add("page","2").add("pageSize","20"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findAll(any())).thenReturn(Future.succeededFuture(CategoryQuery.ApiResponsePaginationCategory.getDefaultInstance()));
        handler.findAll(ctx);
        var captor = ArgumentCaptor.forClass(Category.FindAllCategoryRequest.class);
        verify(queryClient).findAll(captor.capture());
        assertThat(captor.getValue().getSearch()).isEqualTo("test");
        assertThat(captor.getValue().getPage()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(20);
        verify(response).setStatusCode(200);
        verify(response).end(anyString());
    }

    @Test
    void findById_shouldBuildRequestFromPathParam() {
        when(ctx.pathParam("id")).thenReturn("42");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findById(any())).thenReturn(Future.succeededFuture(Category.ApiResponseCategory.getDefaultInstance()));
        handler.findById(ctx);
        var captor = ArgumentCaptor.forClass(Category.FindByIdCategoryRequest.class);
        verify(queryClient).findById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(42);
        verify(response).setStatusCode(200);
    }

    @Test
    void create_shouldBuildRequestFromBody() {
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(new JsonObject().put("name","Food").put("description","Edible items"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(commandClient.create(any())).thenReturn(Future.succeededFuture(Category.ApiResponseCategory.getDefaultInstance()));
        handler.create(ctx);
        var captor = ArgumentCaptor.forClass(CategoryCommand.CreateCategoryRequest.class);
        verify(commandClient).create(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Food");
        assertThat(captor.getValue().getDescription()).isEqualTo("Edible items");
        verify(response).setStatusCode(201);
    }

    @Test
    void restoreAll_shouldCallGrpc() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(commandClient.restoreAllCategory(any())).thenReturn(Future.succeededFuture(CategoryCommand.ApiResponseCategoryAll.getDefaultInstance()));
        handler.restoreAll(ctx);
        verify(commandClient).restoreAllCategory(any(com.google.protobuf.Empty.class));
        verify(response).setStatusCode(200);
    }
}
