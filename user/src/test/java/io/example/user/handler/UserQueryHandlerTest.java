package io.example.user.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.example.common.domain.PagedResult;
import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.example.user.service.UserQueryService;
import io.vertx.core.Future;
import pb.user.User.ApiResponseUser;
import pb.user.User.FindAllUserRequest;
import pb.user.User.FindByIdUserRequest;
import pb.user.UserQuery.ApiResponsePaginationUser;
import pb.user.UserQuery.ApiResponsePaginationUserDeleteAt;

@ExtendWith(MockitoExtension.class)
class UserQueryHandlerTest {

    @Mock
    private UserQueryService queryService;

    private UserQueryHandler queryHandler;

    @BeforeEach
    void setUp() {
        queryHandler = new UserQueryHandler(queryService);
    }

    @Test
    void findAll_shouldReturnPagedResponse() {
        FindAllUserRequest request = FindAllUserRequest.newBuilder()
                .setSearch("")
                .setPage(1)
                .setPageSize(10)
                .build();

        UserResponse resp = new UserResponse(1, "John", "Doe", "john@test.com", "2024-01-01", "2024-06-01");
        PagedResult<UserResponse> paged = new PagedResult<>(List.of(resp), 1);

        doReturn(Future.succeededFuture(paged)).when(queryService).getUsers(any(io.example.user.domain.requests.FindAllUsers.class));

        Future<ApiResponsePaginationUser> result = queryHandler.findAll(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("OK");
        assertThat(result.result().getDataList()).hasSize(1);
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
        assertThat(result.result().getData(0).getFirstname()).isEqualTo("John");
        assertThat(result.result().getData(0).getLastname()).isEqualTo("Doe");
        assertThat(result.result().getPagination().getCurrentPage()).isEqualTo(1);
        assertThat(result.result().getPagination().getPageSize()).isEqualTo(10);
        assertThat(result.result().getPagination().getTotalPages()).isEqualTo(1);
        assertThat(result.result().getPagination().getTotalRecords()).isEqualTo(1);
        verify(queryService).getUsers(any());
    }

    @Test
    void findById_shouldReturnUserResponse() {
        FindByIdUserRequest request = FindByIdUserRequest.newBuilder()
                .setId(1)
                .build();

        UserResponse resp = new UserResponse(1, "John", "Doe", "john@test.com", "2024-01-01", "2024-06-01");

        doReturn(Future.succeededFuture(resp)).when(queryService).getUserById(anyInt());

        Future<ApiResponseUser> result = queryHandler.findById(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("OK");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getFirstname()).isEqualTo("John");
        assertThat(result.result().getData().getLastname()).isEqualTo("Doe");
        assertThat(result.result().getData().getEmail()).isEqualTo("john@test.com");
        verify(queryService).getUserById(1);
    }

    @Test
    void findByActive_shouldReturnPagedResponse() {
        FindAllUserRequest request = FindAllUserRequest.newBuilder()
                .setSearch("")
                .setPage(1)
                .setPageSize(10)
                .build();

        UserResponseDeleteAt delResp = new UserResponseDeleteAt(1, "John", "Doe", "john@test.com", "2024-01-01", "2024-06-01", null);
        PagedResult<UserResponseDeleteAt> pagedDel = new PagedResult<>(List.of(delResp), 1);

        doReturn(Future.succeededFuture(pagedDel)).when(queryService).getActiveUsers(any(io.example.user.domain.requests.FindAllUsers.class));

        Future<ApiResponsePaginationUserDeleteAt> result = queryHandler.findByActive(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("OK");
        assertThat(result.result().getDataList()).hasSize(1);
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
        assertThat(result.result().getData(0).getFirstname()).isEqualTo("John");
        assertThat(result.result().getPagination().getCurrentPage()).isEqualTo(1);
        assertThat(result.result().getPagination().getPageSize()).isEqualTo(10);
        assertThat(result.result().getPagination().getTotalPages()).isEqualTo(1);
        assertThat(result.result().getPagination().getTotalRecords()).isEqualTo(1);
        verify(queryService).getActiveUsers(any());
    }

    @Test
    void findByTrashed_shouldReturnPagedResponse() {
        FindAllUserRequest request = FindAllUserRequest.newBuilder()
                .setSearch("")
                .setPage(1)
                .setPageSize(10)
                .build();

        UserResponseDeleteAt delResp = new UserResponseDeleteAt(1, "John", "Doe", "john@test.com", "2024-01-01", "2024-06-01", null);
        PagedResult<UserResponseDeleteAt> pagedDel = new PagedResult<>(List.of(delResp), 1);

        doReturn(Future.succeededFuture(pagedDel)).when(queryService).getTrashedUsers(any(io.example.user.domain.requests.FindAllUsers.class));

        Future<ApiResponsePaginationUserDeleteAt> result = queryHandler.findByTrashed(request);

        assertThat(result.succeeded()).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("OK");
        assertThat(result.result().getDataList()).hasSize(1);
        assertThat(result.result().getData(0).getId()).isEqualTo(1);
        assertThat(result.result().getData(0).getFirstname()).isEqualTo("John");
        assertThat(result.result().getPagination().getCurrentPage()).isEqualTo(1);
        assertThat(result.result().getPagination().getPageSize()).isEqualTo(10);
        assertThat(result.result().getPagination().getTotalPages()).isEqualTo(1);
        assertThat(result.result().getPagination().getTotalRecords()).isEqualTo(1);
        verify(queryService).getTrashedUsers(any());
    }
}
