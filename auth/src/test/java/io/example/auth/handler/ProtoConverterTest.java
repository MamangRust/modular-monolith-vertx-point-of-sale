package io.example.auth.handler;

import io.example.auth.model.AuthUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ProtoConverterTest {

  @Test
  void toUserResponse_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 6, 1, 10, 30);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 15, 14, 0);

    AuthUser user = AuthUser.builder()
        .userId(10)
        .firstname("Charlie")
        .lastname("Brown")
        .email("charlie@peanuts.com")
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();

    pb.user.User.UserResponse response = ProtoConverter.toUserResponse(user);

    assertThat(response.getId()).isEqualTo(10);
    assertThat(response.getFirstname()).isEqualTo("Charlie");
    assertThat(response.getLastname()).isEqualTo("Brown");
    assertThat(response.getEmail()).isEqualTo("charlie@peanuts.com");
    assertThat(response.getCreatedAt()).isEqualTo("2024-06-01T10:30");
    assertThat(response.getUpdatedAt()).isEqualTo("2024-06-15T14:00");
  }

  @Test
  void toUserResponse_shouldHandleNullFields() {
    AuthUser user = AuthUser.builder()
        .userId(null)
        .firstname(null)
        .lastname(null)
        .email(null)
        .build();

    pb.user.User.UserResponse response = ProtoConverter.toUserResponse(user);

    assertThat(response.getId()).isEqualTo(0);
    assertThat(response.getFirstname()).isEqualTo("");
    assertThat(response.getLastname()).isEqualTo("");
    assertThat(response.getEmail()).isEqualTo("");
  }

  @Test
  void toUserResponse_shouldSkipNullDates() {
    AuthUser user = AuthUser.builder()
        .userId(5)
        .firstname("Diana")
        .lastname("Prince")
        .email("diana@example.com")
        .createdAt(null)
        .updatedAt(null)
        .build();

    pb.user.User.UserResponse response = ProtoConverter.toUserResponse(user);

    assertThat(response.getId()).isEqualTo(5);
    assertThat(response.getFirstname()).isEqualTo("Diana");
    assertThat(response.getCreatedAt()).isEmpty();
    assertThat(response.getUpdatedAt()).isEmpty();
  }

  @Test
  void toUserResponse_shouldSetDatesWhenPresent() {
    LocalDateTime createdAt = LocalDateTime.of(2025, 12, 25, 8, 0, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2025, 12, 26, 9, 15, 30);

    AuthUser user = AuthUser.builder()
        .userId(20)
        .firstname("Eve")
        .lastname("Adams")
        .email("eve@example.com")
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();

    pb.user.User.UserResponse response = ProtoConverter.toUserResponse(user);

    assertThat(response.getCreatedAt()).isEqualTo("2025-12-25T08:00");
    assertThat(response.getUpdatedAt()).isEqualTo("2025-12-26T09:15:30");
  }
}
