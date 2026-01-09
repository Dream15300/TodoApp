package com.example;

import com.example.persistence.Db;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class DatabaseInitializer { // final --> darf nicht vererbt werden

    private DatabaseInitializer() { // private --> verhindert Instanzierung, Nutzung nur über statische Methoden
    }

    public static void init() {
        executeSqlResource("db/init_schema.sql");
        executeSqlResource("db/seed_base_data.sql");
    }

    private static void executeSqlResource(String resourcePath) {
        String sql = readResource(resourcePath); // Liest Inhalt als String
        String[] statements = sql.split(";"); // Zerlegt SQL in einzelne Statements

        try (Connection connection = Db.open();
                Statement statement = connection.createStatement()) {

            for (String raw : statements) {
                String s = raw.trim();
                if (!s.isEmpty()) {
                    statement.execute(s);
                }
            }
        } catch (Exception exception) {
            throw new RuntimeException("DB init failed for resource: " + resourcePath, exception);
        }
    }

    // Gibt Inhalt als String zurück
    private static String readResource(String resourcePath) { // Lädt Datei aus Classpath
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath; // absoluter Pfad

        try (var initializer = DatabaseInitializer.class.getResourceAsStream(path)) { // getResourceAsStream erwartet
                                                                                      // absoluten Pfad
            if (initializer == null) {
                throw new IllegalStateException("Resource not found: " + path);
            }
            try (var bufferedReader = new BufferedReader(new InputStreamReader(initializer, StandardCharsets.UTF_8))) {
                return bufferedReader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read resource: " + path, exception);
        }
    }
}
