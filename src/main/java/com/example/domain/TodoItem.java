package com.example.domain;

import java.time.LocalDate;

public class TodoItem {
    private int id;
    private int categoryId;
    private String title;
    private LocalDate dueDate;
    private TodoStatus status;
    private String notes;

    public TodoItem() {
    }

    public TodoItem(int id, int categoryId, String title, LocalDate dueDate, String notes, TodoStatus status) {
        this.id = id;
        this.categoryId = categoryId;
        this.title = title;
        this.dueDate = dueDate;
        this.notes = notes;
        this.status = status;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
