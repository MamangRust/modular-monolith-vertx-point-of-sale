package io.example.apigateway.handler;

import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pb.product.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductProxyHandlerTest {
    @Mock VertxProductServiceGrpcClient queryClient;
    @Mock VertxProductCommandServiceGrpcClient commandClient;
    @Mock RoutingContext ctx;
    @Mock io.vertx.core.http.HttpServerResponse response;
    @Mock io.vertx.core.http.HttpServerRequest request;
    private ProductProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ProductProxyHandler(queryClient, commandClient);
    }

    @Test
    void findAll_shouldBuildRequestFromQueryParams() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("search","test").add("page","1").add("pageSize","10"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findAll(any())).thenReturn(Future.succeededFuture(ProductQuery.ApiResponsePaginationProduct.getDefaultInstance()));
        handler.findAll(ctx);
        verify(queryClient).findAll(any(Product.FindAllProductRequest.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void findById_shouldBuildRequestFromPathParam() {
        when(ctx.pathParam("id")).thenReturn("42");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findById(any())).thenReturn(Future.succeededFuture(Product.ApiResponseProduct.getDefaultInstance()));
        handler.findById(ctx);
        var captor = ArgumentCaptor.forClass(Product.FindByIdProductRequest.class);
        verify(queryClient).findById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(42);
        verify(response).setStatusCode(200);
    }

    @Test
    void findByMerchant_shouldBuildRequestWithFilters() {
        when(ctx.pathParam("merchantId")).thenReturn("7");
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("search","test").add("categoryId","3").add("minPrice","1000").add("maxPrice","50000"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(queryClient.findByMerchant(any())).thenReturn(Future.succeededFuture(ProductQuery.ApiResponsePaginationProduct.getDefaultInstance()));
        handler.findByMerchant(ctx);
        var captor = ArgumentCaptor.forClass(Product.FindAllProductMerchantRequest.class);
        verify(queryClient).findByMerchant(captor.capture());
        assertThat(captor.getValue().getMerchantId()).isEqualTo(7);
        assertThat(captor.getValue().getCategoryId()).isEqualTo(3);
        verify(response).setStatusCode(200);
    }

    @Test
    void create_shouldBuildRequestFromFormParams() {
        when(ctx.request()).thenReturn(request);
        when(request.getFormAttribute("image_product")).thenReturn("");
        when(request.getFormAttribute("description")).thenReturn("Description");
        when(request.getFormAttribute("merchant_id")).thenReturn("1");
        when(request.getFormAttribute("category_id")).thenReturn("2");
        when(request.getFormAttribute("name")).thenReturn("Product A");
        when(request.getFormAttribute("price")).thenReturn("25000");
        when(request.getFormAttribute("count_in_stock")).thenReturn("10");
        when(request.getFormAttribute("brand")).thenReturn("Brand");
        when(request.getFormAttribute("weight")).thenReturn("100");
        when(ctx.fileUploads()).thenReturn(java.util.Collections.emptyList());
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(commandClient.create(any())).thenReturn(Future.succeededFuture(Product.ApiResponseProduct.getDefaultInstance()));
        handler.create(ctx);
        verify(commandClient).create(any(ProductCommand.CreateProductRequest.class));
        verify(response).setStatusCode(201);
    }

    @Test
    void restoreAll_shouldCallGrpc() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(commandClient.restoreAllProduct(any())).thenReturn(Future.succeededFuture(ProductCommand.ApiResponseProductAll.getDefaultInstance()));
        handler.restoreAll(ctx);
        verify(commandClient).restoreAllProduct(any(com.google.protobuf.Empty.class));
        verify(response).setStatusCode(200);
    }
}
