package io.example.role.model;

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
class RoleTest {

  @Mock
  Row row;

  @Test
  void fromRow_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 0, 0);
    LocalDateTime deletedAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(row.getInteger("role_id")).thenReturn(1);
    when(row.getString("role_name")).thenReturn("ADMIN");
    when(row.get(LocalDateTime.class, "created_at")).thenReturn(createdAt);
    when(row.get(LocalDateTime.class, "updated_at")).thenReturn(updatedAt);
    when(row.get(LocalDateTime.class, "deleted_at")).thenReturn(deletedAt);

    Role role = Role.fromRow(row);

    assertThat(role).isNotNull();
    assertThat(role.getRoleId()).isEqualTo(1);
    assertThat(role.getRoleName()).isEqualTo("ADMIN");
    assertThat(role.getCreatedAt()).isEqualTo(Timestamp.valueOf(createdAt));
    assertThat(role.getUpdatedAt()).isEqualTo(Timestamp.valueOf(updatedAt));
    assertThat(role.getDeletedAt()).isEqualTo(Timestamp.valueOf(deletedAt));
  }

  @Test
  void fromRow_shouldReturnNullForNullRow() {
    assertThat(Role.fromRow(null)).isNull();
  }

  @Test
  void fromJson_shouldDeserialize() {
    JsonObject json = new JsonObject()
        .put("roleId", 1)
        .put("roleName", "ADMIN")
        .put("createdAt", "2024-01-01T00:00:00Z")
        .put("updatedAt", "2024-06-01T00:00:00Z");

    Role role = Role.fromJson(json);

    assertThat(role).isNotNull();
    assertThat(role.getRoleId()).isEqualTo(1);
    assertThat(role.getRoleName()).isEqualTo("ADMIN");
    assertThat(role.getCreatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-01-01T00:00:00Z")));
    assertThat(role.getUpdatedAt()).isEqualTo(Timestamp.from(java.time.Instant.parse("2024-06-01T00:00:00Z")));
    assertThat(role.getDeletedAt()).isNull();
  }

  @Test
  void fromJson_shouldReturnNullForNullJson() {
    assertThat(Role.fromJson(null)).isNull();
  }

  @Test
  void toJson_shouldSerializeAllFields() {
    Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
    Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));

    Role role = Role.builder()
        .roleId(1)
        .roleName("ADMIN")
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();

    JsonObject json = role.toJson();

    assertThat(json.getInteger("roleId")).isEqualTo(1);
    assertThat(json.getString("roleName")).isEqualTo("ADMIN");
    assertThat(json.getString("createdAt")).isEqualTo(createdAt.toString());
    assertThat(json.getString("updatedAt")).isEqualTo(updatedAt.toString());
  }

  @Test
  void toJson_shouldSkipNullDates() {
    Role role = Role.builder()
        .roleId(2)
        .roleName("USER")
        .build();

    JsonObject json = role.toJson();

    assertThat(json.getInteger("roleId")).isEqualTo(2);
    assertThat(json.getString("roleName")).isEqualTo("USER");
    assertThat(json.containsKey("createdAt")).isFalse();
    assertThat(json.containsKey("updatedAt")).isFalse();
    assertThat(json.containsKey("deletedAt")).isFalse();
  }

  @Test
  void builder_shouldCreateRole() {
    Timestamp createdAt = Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0));
    Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0));
    Timestamp deletedAt = Timestamp.valueOf(LocalDateTime.of(2024, 7, 1, 0, 0));

    Role role = Role.builder()
        .roleId(1)
        .roleName("ADMIN")
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(deletedAt)
        .build();

    assertThat(role.getRoleId()).isEqualTo(1);
    assertThat(role.getRoleName()).isEqualTo("ADMIN");
    assertThat(role.getCreatedAt()).isEqualTo(createdAt);
    assertThat(role.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(role.getDeletedAt()).isEqualTo(deletedAt);
  }
}
