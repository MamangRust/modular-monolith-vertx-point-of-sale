package io.example.role.domain.requests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.example.role.domain.requests.role.CreateRoleRequest;
import io.example.role.domain.requests.role.FindAllRoles;
import io.example.role.domain.requests.role.UpdateRoleRequest;

class RoleRequestTest {

  @Test
  void createRoleRequest_shouldBuild() {
    CreateRoleRequest request = CreateRoleRequest.builder()
        .name("ADMIN")
        .build();

    assertThat(request.getName()).isEqualTo("ADMIN");
  }

  @Test
  void updateRoleRequest_shouldBuild() {
    UpdateRoleRequest request = UpdateRoleRequest.builder()
        .roleId(1)
        .name("USER")
        .build();

    assertThat(request.getRoleId()).isEqualTo(1);
    assertThat(request.getName()).isEqualTo("USER");
  }

  @Test
  void findAllRoles_shouldUseDefaults() {
    FindAllRoles request = new FindAllRoles();

    assertThat(request.getPage()).isEqualTo(1);
    assertThat(request.getPageSize()).isEqualTo(10);
    assertThat(request.getSearch()).isEqualTo("");
  }

  @Test
  void findAllRoles_shouldSetFields() {
    FindAllRoles request = new FindAllRoles();
    request.setPage(2);
    request.setPageSize(20);
    request.setSearch("ADMIN");

    assertThat(request.getPage()).isEqualTo(2);
    assertThat(request.getPageSize()).isEqualTo(20);
    assertThat(request.getSearch()).isEqualTo("ADMIN");
  }
}
