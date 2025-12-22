package com.example.service;

import com.example.domain.Category;
import com.example.domain.TodoItem;
import com.example.domain.TodoStatus;
import com.example.persistence.CategoryRepository;
import com.example.persistence.Db;
import com.example.persistence.TodoRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.sql.Types;
import java.util.List;

public class TodoService {

    private final CategoryRepository categoryRepo = new CategoryRepository();
    private final TodoRepository todoRepo = new TodoRepository();

    public List<Category> getCategories() {
        return categoryRepo.findAll();
    }

    public int createCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name ist Pflicht");
        }
        return categoryRepo.insert(name.trim());
    }

    public void renameCategory(int id, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Name ist Pflicht");
        }
        categoryRepo.updateName(id, newName.trim());
    }

    public void deleteCategory(int categoryId) {
        if (todoRepo.hasTodos(categoryId)) {
            throw new IllegalStateException("Liste enthaelt noch Todos. Erst Todos loeschen/verschieben.");
        }
        categoryRepo.delete(categoryId);
    }

    public List<TodoItem> getOpenTodosForCategory(int categoryId) {
        return todoRepo.findOpenByCategory(categoryId);
    }

    public List<TodoItem> getDoneTodosForCategory(int categoryId) {
        return todoRepo.findDoneByCategory(categoryId);
    }

    public int addTodo(int categoryId, String title, LocalDate dueDate) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Titel ist Pflicht");
        }

        TodoItem item = new TodoItem(
                0,
                categoryId,
                title.trim(),
                dueDate,
                TodoStatus.OPEN,
                0);

        return todoRepo.insert(item);
    }

    public void updateTodo(int todoId, String newTitle, LocalDate newDueDate) {
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Title darf nicht leer sein");
        }

        String sql = "UPDATE TodoItems SET Title = ?, DueDate = ? WHERE Id = ?";

        try (Connection c = Db.open();
                PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, newTitle.trim());

            // DueDate ist TEXT: yyyy-MM-dd oder NULL
            if (newDueDate == null) {
                ps.setNull(2, Types.VARCHAR);
            } else {
                ps.setString(2, newDueDate.toString());
            }

            ps.setInt(3, todoId);

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Todo nicht gefunden: Id=" + todoId);
            }
        } catch (Exception e) {
            throw new RuntimeException("updateTodo fehlgeschlagen", e);
        }
    }

    public void deleteDoneTodosByCategory(int categoryId) {
        String sql = """
                DELETE FROM TodoItems
                WHERE Status = ? AND CategoryId = ?
                """;

        try (var c = Db.open();
                var ps = c.prepareStatement(sql)) {

            ps.setInt(1, 1); // DONE
            ps.setInt(2, categoryId);

            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("deleteDoneTodosByCategory fehlgeschlagen", e);
        }
    }

    // Todos erledigt / nicht erledigt
    public void markDone(int todoId) {
        todoRepo.updateStatus(todoId, TodoStatus.DONE);
    }

    public void markOpen(int todoId) {
        String sql = "UPDATE TodoItems SET Status = ? WHERE Id = ?";

        try (var c = Db.open();
                var ps = c.prepareStatement(sql)) {

            ps.setInt(1, 0); // OPEN
            ps.setInt(2, todoId);

            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("markOpen fehlgeschlagen", e);
        }
    }

}
