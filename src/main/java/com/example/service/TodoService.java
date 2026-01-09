package com.example.service;

import com.example.domain.Category;
import com.example.domain.TodoItem;
import com.example.domain.TodoStatus;
import com.example.persistence.CategoryRepository;
import com.example.persistence.TodoRepository;

import java.time.LocalDate;
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
            throw new IllegalStateException("Liste enthält noch Todos. Erst Todos löschen/verschieben.");
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
                null,
                TodoStatus.OPEN,
                0);

        return todoRepo.insert(item);
    }

    public void updateTodo(int todoId, String newTitle, LocalDate newDueDate, String notes) {
        if (newTitle == null || newTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("Titel darf nicht leer sein");
        }

        todoRepo.updateTodo(todoId, newTitle.trim(), newDueDate, notes);
    }

    public void deleteDoneTodosByCategory(int categoryId) {
        todoRepo.deleteDoneByCategory(categoryId);
    }

    // Todos erledigt / nicht erledigt
    public void markDone(int todoId) {
        todoRepo.updateStatus(todoId, TodoStatus.DONE);
    }

    public void markOpen(int todoId) {
        todoRepo.updateStatus(todoId, TodoStatus.OPEN);
    }
}
