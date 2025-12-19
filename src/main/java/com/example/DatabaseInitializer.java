package com.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class DatabaseInitializer {

    private static final String DB_URL = "jdbc:sqlite:todo.db";

    private DatabaseInitializer() {
    }

    public static void init() {
        executeSqlResource("db/init_schema.sql");
        executeSqlResource("db/seed_base_data.sql");
    }

    private static void executeSqlResource(String resourcePath) {
        String sql = readResource(resourcePath);
        // split bei ';'
        String[] statements = sql.split(";");
        try (Connection con = DriverManager.getConnection(DB_URL);
                Statement st = con.createStatement()) {

            st.execute("PRAGMA foreign_keys = ON;");

            for (String raw : statements) {
                String s = raw.trim();
                if (!s.isEmpty()) {
                    st.execute(s);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("DB init failed for resource: " + resourcePath, e);
        }
    }

    private static String readResource(String resourcePath) {
        String p = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;

        try (var in = DatabaseInitializer.class.getResourceAsStream(p)) {
            if (in == null) {
                throw new IllegalStateException("Resource not found: " + p);
            }
            try (var br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return br.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read resource: " + p, e);
        }
    }

}
