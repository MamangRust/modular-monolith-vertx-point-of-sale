package io.example.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DbSeederTest {

  @Test
  void seederEnabledForTruthyValues() {
    assertThat(DbSeeder.isSeederEnabled("true")).isTrue();
    assertThat(DbSeeder.isSeederEnabled("TRUE")).isTrue();
    assertThat(DbSeeder.isSeederEnabled("1")).isTrue();
    assertThat(DbSeeder.isSeederEnabled("yes")).isTrue();
    assertThat(DbSeeder.isSeederEnabled("Yes")).isTrue();
  }

  @Test
  void seederDisabledForUnsetOrFalsyValues() {
    assertThat(DbSeeder.isSeederEnabled((String) null)).isFalse();
    assertThat(DbSeeder.isSeederEnabled("")).isFalse();
    assertThat(DbSeeder.isSeederEnabled("false")).isFalse();
    assertThat(DbSeeder.isSeederEnabled("0")).isFalse();
    assertThat(DbSeeder.isSeederEnabled("random")).isFalse();
  }

  @Test
  void defaultRolesContainAdminFirst() {
    // RegisterService.new users default to ROLE_ADMIN — seeder must provide it.
    assertThat(DbSeeder.DEFAULT_ROLES).containsExactly("ROLE_ADMIN", "ROLE_CASHIER", "ROLE_MERCHANT");
  }
}
