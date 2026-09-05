package io.example.user.handler;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.protobuf.StringValue;

import io.example.user.model.UserResponse;
import io.example.user.model.UserResponseDeleteAt;

class ProtoConverterTest {

  @Test
  void toUserResponse_shouldMapAllFields() {
    UserResponse resp = new UserResponse(1, "John", "Doe", "john@test.com", "2024-01-01", "2024-06-01");

    pb.user.User.UserResponse response = ProtoConverter.toUserResponse(resp);

    assertThat(response.getId()).isEqualTo(1);
    assertThat(response.getFirstname()).isEqualTo("John");
    assertThat(response.getLastname()).isEqualTo("Doe");
    assertThat(response.getEmail()).isEqualTo("john@test.com");
    assertThat(response.getCreatedAt()).isEqualTo("2024-01-01");
    assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01");
  }

  @Test
  void toUserResponse_shouldHandleNullInput() {
    pb.user.User.UserResponse response = ProtoConverter.toUserResponse(null);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(0);
    assertThat(response.getFirstname()).isEqualTo("");
    assertThat(response.getLastname()).isEqualTo("");
    assertThat(response.getEmail()).isEqualTo("");
    assertThat(response.getCreatedAt()).isEqualTo("");
    assertThat(response.getUpdatedAt()).isEqualTo("");
  }

  @Test
  void toUserResponse_shouldHandleNullFields() {
    UserResponse resp = new UserResponse();

    pb.user.User.UserResponse response = ProtoConverter.toUserResponse(resp);

    assertThat(response.getId()).isEqualTo(0);
    assertThat(response.getFirstname()).isEqualTo("");
    assertThat(response.getLastname()).isEqualTo("");
    assertThat(response.getEmail()).isEqualTo("");
    assertThat(response.getCreatedAt()).isEqualTo("");
    assertThat(response.getUpdatedAt()).isEqualTo("");
  }

  @Test
  void toUserDeleteAt_shouldIncludeDeletedAt() {
    UserResponseDeleteAt delResp = new UserResponseDeleteAt(1, "John", "Doe", "john@test.com", "2024-01-01", "2024-06-01", "2024-07-01");

    pb.user.User.UserResponseDeleteAt response = ProtoConverter.toUserDeleteAt(delResp);

    assertThat(response.getId()).isEqualTo(1);
    assertThat(response.getFirstname()).isEqualTo("John");
    assertThat(response.getLastname()).isEqualTo("Doe");
    assertThat(response.getEmail()).isEqualTo("john@test.com");
    assertThat(response.getCreatedAt()).isEqualTo("2024-01-01");
    assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01");
    assertThat(response.hasDeletedAt()).isTrue();
    assertThat(response.getDeletedAt().getValue()).isEqualTo("2024-07-01");
  }

  @Test
  void toUserDeleteAt_shouldSkipEmptyDeletedAt() {
    UserResponseDeleteAt delResp = new UserResponseDeleteAt();

    pb.user.User.UserResponseDeleteAt response = ProtoConverter.toUserDeleteAt(delResp);

    assertThat(response.hasDeletedAt()).isFalse();
  }

  @Test
  void toUserDeleteAt_shouldHandleNullInput() {
    pb.user.User.UserResponseDeleteAt response = ProtoConverter.toUserDeleteAt(null);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(0);
    assertThat(response.getFirstname()).isEqualTo("");
    assertThat(response.getLastname()).isEqualTo("");
    assertThat(response.getEmail()).isEqualTo("");
    assertThat(response.getCreatedAt()).isEqualTo("");
    assertThat(response.getUpdatedAt()).isEqualTo("");
    assertThat(response.hasDeletedAt()).isFalse();
  }
}
