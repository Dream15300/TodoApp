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

    public List<TodoItem> findByCategory(int categoryId) {
        String sql = """
                SELECT Id, CategoryId, Title, DueDate, Status
                FROM TodoItems
                WHERE CategoryId = ?
                ORDER BY CASE WHEN Status = 1 THEN 1 ELSE 0 END, DueDate IS NULL, DueDate, Id
                """;

        List<TodoItem> out = new ArrayList<>();

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setInt(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(map(rs));
                }
            }
            return out;

        } catch (Exception e) {
            throw new RuntimeException("Todos laden fehlgeschlagen", e);
        }
    }

    public int insert(TodoItem item) {
        String sql = "INSERT INTO TodoItems (CategoryId, Title, DueDate, Status) VALUES (?, ?, ?, ?)";

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, item.getCategoryId());
            ps.setString(2, item.getTitle());
            ps.setString(3, item.getDueDate() == null ? null : item.getDueDate().toString());
            ps.setInt(4, toDbStatus(item.getStatus()));

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

        LocalDate dueDate = (due == null || due.isBlank()) ? null : LocalDate.parse(due);
        TodoStatus st = fromDbStatus(status);

        return new TodoItem(id, catId, title, dueDate, st);
    }

    private int toDbStatus(TodoStatus s) {
        // Annahme: 0 = OPEN, 1 = DONE
        return (s == TodoStatus.DONE) ? 1 : 0;
    }

    private TodoStatus fromDbStatus(int v) {
        return (v == 1) ? TodoStatus.DONE : TodoStatus.OPEN;
    }
}
