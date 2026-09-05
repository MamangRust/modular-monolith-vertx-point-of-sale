package io.example.migration;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationApp {
  private static final Logger log = LoggerFactory.getLogger(MigrationApp.class);

  public static void main(String[] args) {
    log.info("🚀 Starting database migration runner...");

    String host = System.getenv().getOrDefault("DB_HOST_MIGRATE", 
                  System.getenv().getOrDefault("DB_HOST", "localhost"));
    String portStr = System.getenv().getOrDefault("DB_PORT", "5432");
    String dbName = System.getenv().getOrDefault("DB_NAME", "PAYMENT_GATEWAY");
    String username = System.getenv().getOrDefault("DB_USERNAME", "DRAGON");
    String password = System.getenv().getOrDefault("DB_PASSWORD", "DRAGON");

    String url = buildJdbcUrl(host, portStr, dbName);

    log.info("Connecting to database for migration at: {}", url);

    Flyway flyway = Flyway.configure()
        .dataSource(url, username, password)
        .baselineOnMigrate(true)
        .load();

    try {
      flyway.migrate();
      log.info("✅ Database migration completed successfully!");

      if (DbSeeder.isSeederEnabled()) {
        log.info("🌱 DB_SEEDER aktif — menjalankan seeder (roles + admin user)...");
        DbSeeder.seed(url, username, password);
      } else {
        log.info("⏭️  Seeder dilewati (DB_SEEDER != true).");
      }

      System.exit(0);
    } catch (Exception e) {
      log.error("❌ Database migration/seeding failed!", e);
      System.exit(1);
    }
  }

  /**
   * Builds the JDBC URL used by Flyway. Extracted for testability.
   */
  static String buildJdbcUrl(String host, String port, String dbName) {
    return String.format("jdbc:postgresql://%s:%s/%s", host, port, dbName);
  }
}
