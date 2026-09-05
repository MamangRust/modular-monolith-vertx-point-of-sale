package io.example.role.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.google.protobuf.StringValue;

import io.example.role.domain.response.RoleResponse;
import io.example.role.domain.response.RoleResponseDeleteAt;
import io.example.role.model.Role;

class ProtoConverterTest {

  @Test
  void toRoleResponse_shouldMapAllFields() {
    Role role = Role.builder()
        .roleId(1)
        .roleName("ADMIN")
        .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
        .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0)))
        .build();

    pb.role.Role.RoleResponse response = ProtoConverter.toRoleResponse(role);

    assertThat(response.getId()).isEqualTo(1);
    assertThat(response.getName()).isEqualTo("ADMIN");
    assertThat(response.getCreatedAt()).isEqualTo("2024-01-01 00:00:00.0");
    assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01 00:00:00.0");
  }

  @Test
  void toRoleResponse_shouldHandleNullRole() {
    pb.role.Role.RoleResponse response = ProtoConverter.toRoleResponse(null);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(0);
    assertThat(response.getName()).isEqualTo("");
    assertThat(response.getCreatedAt()).isEqualTo("");
    assertThat(response.getUpdatedAt()).isEqualTo("");
  }

  @Test
  void toRoleResponse_shouldHandleNullFields() {
    Role role = new Role();

    pb.role.Role.RoleResponse response = ProtoConverter.toRoleResponse(role);

    assertThat(response.getId()).isEqualTo(0);
    assertThat(response.getName()).isEqualTo("");
    assertThat(response.getCreatedAt()).isEqualTo("");
    assertThat(response.getUpdatedAt()).isEqualTo("");
  }

  @Test
  void toRoleDeleteAt_shouldIncludeDeletedAt() {
    Role role = Role.builder()
        .roleId(1)
        .roleName("ADMIN")
        .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
        .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0)))
        .deletedAt(Timestamp.valueOf(LocalDateTime.of(2024, 7, 1, 0, 0)))
        .build();

    pb.role.Role.RoleResponseDeleteAt response = ProtoConverter.toRoleDeleteAt(role);

    assertThat(response.getId()).isEqualTo(1);
    assertThat(response.getName()).isEqualTo("ADMIN");
    assertThat(response.getCreatedAt()).isEqualTo("2024-01-01 00:00:00.0");
    assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01 00:00:00.0");
    assertThat(response.hasDeletedAt()).isTrue();
    assertThat(response.getDeletedAt().getValue()).isEqualTo("2024-07-01 00:00:00.0");
  }

  @Test
  void toRoleDeleteAt_shouldSkipDeletedAt() {
    Role role = Role.builder()
        .roleId(1)
        .roleName("ADMIN")
        .createdAt(Timestamp.valueOf(LocalDateTime.of(2024, 1, 1, 0, 0)))
        .updatedAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 0, 0)))
        .build();

    pb.role.Role.RoleResponseDeleteAt response = ProtoConverter.toRoleDeleteAt(role);

    assertThat(response.hasDeletedAt()).isFalse();
  }

  @Test
  void fromRoleResponse_shouldMapDomainDto() {
    RoleResponse dto = new RoleResponse(1, "ADMIN", "2024-01-01", "2024-06-01");

    pb.role.Role.RoleResponse response = ProtoConverter.fromRoleResponse(dto);

    assertThat(response.getId()).isEqualTo(1);
    assertThat(response.getName()).isEqualTo("ADMIN");
    assertThat(response.getCreatedAt()).isEqualTo("2024-01-01");
    assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01");
  }

  @Test
  void fromRoleResponseDeleteAt_shouldIncludeDeletedAt() {
    RoleResponseDeleteAt dto = new RoleResponseDeleteAt(1, "ADMIN", "2024-01-01", "2024-06-01", "2024-07-01");

    pb.role.Role.RoleResponseDeleteAt response = ProtoConverter.fromRoleResponseDeleteAt(dto);

    assertThat(response.getId()).isEqualTo(1);
    assertThat(response.getName()).isEqualTo("ADMIN");
    assertThat(response.getCreatedAt()).isEqualTo("2024-01-01");
    assertThat(response.getUpdatedAt()).isEqualTo("2024-06-01");
    assertThat(response.hasDeletedAt()).isTrue();
    assertThat(response.getDeletedAt().getValue()).isEqualTo("2024-07-01");
  }

  @Test
  void nullSafety_forAllMethods() {
    assertThat(ProtoConverter.toRoleResponse(null)).isNotNull();
    assertThat(ProtoConverter.toRoleDeleteAt(null)).isNotNull();
    assertThat(ProtoConverter.fromRoleResponse((RoleResponse) null)).isNotNull();
    assertThat(ProtoConverter.fromRoleResponseDeleteAt((RoleResponseDeleteAt) null)).isNotNull();
  }
}
