package io.example.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

class MigrationAppTest {

  private static final Pattern VERSIONED_SCRIPT = Pattern.compile("V\\d+__.*\\.sql");

  @Test
  void buildJdbcUrlFormatsPostgresUrl() {
    assertThat(MigrationApp.buildJdbcUrl("localhost", "5432", "POINT_OF_SALE"))
        .isEqualTo("jdbc:postgresql://localhost:5432/POINT_OF_SALE");
    assertThat(MigrationApp.buildJdbcUrl("postgres", "6432", "POSDB"))
        .isEqualTo("jdbc:postgresql://postgres:6432/POSDB");
  }

  @Test
  void migrationScriptsAreCompleteAndSequential() throws Exception {
    List<Integer> versions = migrationScriptNames().stream()
        .map(this::parseVersion)
        .toList();

    // V1..V14 — tidak boleh ada gap, duplikat, atau typo penomoran.
    assertThat(versions).as("Daftar versi migration dari classpath").containsExactlyInAnyOrder(
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14);
  }

  @Test
  void migrationScriptNamesFollowFlywayConvention() throws Exception {
    assertThat(migrationScriptNames())
        .allMatch(name -> VERSIONED_SCRIPT.matcher(name).matches());
  }

  private Set<String> migrationScriptNames() throws Exception {
    Set<String> names = new TreeSet<>();
    Enumeration<URL> urls = getClass().getClassLoader().getResources("db/migration");

    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      if ("file".equals(url.getProtocol())) {
        Path dir = Paths.get(url.toURI());
        if (Files.isDirectory(dir)) {
          try (Stream<Path> files = Files.list(dir)) {
            files.map(path -> path.getFileName().toString())
                .filter(name -> VERSIONED_SCRIPT.matcher(name).matches())
                .forEach(names::add);
          }
        }
      } else if ("jar".equals(url.getProtocol())) {
        JarURLConnection connection = (JarURLConnection) url.openConnection();
        JarFile jar = connection.getJarFile();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
          String name = entries.nextElement().getName();
          if (name.startsWith("db/migration/") && !name.endsWith("/")) {
            String fileName = name.substring("db/migration/".length());
            if (VERSIONED_SCRIPT.matcher(fileName).matches()) {
              names.add(fileName);
            }
          }
        }
      }
    }

    return names;
  }

  private int parseVersion(String fileName) {
    Matcher matcher = Pattern.compile("V(\\d+)__").matcher(fileName);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Nama migration tidak valid: " + fileName);
    }
    return Integer.parseInt(matcher.group(1));
  }
}
