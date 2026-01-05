package com.example.persistence;

import com.example.domain.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository { // Repository-Klasse für CRUD-Operationen auf Kategorien

    public List<Category> findAll() { // Findet alle Kategorien in der DB
        String sql = "SELECT Id, Name FROM Categories ORDER BY Name";
        List<Category> outputedList = new ArrayList<>();

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                outputedList.add(new Category(resultSet.getInt("Id"), resultSet.getString("Name")));
            }
            return outputedList;

        } catch (Exception exception) {
            throw new RuntimeException("Kategorien laden fehlgeschlagen", exception);
        }
    }

    public int insert(String name) { // Fügt eine neue Kategorie in die DB ein und gibt die generierte ID zurück
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
            throw new RuntimeException("Keine ID zurückgegeben");

        } catch (Exception exception) {
            throw new RuntimeException("Kategorie einfügen fehlgeschlagen", exception);
        }
    }

    public void updateName(int id, String newName) { // Aktualisiert den Namen einer Kategorie anhand der ID
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

    public void delete(int id) { // Löscht eine Kategorie anhand der ID
        String sql = "DELETE FROM Categories WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();

        } catch (Exception exception) {
            throw new RuntimeException("Kategorie löschen fehlgeschlagen", exception);
        }
    }
}
