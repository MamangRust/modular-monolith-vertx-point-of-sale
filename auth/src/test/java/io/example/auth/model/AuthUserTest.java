package io.example.auth.model;

import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.RowSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserTest {

  @Mock Row row;

  @Test
  void fromRow_shouldMapAllFields() {
    LocalDateTime createdAt = LocalDateTime.of(2024, 6, 1, 10, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 15, 14, 30);
    LocalDateTime deletedAt = LocalDateTime.of(2024, 7, 1, 0, 0);

    when(row.getInteger("user_id")).thenReturn(42);
    when(row.getString("firstname")).thenReturn("Alice");
    when(row.getString("lastname")).thenReturn("Wonderland");
    when(row.getString("email")).thenReturn("alice@example.com");
    when(row.getString("password")).thenReturn("hashed-pw");
    when(row.getLocalDateTime("created_at")).thenReturn(createdAt);
    when(row.getLocalDateTime("updated_at")).thenReturn(updatedAt);
    when(row.getLocalDateTime("deleted_at")).thenReturn(deletedAt);

    AuthUser user = AuthUser.fromRow(row);

    assertThat(user).isNotNull();
    assertThat(user.getUserId()).isEqualTo(42);
    assertThat(user.getFirstname()).isEqualTo("Alice");
    assertThat(user.getLastname()).isEqualTo("Wonderland");
    assertThat(user.getEmail()).isEqualTo("alice@example.com");
    assertThat(user.getPassword()).isEqualTo("hashed-pw");
    assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(user.getDeletedAt()).isEqualTo(deletedAt);
  }

  @Test
  void fromRow_shouldReturnNullForNullRow() {
    assertThat(AuthUser.fromRow(null)).isNull();
  }

  @Test
  void fromRowsWithRoles_shouldBuildUserWithRoles() {
    Row row1 = mock(Row.class);
    Row row2 = mock(Row.class);

    when(row1.getInteger("user_id")).thenReturn(1);
    when(row1.getString("firstname")).thenReturn("John");
    when(row1.getString("lastname")).thenReturn("Doe");
    when(row1.getString("email")).thenReturn("john@example.com");
    when(row1.getString("password")).thenReturn("secret");
    when(row1.getLocalDateTime("created_at")).thenReturn(LocalDateTime.of(2024, 1, 1, 0, 0));
    when(row1.getLocalDateTime("updated_at")).thenReturn(LocalDateTime.of(2024, 1, 2, 0, 0));
    when(row1.getString("role_name")).thenReturn("ADMIN");

    when(row2.getString("role_name")).thenReturn("USER");

    RowSet<Row> rows = mock(RowSet.class);
    RowIterator<Row> checkIterator = mock(RowIterator.class);
    when(checkIterator.hasNext()).thenReturn(true);

    RowIterator<Row> baseUserIterator = mock(RowIterator.class);
    when(baseUserIterator.next()).thenReturn(row1);

    RowIterator<Row> forEachIterator = mock(RowIterator.class);
    when(forEachIterator.hasNext()).thenReturn(true, true, false);
    when(forEachIterator.next()).thenReturn(row1, row2);

    when(rows.iterator()).thenReturn(checkIterator, baseUserIterator, forEachIterator);

    AuthUser result = AuthUser.fromRowsWithRoles(rows);

    assertThat(result).isNotNull();
    assertThat(result.getUserId()).isEqualTo(1);
    assertThat(result.getFirstname()).isEqualTo("John");
    assertThat(result.getEmail()).isEqualTo("john@example.com");
    assertThat(result.getRoles()).containsExactly("ADMIN", "USER");
  }

  @Test
  void fromRowsWithRoles_shouldReturnNullForNullRows() {
    assertThat(AuthUser.fromRowsWithRoles(null)).isNull();
  }

  @Test
  void fromRowsWithRoles_shouldReturnNullForEmptyRows() {
    RowSet<Row> rows = mock(RowSet.class);
    RowIterator<Row> emptyIterator = mock(RowIterator.class);
    when(emptyIterator.hasNext()).thenReturn(false);
    when(rows.iterator()).thenReturn(emptyIterator);

    AuthUser result = AuthUser.fromRowsWithRoles(rows);

    assertThat(result).isNull();
  }

  @Test
  void builder_shouldCreateUser() {
    LocalDateTime createdAt = LocalDateTime.of(2025, 3, 15, 9, 0);
    LocalDateTime updatedAt = LocalDateTime.of(2025, 3, 16, 12, 0);

    AuthUser user = AuthUser.builder()
        .userId(99)
        .firstname("Bob")
        .lastname("Builder")
        .email("bob@build.com")
        .password("secure123")
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .deletedAt(null)
        .build();

    assertThat(user.getUserId()).isEqualTo(99);
    assertThat(user.getFirstname()).isEqualTo("Bob");
    assertThat(user.getLastname()).isEqualTo("Builder");
    assertThat(user.getEmail()).isEqualTo("bob@build.com");
    assertThat(user.getPassword()).isEqualTo("secure123");
    assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
    assertThat(user.getDeletedAt()).isNull();
  }

  @Test
  void roles_shouldDefaultToEmptyList() {
    AuthUser user = new AuthUser();
    assertThat(user.getRoles()).isNotNull();
    assertThat(user.getRoles()).isEmpty();
  }
}
