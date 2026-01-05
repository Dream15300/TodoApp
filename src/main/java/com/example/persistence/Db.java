// verantworlich für Datenzugriff (Persistence Layer, kapselt)
// Db.open() --> Baut JDBC-Verbindung zu SQLite auf, JDBC = Java Database Connectivity API (Datenzugriff)
package com.example.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {
    private static final String URL = "jdbc:sqlite:todo.db"; // Nicht änderbarer JDBC-URL für SQLite

    private Db() { // Konstruktor privat --> verhindert Instanzierung neuer Db
    }

    public static Connection open() throws SQLException { // Rückgabe: Connection zur DB
        Connection connection = DriverManager.getConnection(URL); // DriverManager --> managt JDBC-Driver, Baut
                                                                  // DB-Verbindung auf
        // anhand JDBC-URL, Rückgabe: offene Connection (return connection)
        try (Statement statement = connection.createStatement()) { // statement = Schnittstelle zur Ausführung
                                                                   // statischer
            // SQL-Anweisungen, create-Abschnitt: erstellt Statement-Objekt,
            // durch try automatisch geschlossen
            statement.execute("PRAGMA foreign_keys = ON;"); // Führt SQLite-Befehl aus, aktiviert Durchsetzung von
                                                            // Foreign-Keys
            // (sind bei SQLite standardmässig deaktiviert)
        }
        return connection;
    }
}
/*
 * Konsequenz --> Aufbau Repos:
 * try (Connection connection = Db.open();
 * PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
 * ...
 * }
 */