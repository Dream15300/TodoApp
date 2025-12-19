package com.example.persistence;

import com.example.domain.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
            throw new RuntimeException("Categories laden fehlgeschlagen", e);
        }
    }
}
