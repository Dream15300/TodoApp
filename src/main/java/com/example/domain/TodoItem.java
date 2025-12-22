package com.example.domain;

import java.time.LocalDate;

public class TodoItem {
    private int id;
    private int categoryId;
    private String title;
    private LocalDate dueDate;
    private TodoStatus status;
    private int priority;

    public TodoItem() {
    }

    public TodoItem(int id, int categoryId, String title, LocalDate dueDate, TodoStatus status, int priority) {
        this.id = id;
        this.categoryId = categoryId;
        this.title = title;
        this.dueDate = dueDate;
        this.status = status;
        this.priority = priority;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public TodoStatus getStatus() {
        return status;
    }

    public void setStatus(TodoStatus status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String toDisplayString() {
        String d = (dueDate == null) ? "" : " (" + dueDate + ")";
        return title + d;
    }

    @Override
    public String toString() {
        String d = (dueDate == null) ? "" : " (" + dueDate + ")";
        return (status == TodoStatus.DONE ? "✔ " : "• ") + title + d;
    }
}
