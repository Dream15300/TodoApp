// verantworlich für Datenzugriff (Persistence Layer, kapselt)
// Db.open() --> Baut JDBC-Verbindung zu SQLite auf, JDBC = Java Database Connectivity API (Datenzugriff)
package com.example.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {

    // App-spezifische Ablage (stabil bei Installer/Shortcut/verschiedenen Working
    // Directories)
    private static final String APP_DIR_NAME = "TodoApp";
    private static final String DB_FILE_NAME = "todo.db";

    // Lazy-initialisiert, weil Pfad/Env erst zur Laufzeit sicher bestimmbar ist
    private static volatile String jdbcUrl;

    private Db() { // Konstruktor privat --> verhindert Instanzierung neuer Db
    }

    public static Connection open() throws SQLException { // Rückgabe: Connection zur DB
        String url = getJdbcUrl();
        Connection connection = DriverManager.getConnection(url); // Baut DB-Verbindung auf
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON;"); // Foreign Keys erzwingen (SQLite default: OFF)
        }
        return connection;
    }

    /**
     * Gibt die JDBC-URL zur DB in einem stabilen User-App-Data-Verzeichnis zurück.
     * Beim ersten Zugriff wird:
     * - Zielverzeichnis erstellt
     * - optional eine legacy "./todo.db" migriert (kopiert), falls vorhanden
     */
    private static String getJdbcUrl() {
        String local = jdbcUrl;
        if (local != null) {
            return local;
        }

        synchronized (Db.class) {
            if (jdbcUrl != null) {
                return jdbcUrl;
            }

            Path dbPath = resolveAppDataDbPath();
            ensureParentDirectory(dbPath);
            migrateLegacyDbIfNeeded(dbPath);

            // SQLite JDBC akzeptiert absolute Pfade; Backslashes sind meist ok,
            // aber "/" ist in JDBC-URLs robuster.
            String normalized = dbPath.toAbsolutePath().toString().replace("\\", "/");
            jdbcUrl = "jdbc:sqlite:" + normalized;
            return jdbcUrl;
        }
    }

    private static Path resolveAppDataDbPath() {
        String os = System.getProperty("os.name", "").toLowerCase();

        // Windows: %APPDATA%\TodoApp\todo.db
        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, APP_DIR_NAME, DB_FILE_NAME);
            }
            // Fallback, falls APPDATA fehlt
            return Paths.get(System.getProperty("user.home"), "AppData", "Roaming", APP_DIR_NAME, DB_FILE_NAME);
        }

        // macOS: ~/Library/Application Support/TodoApp/todo.db
        if (os.contains("mac")) {
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support", APP_DIR_NAME,
                    DB_FILE_NAME);
        }

        // Linux/Unix: $XDG_DATA_HOME/TodoApp/todo.db oder
        // ~/.local/share/TodoApp/todo.db
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome != null && !xdgDataHome.isBlank()) {
            return Paths.get(xdgDataHome, APP_DIR_NAME, DB_FILE_NAME);
        }
        return Paths.get(System.getProperty("user.home"), ".local", "share", APP_DIR_NAME, DB_FILE_NAME);
    }

    private static void ensureParentDirectory(Path dbPath) {
        try {
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create DB directory for: " + dbPath, exception);
        }
    }

    /**
     * Migriert eine alte DB aus dem Working Directory (./todo.db),
     * falls dort eine existiert und am Ziel noch keine DB liegt.
     */
    private static void migrateLegacyDbIfNeeded(Path targetDbPath) {
        try {
            Path legacy = Paths.get(DB_FILE_NAME); // relativ zum Working Directory
            if (Files.exists(legacy) && !Files.exists(targetDbPath)) {
                Files.copy(legacy, targetDbPath);
            }
        } catch (Exception exception) {
            // Migration ist "best-effort": Wenn das Kopieren scheitert, soll die App
            // trotzdem starten können
            // und eine neue DB erzeugen. Darum kein throw.
        }
    }
}
