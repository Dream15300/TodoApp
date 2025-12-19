// verantworlich für Datenzugriff (Persistence Layer, kapselt)
// Kapselt SQL und JDBC-Zugriffe
package com.example.persistence;

import com.example.domain.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    // Ladet alle Kategorien aus der Datenbank
    public List<Category> findAll() {
        String sql = "SELECT Id, Name FROM Categories ORDER BY Name";
        List<Category> out = new ArrayList<>();

        try (Connection c = Db.open(); // Db.open() --> siehe Db.java-Datei
                PreparedStatement ps = c.prepareStatement(sql); // kompiliert SQL, schützt vor SQL-Injection,
                                                                // Performance-Vorteil
                ResultSet rs = ps.executeQuery()) { // Führt Select aus und liefert daraus ein ResultSet

            while (rs.next()) { // next springt zeilenweise durch das Ergebnis und fügt Objekte der Liste hinzu
                out.add(new Category(rs.getInt("Id"), rs.getString("Name"))); // Mapping-Prinzip
            }
            return out;

        } catch (Exception e) { // Fehlerbehandlung
            throw new RuntimeException("Kategorien laden fehlgeschlagen", e);
        }
    }
}
