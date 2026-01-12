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
        migrateCategoriesAddIconColumn();
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

    /**
     * Migration für bestehende DBs: Spalte "Icon" in Categories nachträglich
     * hinzufügen.
     * (CREATE TABLE IF NOT EXISTS ändert bestehende Tabellen nicht.)
     */
    private static void migrateCategoriesAddIconColumn() {
        try (Connection connection = Db.open();
                Statement statement = connection.createStatement();
                var rs = statement.executeQuery("PRAGMA table_info(Categories)")) {

            boolean hasIcon = false;
            while (rs.next()) {
                String col = rs.getString("name");
                if ("Icon".equalsIgnoreCase(col)) {
                    hasIcon = true;
                    break;
                }
            }

            if (!hasIcon) {
                statement.execute("ALTER TABLE Categories ADD COLUMN Icon TEXT");
            }

            // Backfill: nur dort setzen, wo Icon noch NULL/leer ist
            statement.execute("""
                        UPDATE Categories SET Icon='💼'
                        WHERE (Icon IS NULL OR TRIM(Icon)='') AND Name='Arbeit'
                    """);
            statement.execute("""
                        UPDATE Categories SET Icon='🎓'
                        WHERE (Icon IS NULL OR TRIM(Icon)='') AND Name='Schule'
                    """);
            statement.execute("""
                        UPDATE Categories SET Icon='🏠'
                        WHERE (Icon IS NULL OR TRIM(Icon)='') AND Name='Privat'
                    """);
            statement.execute("""
                        UPDATE Categories SET Icon='📁'
                        WHERE (Icon IS NULL OR TRIM(Icon)='')
                    """);

        } catch (Exception exception) {
            throw new RuntimeException("DB migration failed: Categories.Icon", exception);
        }
    }

}
