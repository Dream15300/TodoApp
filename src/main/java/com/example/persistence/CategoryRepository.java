package com.example.persistence;

import com.example.domain.Category;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CategoryRepository { // Repository-Klasse für CRUD-Operationen auf Kategorien

    public List<Category> findAll() { // Findet alle Kategorien in der DB
        String sql = "SELECT Id, Name, Icon FROM Categories ORDER BY Name";
        List<Category> outputedList = new ArrayList<>();

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                outputedList.add(new Category(
                        resultSet.getInt("Id"),
                        resultSet.getString("Name"),
                        resultSet.getString("Icon")));
            }
            return outputedList;

        } catch (Exception exception) {
            throw new RuntimeException("Kategorien laden fehlgeschlagen", exception);
        }
    }

    public int insert(String name, String icon) { // Fügt eine neue Kategorie in die DB ein
        String sql = "INSERT INTO Categories (Name, Icon) VALUES (?, ?)";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, name);
            preparedStatement.setString(2, icon);

            preparedStatement.executeUpdate();

            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Kategorie konnte nicht eingefügt werden", exception);
        }

        return -1;
    }

    public void updateName(int id, String newName) { // Ändert den Namen einer Kategorie
        String sql = "UPDATE Categories SET Name = ? WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, newName);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Kategorie umbenennen fehlgeschlagen", exception);
        }
    }

    public void updateIcon(int id, String newIcon) { // Ändert das Icon einer Kategorie
        String sql = "UPDATE Categories SET Icon = ? WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, newIcon);
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Kategorie Icon ändern fehlgeschlagen", exception);
        }
    }

    public void delete(int id) { // Löscht eine Kategorie aus der DB
        String sql = "DELETE FROM Categories WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Kategorie löschen fehlgeschlagen", exception);
        }
    }
}
