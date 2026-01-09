package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class DatabaseInitializer { // final --> darf nicht vererbt werden

    private static final String DB_URL = "jdbc:sqlite:todo.db";

    private DatabaseInitializer() { // private --> verhindert Instanzierung, Nutzung nur über statische Methoden
    }

    public static void init() {
        executeSqlResource("db/init_schema.sql");
        executeSqlResource("db/seed_base_data.sql");
    }

    private static void executeSqlResource(String resourcePath) {
        String sql = readResource(resourcePath); // Liest Inhalt als String
        // split bei ';'
        String[] statements = sql.split(";"); // Zerlegt SQL in einzelne Statements
        try (Connection connection = DriverManager.getConnection(DB_URL);
                Statement statement = connection.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON;");

            for (String raw : statements) { // Iteration über alle SQL-Teilstrings
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
    /*
     * 1. Pfad normieren (Classpath-konform)
     * 2. Resource als InputStream laden
     * 3. Bytestrom → UTF-8-Zeichenstrom umwandeln
     * 4. Zeilenweise lesen
     * 5. Zu einem String zusammenfügen
     * 6. Fehler sauber kapseln und weiterreichen
     */
    private static String readResource(String resourcePath) { // Lädt Datei aus Classpath
        String path = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath; // absoluter Pfad

        try (var initializer = DatabaseInitializer.class.getResourceAsStream(path)) { // getResourceAsStream erwartet
                                                                                      // absoluten Pfad
            if (initializer == null) {
                throw new IllegalStateException("Resource not found: " + path);
            }
            try (var bufferedReader = new BufferedReader(new InputStreamReader(initializer, StandardCharsets.UTF_8))) { // wandelt
                                                                                                                        // Bytestream
                                                                                                                        // in
                                                                                                                        // UTF-8-Zeichenstrom
                                                                                                                        // Buffered
                                                                                                                        // Reader
                                                                                                                        // -->
                                                                                                                        // effizientes
                                                                                                                        // zeilenweises
                                                                                                                        // Lesen
                return bufferedReader.lines().collect(Collectors.joining("\n")); // .lines (jede Zeile ein Element)
                                                                                 // → Stream
                                                                                 // Collectors.joining("\n")
                                                                                 // → String mit Zeilenumbruch
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read resource: " + path, exception);
        }
    }

}
