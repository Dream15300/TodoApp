package com.example;

import com.example.persistence.Db;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.stream.Collectors;
import java.sql.ResultSet;

/**
 * Ziele:
 * - Schema erstellen (falls noch nicht vorhanden)
 * - Migrationen für bestehende DBs ausführen (z. B. neue Spalten)
 * - Seed-Daten einfügen (Basisdaten)
 *
 * Design:
 * - final + privater Konstruktor: Utility-Klasse, nur statische Nutzung
 * - SQL-Dateien werden aus dem Classpath geladen (Resources)
 */
public final class DatabaseInitializer { // final --> darf nicht vererbt werden

    private static final int DB_VERSION = 2;

    private DatabaseInitializer() { // private --> verhindert Instanzierung, Nutzung nur über statische Methoden
    }

    /**
     * Einstiegspunkt für DB-Setup.
     *
     * Reihenfolge:
     * 1) init_schema.sql ausführen (Tabellen erstellen, falls nicht vorhanden)
     * 2) Migration: Categories.Icon-Spalte nachrüsten (für bestehende DBs)
     * 3) seed_base_data.sql ausführen (Basis-Kategorien etc.)
     */
    public static void init() {
        // 1) Basis-Schema sicherstellen (idempotent)
        executeSqlResource("db/init_schema.sql");

        // 2) Migrationen anhand user_version
        migrateToLatest();

        // 3) Seed nur wenn leer (nur erste Initialisierung)
        if (isTableEmpty("Categories")) {
            executeSqlResource("db/seed_base_data.sql");
        }
    }

    /**
     * Lädt eine SQL-Resource als String, splittet sie an ';' und führt jedes
     * Statement aus.
     *
     * Technischer Hintergrund:
     * - SQLite JDBC Statement.execute(...) akzeptiert pro Aufruf i. d. R. ein
     * Statement.
     * - Das Splitten ermöglicht Batch-Ausführung ohne Script-Engine.
     *
     * @param resourcePath Pfad zur Resource (z. B. "db/init_schema.sql")
     */
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

    private static boolean isTableEmpty(String table) {
        String sql = "SELECT 1 FROM " + table + " LIMIT 1";

        try (Connection connection = Db.open();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {

            return !rs.next();

        } catch (Exception exception) {
            // Wenn Tabelle nicht existiert o.ä., lieber seeden, damit App funktioniert
            return true;
        }
    }

    private static void migrateToLatest() {
        try (Connection connection = Db.open();
                Statement statement = connection.createStatement()) {

            int current = readUserVersion(statement);

            if (current < 1) {
                writeUserVersion(statement, 1);
                current = 1;
            }

            while (current < DB_VERSION) {
                int next = current + 1;

                switch (next) {
                    case 2 -> migrateCategoriesAddIconColumn(connection);
                    // case 3 -> migrateX(connection);
                    default -> throw new IllegalStateException("Keine Migration definiert für Version " + next);
                }

                writeUserVersion(statement, next);
                current = next;
            }

        } catch (Exception exception) {
            throw new RuntimeException("DB migration failed", exception);
        }
    }

    private static int readUserVersion(Statement statement) throws Exception {
        try (ResultSet rs = statement.executeQuery("PRAGMA user_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void writeUserVersion(Statement statement, int version) throws Exception {
        statement.execute("PRAGMA user_version = " + version);
    }

    /**
     * Liest eine Resource-Datei aus dem Classpath und gibt ihren Inhalt als String
     * zurück.
     *
     * Ablauf:
     * - resourcePath wird zu einem absoluten Pfad normalisiert (führt mit '/')
     * - getResourceAsStream(...) liefert InputStream oder null, wenn nicht gefunden
     * - Stream wird als UTF-8 gelesen und zeilenweise zusammengefügt
     *
     * @param resourcePath relativer oder absoluter Resource-Pfad
     * @return kompletter Dateiinhalt als String
     */
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
     *
     * Hintergrund:
     * - CREATE TABLE IF NOT EXISTS verändert bestehende Tabellen nicht.
     * - Deshalb braucht es Migrationen, wenn sich das Schema weiterentwickelt.
     *
     * Vorgehen:
     * 1) PRAGMA table_info(Categories) abfragen (liefert Metadaten zu Spalten)
     * 2) Prüfen, ob Spalte "Icon" existiert (case-insensitive)
     * 3) Falls nicht: ALTER TABLE ... ADD COLUMN Icon TEXT
     * 4) Backfill: Standard-Icons setzen (nur wenn Icon NULL/leer ist)
     */
    private static void migrateCategoriesAddIconColumn(Connection connection) {
        try (Statement statement = connection.createStatement();
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

            // Backfill nur wenn leer/NULL
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
