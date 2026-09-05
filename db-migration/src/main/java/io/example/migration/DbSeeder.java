package io.example.migration;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Idempotent bootstrap seeder — runs after Flyway migration when {@code DB_SEEDER}
 * is enabled (true/1/yes). Seeds the default roles required by the auth service
 * (RegisterService defaults new users to {@code ROLE_ADMIN}) plus a pre-verified
 * admin user so register→login works from a fresh database without an SMTP/OTP round-trip.
 *
 * <p>All statements use {@code ON CONFLICT DO NOTHING} so re-running on an already
 * seeded database is a no-op.
 */
public class DbSeeder {
  private static final Logger log = LoggerFactory.getLogger(DbSeeder.class);

  /** Roles seeded by default. Keep ROLE_ADMIN first — RegisterService depends on it. */
  static final String[] DEFAULT_ROLES = {"ROLE_ADMIN", "ROLE_CASHIER", "ROLE_MERCHANT"};

  static final String DEFAULT_ADMIN_FIRSTNAME = "Admin";
  static final String DEFAULT_ADMIN_LASTNAME = "System";
  static final String DEFAULT_ADMIN_EMAIL = "admin@example.com";
  static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

  /**
   * Whether the seeder should run. Accepts {@code DB_SEEDER=true|1|yes} (case-insensitive).
   */
  public static boolean isSeederEnabled() {
    return isSeederEnabled(System.getenv("DB_SEEDER"));
  }

  /** Testable overload — package-private so unit tests can cover parsing without env mutation. */
  static boolean isSeederEnabled(String raw) {
    if (raw == null) {
      return false;
    }
    return raw.equalsIgnoreCase("true") || raw.equals("1") || raw.equalsIgnoreCase("yes");
  }

  /**
   * Seeds default roles and the admin user using the same JDBC URL/credentials as Flyway.
   * Failure throws {@link IllegalStateException} so the migration container exits non-zero.
   */
  public static void seed(String jdbcUrl, String username, String password) {
    String adminFirst = envOr("SEED_ADMIN_FIRSTNAME", DEFAULT_ADMIN_FIRSTNAME);
    String adminLast = envOr("SEED_ADMIN_LASTNAME", DEFAULT_ADMIN_LASTNAME);
    String adminEmail = envOr("SEED_ADMIN_EMAIL", DEFAULT_ADMIN_EMAIL);
    String adminPassword = envOr("SEED_ADMIN_PASSWORD", DEFAULT_ADMIN_PASSWORD);

    // Same hashing as auth's RegisterService (BCrypt, cost 12) so login verifies.
    String passwordHash = BCrypt.withDefaults().hashToString(12, adminPassword.toCharArray());

    // Catatan: sengaja TIDAK memakai ON CONFLICT ... DO NOTHING. Volume DB legacy
    // (mis. hasil E2E lama) bisa saja tidak memiliki constraint UNIQUE pada kolom
    // role_name/email — ON CONFLICT butuh constraint yang cocok dan akan gagal
    // dengan "no unique or exclusion constraint matching". Pola INSERT..SELECT
    // + NOT EXISTS di bawah ini idempotent & portabel di skema apa pun.
    try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
      for (String role : DEFAULT_ROLES) {
        try (PreparedStatement ps = conn.prepareStatement("""
            INSERT INTO roles (role_name)
            SELECT ? WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = ?)""")) {
          ps.setString(1, role);
          ps.setString(2, role);
          ps.executeUpdate();
        }
      }

      try (PreparedStatement ps = conn.prepareStatement("""
          INSERT INTO users (firstname, lastname, email, password, is_verified)
          SELECT ?, ?, ?, ?, TRUE
          WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = ?)""")) {
        ps.setString(1, adminFirst);
        ps.setString(2, adminLast);
        ps.setString(3, adminEmail);
        ps.setString(4, passwordHash);
        ps.setString(5, adminEmail);
        ps.executeUpdate();
      }

      // Jika email admin sudah pernah terdaftar tapi belum di-verify, pastikan tetap
      // bisa login (seeder menjamin akses admin bootstrap pada DB apa pun).
      try (PreparedStatement ps = conn.prepareStatement("""
          UPDATE users SET is_verified = TRUE
          WHERE email = ? AND deleted_at IS NULL""")) {
        ps.setString(1, adminEmail);
        ps.executeUpdate();
      }

      // Link admin → ROLE_ADMIN hanya jika belum ada pasangan aktif (LIMIT 1 agar
      // aman walau legacy DB punya duplikat baris ROLE_ADMIN).
      try (PreparedStatement ps = conn.prepareStatement("""
          INSERT INTO user_roles (user_id, role_id)
          SELECT u.user_id, r.role_id
          FROM users u
          CROSS JOIN roles r
          WHERE u.email = ? AND r.role_name = ?
            AND NOT EXISTS (
              SELECT 1 FROM user_roles ur
              WHERE ur.user_id = u.user_id AND ur.role_id = r.role_id
                AND ur.deleted_at IS NULL
            )
          LIMIT 1""")) {
        ps.setString(1, adminEmail);
        ps.setString(2, "ROLE_ADMIN");
        ps.executeUpdate();
      }

      log.info("✅ Seeder selesai — roles=[{}], admin=<{}> (is_verified=true)",
          String.join(",", DEFAULT_ROLES), adminEmail);
    } catch (SQLException e) {
      throw new IllegalStateException("Seeder gagal dijalankan terhadap " + jdbcUrl, e);
    }
  }

  private static String envOr(String key, String def) {
    String value = System.getenv(key);
    return (value == null || value.isBlank()) ? def : value;
  }
}
