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

    private final CategoryRepository categoryRepo = new CategoryRepository(); // Repository-Instanz für Kategorien
    private final TodoRepository todoRepo = new TodoRepository(); // Repository-Instanz für TodoItems

    public List<Category> getCategories() { // Holt alle Kategorien
        return categoryRepo.findAll();
    }

    public int createCategory(String name) { // Erstellt eine neue Kategorie und gibt die generierte ID zurück
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name ist Pflicht");
        }
        return categoryRepo.insert(name.trim());
    }

    public void renameCategory(int id, String newName) { // Benennt eine Kategorie um
        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("Name ist Pflicht");
        }
        categoryRepo.updateName(id, newName.trim());
    }

    public void deleteCategory(int categoryId) { // Löscht eine Kategorie, nur möglich, wenn keine Todos mehr vorhanden
                                                 // sind
        if (todoRepo.hasTodos(categoryId)) {
            throw new IllegalStateException("Liste enthält noch Todos. Erst Todos löschen/verschieben.");
        }
        categoryRepo.delete(categoryId);
    }

    public List<TodoItem> getOpenTodosForCategory(int categoryId) { // Holt offene Todos für eine Kategorie
        return todoRepo.findOpenByCategory(categoryId);
    }

    public List<TodoItem> getDoneTodosForCategory(int categoryId) { // Holt erledigte Todos für eine Kategorie
        return todoRepo.findDoneByCategory(categoryId);
    }

    public int addTodo(int categoryId, String title, LocalDate dueDate) { // Fügt ein neues Todo hinzu und gibt die
                                                                          // generierte ID zurück
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

    public void updateTodo(int todoId, String newTitle, LocalDate newDueDate) { // Aktualisiert Titel und
                                                                                // Fälligkeitsdatum eines Todos
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Titel darf nicht leer sein");
        }

        String sql = "UPDATE TodoItems SET Title = ?, DueDate = ? WHERE Id = ?";

        try (Connection connection = Db.open();
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, newTitle.trim());

            // DueDate ist TEXT: yyyy-MM-dd oder NULL
            if (newDueDate == null) {
                preparedStatement.setNull(2, Types.VARCHAR);
            } else {
                preparedStatement.setString(2, newDueDate.toString());
            }

            preparedStatement.setInt(3, todoId);

            int affected = preparedStatement.executeUpdate();
            if (affected == 0) {
                throw new IllegalStateException("Todo nicht gefunden: Id=" + todoId);
            }
        } catch (Exception exception) {
            throw new RuntimeException("updateTodo fehlgeschlagen", exception);
        }
    }

    public void deleteDoneTodosByCategory(int categoryId) { // Löscht alle erledigten Todos einer Kategorie
        String sql = """
                DELETE FROM TodoItems
                WHERE Status = ? AND CategoryId = ?
                """;

        try (var connection = Db.open();
                var preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, 1); // DONE
            preparedStatement.setInt(2, categoryId);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            throw new RuntimeException("deleteDoneTodosByCategory fehlgeschlagen", exception);
        }
    }

    // Todos erledigt / nicht erledigt
    public void markDone(int todoId) {
        todoRepo.updateStatus(todoId, TodoStatus.DONE);
    }

    public void markOpen(int todoId) {
        String sql = "UPDATE TodoItems SET Status = ? WHERE Id = ?";

        try (var connection = Db.open();
                var preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, 0); // OPEN
            preparedStatement.setInt(2, todoId);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            throw new RuntimeException("markOpen fehlgeschlagen", exception);
        }
    }

}
