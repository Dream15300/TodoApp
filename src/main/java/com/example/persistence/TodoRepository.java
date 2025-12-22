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

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            throw new RuntimeException("Todo-Existenz pruefen fehlgeschlagen", e);
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

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    out.add(map(rs));
            }

            return out;

        } catch (Exception e) {
            throw new RuntimeException("Todos laden fehlgeschlagen", e);
        }
    }

    public int insert(TodoItem item) {
        String sql = "INSERT INTO TodoItems (CategoryId, Title, DueDate, Status, Priority) VALUES (?, ?, ?, ?, ?)";

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, item.getCategoryId());
            ps.setString(2, item.getTitle());
            ps.setString(3, item.getDueDate() == null ? null : item.getDueDate().toString());
            ps.setInt(4, toDbStatus(item.getStatus()));
            ps.setInt(5, item.getPriority());

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next())
                    return keys.getInt(1);
            }
            throw new RuntimeException("Keine ID zurueckgegeben");

        } catch (Exception e) {
            throw new RuntimeException("Todo einfuegen fehlgeschlagen", e);
        }
    }

    public void updateStatus(int todoId, TodoStatus status) {
        String sql = "UPDATE TodoItems SET Status = ? WHERE Id = ?";

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, toDbStatus(status));
            ps.setInt(2, todoId);
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException("Todo-Status aktualisieren fehlgeschlagen", e);
        }
    }

    private TodoItem map(ResultSet rs) throws Exception {
        int id = rs.getInt("Id");
        int catId = rs.getInt("CategoryId");
        String title = rs.getString("Title");
        String due = rs.getString("DueDate");
        int status = rs.getInt("Status");
        int priority = rs.getInt("Priority");

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
