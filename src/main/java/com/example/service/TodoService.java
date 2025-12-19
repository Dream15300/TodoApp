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

    public List<TodoItem> getTodosForCategory(int categoryId) {
        return todoRepo.findByCategory(categoryId);
    }

    public int addTodo(int categoryId, String title, LocalDate dueDate) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Titel ist Pflicht");
        }

        TodoItem item = new TodoItem(0, categoryId, title.trim(), dueDate, TodoStatus.OPEN);
        return todoRepo.insert(item);
    }

    public void markDone(int todoId) {
        todoRepo.updateStatus(todoId, TodoStatus.DONE);
    }
}
