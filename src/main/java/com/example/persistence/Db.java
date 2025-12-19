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
        Connection c = DriverManager.getConnection(URL); // DriverManager --> managt JDBC-Driver, Baut DB-Verbindung auf
                                                         // anhand JDBC-URL, Rückgabe: offene Connection (return c)
        try (Statement st = c.createStatement()) { // statement = Schnittstelle zur Ausführung statischer
                                                   // SQL-Anweisungen, create-Abschnitt: erstellt Statement-Objekt,
                                                   // durch try automatisch geschlossen
            st.execute("PRAGMA foreign_keys = ON;"); // Führt SQLite-Befehl aus, aktiviert Durchsetzung von Foreign-Keys
                                                     // (sind bei SQLite standardmässig deaktiviert)
        }
        return c;
    }
}
/*
 * Konsequenz --> Aufbau Repos:
 * try (Connection c = Db.open();
 * PreparedStatement ps = c.prepareStatement(sql)) {
 * ...
 * }
 */