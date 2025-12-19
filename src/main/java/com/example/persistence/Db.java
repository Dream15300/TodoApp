package com.example.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {
    private static final String URL = "jdbc:sqlite:todo.db";

    private Db() {
    }

    public static Connection open() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
        }
        return c;
    }
}
