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

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                out.add(new Category(resultSet.getInt("Id"), resultSet.getString("Name")));
            }
            return out;

        } catch (Exception exception) {
            throw new RuntimeException("Kategorien laden fehlgeschlagen", exception);
        }
    }

    public int insert(String name) {
        String sql = "INSERT INTO Categories (Name) VALUES (?)";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, name);
            preparedStatement.executeUpdate();

            try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getInt(1);
            }
            throw new RuntimeException("Keine ID zurueckgegeben");

        } catch (Exception exception) {
            throw new RuntimeException("Kategorie einfuegen fehlgeschlagen", exception);
        }
    }

    public void updateName(int id, String newName) {
        String sql = "UPDATE Categories SET Name = ? WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, newName);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();

        } catch (Exception exception) {
            throw new RuntimeException("Kategorie umbenennen fehlgeschlagen", exception);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM Categories WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();

        } catch (Exception exception) {
            throw new RuntimeException("Kategorie loeschen fehlgeschlagen", exception);
        }
    }
}
