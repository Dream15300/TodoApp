package com.example.ui.controller;

import com.example.domain.TodoItem;
import com.example.service.TodoService;
import com.example.ui.UiDialogs;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDate;

public class DetailsController {

    private final VBox detailsPane;
    private final TextField detailsTitle;
    private final DatePicker detailsDueDate;
    private final TextArea detailsNotes;

    private final TodoService service;

    private TodoItem detailsItem;

    public DetailsController(VBox detailsPane, TextField detailsTitle, DatePicker detailsDueDate, TextArea detailsNotes,
            TodoService service) {
        this.detailsPane = detailsPane;
        this.detailsTitle = detailsTitle;
        this.detailsDueDate = detailsDueDate;
        this.detailsNotes = detailsNotes;
        this.service = service;
    }

    public void open(TodoItem item) {
        detailsItem = item;

        detailsTitle.setText(item.getTitle());
        detailsDueDate.setValue(item.getDueDate());
        detailsNotes.setText(item.getNotes() == null ? "" : item.getNotes());

        detailsPane.setManaged(true);
        detailsPane.setVisible(true);
    }

    public void close() {
        detailsItem = null;
        detailsPane.setVisible(false);
        detailsPane.setManaged(false);
    }

    public boolean isOpen() {
        return detailsPane.isVisible();
    }

    public void clearDueDate() {
        detailsDueDate.setValue(null);
    }

    /**
     * Speichern und true zurückgeben, wenn Update erfolgreich war (caller kann dann
     * refreshen).
     */
    public boolean save() {
        if (detailsItem == null)
            return false;

        String newTitle = detailsTitle.getText() == null ? "" : detailsTitle.getText().trim();
        if (newTitle.isEmpty())
            return false;

        LocalDate newDue = detailsDueDate.getValue();
        String notes = detailsNotes.getText();

        try {
            service.updateTodo(detailsItem.getId(), newTitle, newDue, notes);
            return true;
        } catch (Exception exception) {
            UiDialogs.error("Aufgabe konnte nicht aktualisiert werden: " + exception.getMessage(), exception);
            return false;
        }
    }
}
