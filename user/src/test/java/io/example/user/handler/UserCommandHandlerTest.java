package io.example.user.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.protobuf.Empty;

import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;
import io.example.user.service.UserCommandService;
import io.vertx.core.Future;
import pb.user.User.ApiResponseUser;
import pb.user.User.ApiResponseUserDeleteAt;
import pb.user.User.FindByIdUserRequest;
import pb.user.UserCommand.ApiResponseUserAll;
import pb.user.UserCommand.ApiResponseUserDelete;
import pb.user.UserCommand.CreateUserRequest;
import pb.user.UserCommand.UpdateUserRequest;

@ExtendWith(MockitoExtension.class)
class UserCommandHandlerTest {

    @Mock
    private UserCommandService commandService;

    private UserCommandHandler commandHandler;

    @BeforeEach
    void setUp() {
        commandHandler = new UserCommandHandler(commandService);
    }

    @Test
    void createUser_shouldReturnApiResponse() {
        CreateUserRequest request = CreateUserRequest.newBuilder()
                .setFirstname("John")
                .setLastname("Doe")
                .setEmail("john@test.com")
                .setPassword("pass")
                .setConfirmPassword("pass")
                .build();

        UserResponse resp = new UserResponse(1, "John", "Doe", "john@test.com", "2024-01-01", "2024-06-01");
        Future<UserResponse> futureResp = Future.succeededFuture(resp);
        doReturn(Future.succeededFuture(resp)).when(commandService).createUser(any(io.example.user.domain.requests.CreateUserRequest.class));

        Future<ApiResponseUser> result = commandHandler.create(request);

        assertThat(result.succeeded()).as("Fail: " + (result.failed() ? result.cause() : "")).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("OK");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getFirstname()).isEqualTo("John");
        assertThat(result.result().getData().getLastname()).isEqualTo("Doe");
        assertThat(result.result().getData().getEmail()).isEqualTo("john@test.com");
        verify(commandService).createUser(any());
    }

    @Test
    void updateUser_shouldReturnApiResponse() {
        UpdateUserRequest request = UpdateUserRequest.newBuilder()
                .setId(1)
                .setFirstname("Jane")
                .build();

        UserResponse resp = new UserResponse(1, "Jane", "Doe", "jane@test.com", "2024-01-01", "2024-06-01");
        Future<UserResponse> futureResp = Future.succeededFuture(resp);
        doReturn(Future.succeededFuture(resp)).when(commandService).updateUser(any(io.example.user.domain.requests.UpdateUserRequest.class));

        Future<ApiResponseUser> result = commandHandler.update(request);

        assertThat(result.succeeded()).as("Fail: " + (result.failed() ? result.cause() : "")).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("OK");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getFirstname()).isEqualTo("Jane");
        verify(commandService).updateUser(any());
    }

    @Test
    void trashedUser_shouldReturnApiResponse() {
        FindByIdUserRequest request = FindByIdUserRequest.newBuilder()
                .setId(1)
                .build();

        UserResponseDeleteAt delResp = new UserResponseDeleteAt(1, "John", "Doe", "john@test.com", "2024-01-01", "2024-06-01", null);
        Future<UserResponseDeleteAt> futureDelResp = Future.succeededFuture(delResp);
        doReturn(Future.succeededFuture(delResp)).when(commandService).trashUser(anyInt());

        Future<ApiResponseUserDeleteAt> result = commandHandler.trashedUser(request);

        assertThat(result.succeeded()).as("Fail: " + (result.failed() ? result.cause() : "")).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("OK");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getFirstname()).isEqualTo("John");
        verify(commandService).trashUser(1);
    }

    @Test
    void restoreUser_shouldReturnApiResponse() {
        FindByIdUserRequest request = FindByIdUserRequest.newBuilder()
                .setId(1)
                .build();

        UserResponseDeleteAt delResp = new UserResponseDeleteAt(1, "John", "Doe", "john@test.com", "2024-01-01", "2024-06-01", null);
        Future<UserResponseDeleteAt> futureDelResp = Future.succeededFuture(delResp);
        doReturn(Future.succeededFuture(delResp)).when(commandService).restoreUser(anyInt());

        Future<ApiResponseUserDeleteAt> result = commandHandler.restoreUser(request);

        assertThat(result.succeeded()).as("Fail: " + (result.failed() ? result.cause() : "")).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("OK");
        assertThat(result.result().getData().getId()).isEqualTo(1);
        assertThat(result.result().getData().getFirstname()).isEqualTo("John");
        verify(commandService).restoreUser(1);
    }

    @Test
    void deleteUserPermanent_shouldReturnApiResponse() {
        FindByIdUserRequest request = FindByIdUserRequest.newBuilder()
                .setId(1)
                .build();

        when(commandService.deletePermanent(anyInt())).thenReturn(Future.succeededFuture());

        Future<ApiResponseUserDelete> result = commandHandler.deleteUserPermanent(request);

        assertThat(result.succeeded()).as("Fail: " + (result.failed() ? result.cause() : "")).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("User deleted permanently");
        verify(commandService).deletePermanent(1);
    }

    @Test
    void restoreAllUser_shouldReturnApiResponse() {
        when(commandService.restoreAllUsers()).thenReturn(Future.succeededFuture());

        Future<ApiResponseUserAll> result = commandHandler.restoreAllUser(Empty.getDefaultInstance());

        assertThat(result.succeeded()).as("Fail: " + (result.failed() ? result.cause() : "")).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("All users restored successfully");
        verify(commandService).restoreAllUsers();
    }

    @Test
    void deleteAllUserPermanent_shouldReturnApiResponse() {
        when(commandService.deleteAllPermanentUsers()).thenReturn(Future.succeededFuture());

        Future<ApiResponseUserAll> result = commandHandler.deleteAllUserPermanent(Empty.getDefaultInstance());

        assertThat(result.succeeded()).as("Fail: " + (result.failed() ? result.cause() : "")).isTrue();
        assertThat(result.result().getStatus()).isEqualTo("success");
        assertThat(result.result().getMessage()).isEqualTo("All users permanently deleted");
        verify(commandService).deleteAllPermanentUsers();
    }
}
