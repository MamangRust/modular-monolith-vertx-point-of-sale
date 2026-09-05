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

import pb.merchant.*;
import pb.merchant_document.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantProxyHandlerTest {
    @Mock VertxMerchantQueryServiceGrpcClient merchantQueryClient;
    @Mock VertxMerchantCommandServiceGrpcClient merchantCommandClient;
    @Mock VertxMerchantDocumentQueryServiceGrpcClient documentQueryClient;
    @Mock VertxMerchantDocumentCommandServiceGrpcClient documentCommandClient;
    @Mock RoutingContext ctx;
    @Mock io.vertx.core.http.HttpServerResponse response;
    @Mock io.vertx.ext.web.RequestBody body;
    private MerchantProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MerchantProxyHandler(merchantQueryClient, merchantCommandClient, documentCommandClient, documentQueryClient);
    }

    @Test
    void getAllMerchants_shouldCallGrpc() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap().add("search","test").add("page","1").add("pageSize","10"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(merchantQueryClient.findAllMerchant(any())).thenReturn(Future.succeededFuture(MerchantQuery.ApiResponsePaginationMerchant.getDefaultInstance()));
        handler.getAllMerchants(ctx);
        verify(merchantQueryClient).findAllMerchant(any(Merchant.FindAllMerchantRequest.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void getMerchantById_shouldCallGrpc() {
        when(ctx.pathParam("merchantId")).thenReturn("7");
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(merchantQueryClient.findByIdMerchant(any())).thenReturn(Future.succeededFuture(Merchant.ApiResponseMerchant.getDefaultInstance()));
        handler.getMerchantById(ctx);
        var captor = ArgumentCaptor.forClass(Merchant.FindByIdMerchantRequest.class);
        verify(merchantQueryClient).findByIdMerchant(captor.capture());
        assertThat(captor.getValue().getMerchantId()).isEqualTo(7);
        verify(response).setStatusCode(200);
    }

    @Test
    void createMerchant_shouldBuildRequestFromBody() {
        when(ctx.body()).thenReturn(body);
        when(body.asJsonObject()).thenReturn(new JsonObject().put("user_id",1).put("name","Test Merchant").put("status","active"));
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(merchantCommandClient.createMerchant(any())).thenReturn(Future.succeededFuture(Merchant.ApiResponseMerchant.getDefaultInstance()));
        handler.createMerchant(ctx);
        verify(merchantCommandClient).createMerchant(any(MerchantCommand.CreateMerchantRequest.class));
        verify(response).setStatusCode(201);
    }

    @Test
    void getAllMerchantDocuments_shouldCallDocumentQuery() {
        when(ctx.queryParams()).thenReturn(MultiMap.caseInsensitiveMultiMap());
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(documentQueryClient.findAll(any())).thenReturn(Future.succeededFuture(MerchantDocumentQuery.ApiResponsePaginationMerchantDocument.getDefaultInstance()));
        handler.getAllMerchantDocuments(ctx);
        verify(documentQueryClient).findAll(any(MerchantDocumentOuterClass.FindAllMerchantDocumentsRequest.class));
        verify(response).setStatusCode(200);
    }

    @Test
    void restoreAllMerchants_shouldCallGrpc() {
        when(ctx.response()).thenReturn(response);
        when(response.putHeader(anyString(),anyString())).thenReturn(response);
        when(response.setStatusCode(anyInt())).thenReturn(response);
        when(merchantCommandClient.restoreAllMerchant(any())).thenReturn(Future.succeededFuture(MerchantCommand.ApiResponseMerchantAll.getDefaultInstance()));
        handler.restoreAllMerchants(ctx);
        verify(merchantCommandClient).restoreAllMerchant(any(com.google.protobuf.Empty.class));
        verify(response).setStatusCode(200);
    }
}
