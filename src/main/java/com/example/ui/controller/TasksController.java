package com.example.ui.controller;

import com.example.domain.Category;
import com.example.domain.TodoItem;
import com.example.domain.TodoStatus;
import com.example.service.TodoService;
import com.example.ui.TodoUiText;
import com.example.ui.UiDialogs;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class TasksController {

    private final ListView<TodoItem> tasksView;
    private final TextField txtNewTaskTitle;
    private final DatePicker dpNewTaskDueDate;

    private final Button btnShowDone;
    private final Button btnBack;
    private final Button btnClearDone;

    private final TodoService service;
    private final Supplier<Category> selectedCategorySupplier;

    private final DateTimeFormatter dueFmt = DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN);

    private boolean showingDone = false;

    private ConfirmPopupController deleteDoneConfirmPopup;

    // verhindert, dass programatische Selektion (Refresh) Details öffnet
    private boolean suppressSelection = false;

    public TasksController(ListView<TodoItem> tasksView,
            TextField txtNewTaskTitle,
            DatePicker dpNewTaskDueDate,
            Button btnShowDone,
            Button btnBack,
            Button btnClearDone,
            TodoService service,
            Supplier<Category> selectedCategorySupplier) {
        this.tasksView = tasksView;
        this.txtNewTaskTitle = txtNewTaskTitle;
        this.dpNewTaskDueDate = dpNewTaskDueDate;
        this.btnShowDone = btnShowDone;
        this.btnBack = btnBack;
        this.btnClearDone = btnClearDone;
        this.service = service;
        this.selectedCategorySupplier = selectedCategorySupplier;
    }

    public void init() {
        setupTodoCells();

        deleteDoneConfirmPopup = new ConfirmPopupController(tasksView);
        deleteDoneConfirmPopup.init();
    }

    public void setOnUserSelection(javafx.beans.value.ChangeListener<TodoItem> listener) {
        tasksView.getSelectionModel().selectedItemProperty().addListener((observableValue, oldV, newV) -> {
            if (suppressSelection)
                return;
            listener.changed(observableValue, oldV, newV);
        });
    }

    public void clearSelectionProgrammatically() {
        suppressSelection = true;
        try {
            tasksView.getSelectionModel().clearSelection();
        } finally {
            suppressSelection = false;
        }
    }

    public void showOpen() {
        showingDone = false;
    }

    public void showDone() {
        showingDone = true;
    }

    public boolean isShowingDone() {
        return showingDone;
    }

    public void refresh() {
        Category category = selectedCategorySupplier.get();

        // Selektion merken (stabil über Refresh, weil Instanzen neu geladen werden)
        Integer selectedId = null;
        TodoItem selected = tasksView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            selectedId = selected.getId();
        }

        if (category == null) {
            suppressSelection = true;
            try {
                tasksView.getItems().setAll(List.of());
                tasksView.getSelectionModel().clearSelection();
            } finally {
                suppressSelection = false;
            }
            updateHistoryButtons(0);
            return;
        }

        int doneCount = service.countDoneTodosForCategory(category.getId());

        List<TodoItem> items = showingDone
                ? service.getDoneTodosForCategory(category.getId())
                : service.getOpenTodosForCategory(category.getId());

        suppressSelection = true;
        try {
            tasksView.getItems().setAll(items);

            // Selektion wiederherstellen (per ID)
            if (selectedId != null) {
                int idx = indexOfId(items, selectedId);
                if (idx >= 0) {
                    tasksView.getSelectionModel().select(idx);
                    tasksView.scrollTo(idx);
                } else {
                    tasksView.getSelectionModel().clearSelection();
                }
            } else {
                tasksView.getSelectionModel().clearSelection();
            }
        } finally {
            suppressSelection = false;
        }

        updateHistoryButtons(doneCount);
    }

    private int indexOfId(List<TodoItem> items, int id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == id)
                return i;
        }
        return -1;
    }

    public void onAddTask() {
        Category category = selectedCategorySupplier.get();
        if (category == null)
            return;

        String title = txtNewTaskTitle.getText() == null ? "" : txtNewTaskTitle.getText().trim();
        if (title.isEmpty())
            return;

        try {
            service.addTodo(category.getId(), title, dpNewTaskDueDate.getValue());
            txtNewTaskTitle.clear();
            dpNewTaskDueDate.setValue(null);

            showingDone = false;
            refresh();
        } catch (Exception exception) {
            UiDialogs.error("Todo konnte nicht hinzugefügt werden: " + exception.getMessage(), exception);
        }
    }

    public void onClearDone() {
        Category category = selectedCategorySupplier.get();
        if (category == null)
            return;

        if (deleteDoneConfirmPopup == null) {
            deleteDoneConfirmPopup = new ConfirmPopupController(tasksView);
            deleteDoneConfirmPopup.init();
        }

        deleteDoneConfirmPopup.showCentered(
                "Alle erledigten Aufgaben in dieser Liste löschen?",
                () -> {
                    try {
                        service.deleteDoneTodosByCategory(category.getId());
                        refresh();
                    } catch (Exception exception) {
                        UiDialogs.error("Erledigte Aufgaben konnten nicht gelöscht werden: " + exception.getMessage(),
                                exception);
                    }
                });
    }

    private void updateHistoryButtons(int doneCount) {
        boolean inHistory = showingDone;

        if (btnShowDone != null) {
            btnShowDone.setText("› Erledigt " + doneCount);
            btnShowDone.setVisible(!inHistory);
            btnShowDone.setManaged(!inHistory);
        }
        if (btnBack != null) {
            btnBack.setVisible(inHistory);
            btnBack.setManaged(inHistory);
        }
        if (btnClearDone != null) {
            btnClearDone.setVisible(inHistory);
            btnClearDone.setManaged(inHistory);
        }
    }

    private void setupTodoCells() {
        tasksView.setEditable(false);
        tasksView.setFixedCellSize(-1);

        tasksView.setCellFactory(lv -> new ListCell<>() {

            private final CheckBox checkBox = new CheckBox();
            private final Label title = new Label();
            private final Label due = new Label();
            private final VBox textBox = new VBox(2, title, due);

            private final Region spacer = new Region();
            private final Label notesIcon = new Label("✎");
            private final HBox root = new HBox(8, checkBox, textBox, spacer, notesIcon);

            {
                setPrefWidth(0);
                prefWidthProperty().bind(lv.widthProperty().subtract(18));

                root.setAlignment(Pos.CENTER_LEFT);
                root.setMaxWidth(Double.MAX_VALUE);

                HBox.setHgrow(textBox, Priority.ALWAYS);
                textBox.setMaxWidth(Double.MAX_VALUE);
                textBox.setMinWidth(0);

                HBox.setHgrow(spacer, Priority.ALWAYS);

                title.setWrapText(true);
                title.setMaxWidth(Double.MAX_VALUE);
                title.setMinWidth(0);
                title.getStyleClass().add("todo-title");

                due.getStyleClass().add("todo-due");
                due.setManaged(false);
                due.setVisible(false);

                textBox.getStyleClass().add("todo-textbox");

                notesIcon.getStyleClass().add("todo-notes-icon");
                notesIcon.setAlignment(Pos.CENTER);
                notesIcon.setMinWidth(40);
                notesIcon.setPrefWidth(40);
                notesIcon.setMaxWidth(40);

                // Falls es doch eng wird: nicht "…" rendern
                notesIcon.setTextOverrun(OverrunStyle.CLIP);
                notesIcon.setManaged(false);
                notesIcon.setVisible(false);

                checkBox.setOnAction(e -> {
                    TodoItem item = getItem();
                    if (item == null)
                        return;

                    try {
                        if (item.getStatus() == TodoStatus.DONE) {
                            service.markOpen(item.getId());
                        } else {
                            service.markDone(item.getId());
                        }
                        refresh();
                    } catch (Exception exception) {
                        checkBox.setSelected(item.getStatus() == TodoStatus.DONE);
                        UiDialogs.error("Status konnte nicht geändert werden: " + exception.getMessage(), exception);
                    }
                });
            }

            @Override
            protected void updateItem(TodoItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                checkBox.setSelected(item.getStatus() == TodoStatus.DONE);
                title.setText(TodoUiText.breakAnywhere(item.getTitle()));

                // Reset (wichtig wegen Cell-Reuse)
                due.getStyleClass().remove("overdue");

                if (item.getDueDate() != null) {
                    due.setText("📅 " + item.getDueDate().format(dueFmt));
                    due.setManaged(true);
                    due.setVisible(true);

                    boolean isDone = item.getStatus() == TodoStatus.DONE;
                    boolean isOverdue = !isDone && item.getDueDate().isBefore(java.time.LocalDate.now());

                    if (isOverdue) {
                        due.getStyleClass().add("overdue");
                    }
                } else {
                    due.setManaged(false);
                    due.setVisible(false);
                }

                boolean hasNotes = item.getNotes() != null && !item.getNotes().isBlank();
                notesIcon.setManaged(hasNotes);
                notesIcon.setVisible(hasNotes);

                setText(null);
                setGraphic(root);
            }

        });
    }
}
