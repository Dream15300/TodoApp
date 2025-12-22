package com.example.persistence;

import com.example.domain.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository {

    public List<Category> findAll() {
        String sql = "SELECT Id, Name FROM Categories ORDER BY Name";
        List<Category> out = new ArrayList<>();

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                out.add(new Category(rs.getInt("Id"), rs.getString("Name")));
            }
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Kategorien laden fehlgeschlagen", e);
        }
    }

    public int insert(String name) {
        String sql = "INSERT INTO Categories (Name) VALUES (?)";

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getInt(1);
            }
            throw new RuntimeException("Keine ID zurueckgegeben");

        } catch (Exception e) {
            throw new RuntimeException("Kategorie einfuegen fehlgeschlagen", e);
        }
    }

    public void updateName(int id, String newName) {
        String sql = "UPDATE Categories SET Name = ? WHERE Id = ?";

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, newName);
            ps.setInt(2, id);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Kategorie umbenennen fehlgeschlagen", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM Categories WHERE Id = ?";

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Kategorie loeschen fehlgeschlagen", e);
        }
    }
}
