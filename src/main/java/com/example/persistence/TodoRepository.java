package com.example.persistence;

import com.example.domain.TodoItem;
import com.example.domain.TodoStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TodoRepository {

    public boolean hasTodos(int categoryId) {
        String sql = "SELECT 1 FROM TodoItems WHERE CategoryId = ? LIMIT 1";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, categoryId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }

        } catch (Exception exception) {
            throw new RuntimeException("Todo-Existenz pruefen fehlgeschlagen", exception);
        }
    }

    public List<TodoItem> findOpenByCategory(int categoryId) {
        String sql = """
                SELECT Id, CategoryId, Title, DueDate, Status, Priority
                FROM TodoItems
                WHERE CategoryId = ? AND Status = 0
                ORDER BY Priority DESC, DueDate IS NULL, DueDate, Id
                """;
        return queryList(sql, categoryId);
    }

    public List<TodoItem> findDoneByCategory(int categoryId) {
        String sql = """
                SELECT Id, CategoryId, Title, DueDate, Status, Priority
                FROM TodoItems
                WHERE CategoryId = ? AND Status = 1
                ORDER BY DueDate IS NULL, DueDate DESC, Id DESC
                """;
        return queryList(sql, categoryId);
    }

    private List<TodoItem> queryList(String sql, int categoryId) {
        List<TodoItem> out = new ArrayList<>();

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, categoryId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next())
                    out.add(map(resultSet));
            }

            return out;

        } catch (Exception exception) {
            throw new RuntimeException("Todos laden fehlgeschlagen", exception);
        }
    }

    public int insert(TodoItem item) {
        String sql = "INSERT INTO TodoItems (CategoryId, Title, DueDate, Status, Priority) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setInt(1, item.getCategoryId());
            preparedStatement.setString(2, item.getTitle());
            preparedStatement.setString(3, item.getDueDate() == null ? null : item.getDueDate().toString());
            preparedStatement.setInt(4, toDbStatus(item.getStatus()));
            preparedStatement.setInt(5, item.getPriority());

            preparedStatement.executeUpdate();

            try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getInt(1);
            }
            throw new RuntimeException("Keine ID zurueckgegeben");

        } catch (Exception exception) {
            throw new RuntimeException("Todo einfuegen fehlgeschlagen", exception);
        }
    }

    public void updateStatus(int todoId, TodoStatus status) {
        String sql = "UPDATE TodoItems SET Status = ? WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, toDbStatus(status));
            preparedStatement.setInt(2, todoId);
            preparedStatement.executeUpdate();

        } catch (Exception exception) {
            throw new RuntimeException("Todo-Status aktualisieren fehlgeschlagen", exception);
        }
    }

    private TodoItem map(ResultSet resultSet) throws Exception {
        int id = resultSet.getInt("Id");
        int catId = resultSet.getInt("CategoryId");
        String title = resultSet.getString("Title");
        String due = resultSet.getString("DueDate");
        int status = resultSet.getInt("Status");
        int priority = resultSet.getInt("Priority");

        LocalDate dueDate = (due == null || due.isBlank()) ? null : LocalDate.parse(due);
        TodoStatus st = fromDbStatus(status);

        return new TodoItem(id, catId, title, dueDate, st, priority);
    }

    private int toDbStatus(TodoStatus s) {
        return (s == TodoStatus.DONE) ? 1 : 0;
    }

    private TodoStatus fromDbStatus(int v) {
        return (v == 1) ? TodoStatus.DONE : TodoStatus.OPEN;
    }
}
