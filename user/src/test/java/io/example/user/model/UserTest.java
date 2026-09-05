package io.example.user.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;

@ExtendWith(MockitoExtension.class)
class UserTest {

  @Mock
  Row row;

  @Test
  void fromRow_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 0, 0);
    LocalDateTime deletedAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(row.getInteger("user_id")).thenReturn(1);
    when(row.getString("firstname")).thenReturn("John");
    when(row.getString("lastname")).thenReturn("Doe");
    when(row.getString("email")).thenReturn("john@test.com");
    when(row.getString("password")).thenReturn("secret");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(createdAt);
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(updatedAt);
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(deletedAt);

    User user = User.fromRow(row);

    assertThat(user).isNotNull();
    assertThat(user.getUserId()).isEqualTo(1);
    assertThat(user.getFirstname()).isEqualTo("John");
    assertThat(user.getLastname()).isEqualTo("Doe");
    assertThat(user.getEmail()).isEqualTo("john@test.com");
    assertThat(user.getPassword()).isEqualTo("secret");
    assertThat(user.getCreatedAt()).isEqualTo(Timestamp.valueOf(createdAt));
    assertThat(user.getUpdatedAt()).isEqualTo(Timestamp.valueOf(updatedAt));
    assertThat(user.getDeletedAt()).isEqualTo(Timestamp.valueOf(deletedAt));
  }

  @Test
  void fromRow_shouldReturnNullForNullRow() {
    assertThat(User.fromRow(null)).isNull();
  }

  @Test
  void fromJson_shouldDeserialize() {
    JsonObject json = new JsonObject()
        .put("userId", 1)
        .put("firstname", "John")
        .put("lastname", "Doe")
        .put("email", "john@test.com")
        .put("createdAt", "2024-01-01T00:00:00Z")
        .put("updatedAt", "2024-06-01T00:00:00Z");

    User user = User.fromJson(json);

    assertThat(user).isNotNull();
    assertThat(user.getUserId()).isEqualTo(1);
    assertThat(user.getFirstname()).isEqualTo("John");
    assertThat(user.getLastname()).isEqualTo("Doe");
    assertThat(user.getEmail()).isEqualTo("john@test.com");
    assertThat(user.getCreatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-01-01T00:00:00Z")));
    assertThat(user.getUpdatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-06-01T00:00:00Z")));
    assertThat(user.getDeletedAt()).isNull();
  }

  @Test
  void fromJson_shouldReturnNullForNullJson() {
    assertThat(User.fromJson(null)).isNull();
  }

  @Test
  void toJson_shouldSerializeAllFields() {
    Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
    Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));

    User user = User.builder()
        .userId(1)
        .firstname("John")
        .lastname("Doe")
        .email("john@test.com")
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();

    JsonObject json = user.toJson();

    assertThat(json.getInteger("userId")).isEqualTo(1);
    assertThat(json.getString("firstname")).isEqualTo("John");
    assertThat(json.getString("lastname")).isEqualTo("Doe");
    assertThat(json.getString("email")).isEqualTo("john@test.com");
    assertThat(json.getString("createdAt")).isEqualTo(createdAt.toString());
    assertThat(json.getString("updatedAt")).isEqualTo(updatedAt.toString());
  }

  @Test
  void toJson_shouldSkipNullDates() {
    User user = User.builder()
        .userId(2)
        .firstname("Jane")
        .lastname("Smith")
        .email("jane@test.com")
        .build();

    JsonObject json = user.toJson();

    assertThat(json.getInteger("userId")).isEqualTo(2);
    assertThat(json.getString("firstname")).isEqualTo("Jane");
    assertThat(json.getString("lastname")).isEqualTo("Smith");
    assertThat(json.getString("email")).isEqualTo("jane@test.com");
    assertThat(json.containsKey("createdAt")).isFalse();
    assertThat(json.containsKey("updatedAt")).isFalse();
    assertThat(json.containsKey("deletedAt")).isFalse();
  }

  @Test
  void builder_shouldCreateUser() {
    Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
    Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));
    Timestamp deletedAt = Timestamp.valueOf(LocalDateTime.of(2024, 7, 1, 0, 0));

    User user = User.builder()
        .userId(1)
        .firstname("John")
        .lastname("Doe")
        .email("john@test.com")
        .password("secret")
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();

    assertThat(user.getUserId()).isEqualTo(1);
    assertThat(user.getFirstname()).isEqualTo("John");
    assertThat(user.getLastname()).isEqualTo("Doe");
    assertThat(user.getEmail()).isEqualTo("john@test.com");
    assertThat(user.getPassword()).isEqualTo("secret");
    assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(user.getDeletedAt()).isEqualTo(deletedAt);
  }
}
