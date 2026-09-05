package io.example.user.domain.requests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserRequestTest {

  @Test
  void createUserRequest_shouldBuild() {
    CreateUserRequest request = CreateUserRequest.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john@test.com")
        .password("secret")
        .confirmPassword("secret")
        .build();

    assertThat(request.getFirstName()).isEqualTo("John");
    assertThat(request.getLastName()).isEqualTo("Doe");
    assertThat(request.getEmail()).isEqualTo("john@test.com");
    assertThat(request.getPassword()).isEqualTo("secret");
    assertThat(request.getConfirmPassword()).isEqualTo("secret");
  }

  @Test
  void findAllUsers_shouldUseDefaults() {
    FindAllUsers request = new FindAllUsers();

    assertThat(request.getPage()).isEqualTo(1);
    assertThat(request.getPageSize()).isEqualTo(10);
    assertThat(request.getSearch()).isEqualTo("");
  }

  @Test
  void findAllUsers_shouldSetFields() {
    FindAllUsers request = new FindAllUsers();
    request.setPage(2);
    request.setPageSize(20);
    request.setSearch("john");

    assertThat(request.getPage()).isEqualTo(2);
    assertThat(request.getPageSize()).isEqualTo(20);
    assertThat(request.getSearch()).isEqualTo("john");
  }

  @Test
  void updateUserRequest_shouldBuild() {
    UpdateUserRequest request = UpdateUserRequest.builder()
        .userId(1)
        .firstName("John")
        .lastName("Doe")
        .email("john@test.com")
        .password("newsecret")
        .confirmPassword("newsecret")
        .build();

    assertThat(request.getUserId()).isEqualTo(1);
    assertThat(request.getFirstName()).isEqualTo("John");
    assertThat(request.getLastName()).isEqualTo("Doe");
    assertThat(request.getEmail()).isEqualTo("john@test.com");
    assertThat(request.getPassword()).isEqualTo("newsecret");
    assertThat(request.getConfirmPassword()).isEqualTo("newsecret");
  }
}
