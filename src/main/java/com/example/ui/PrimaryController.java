package com.example.ui;

import com.example.App;
import com.example.domain.Category;
import com.example.domain.TodoItem;
import com.example.domain.TodoStatus;
import com.example.service.TodoService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class PrimaryController {

    @FXML
    private ListView<Category> listsView;
    @FXML
    private ListView<TodoItem> tasksView;
    @FXML
    private TextField txtNewTaskTitle;
    @FXML
    private DatePicker dpNewTaskDueDate;

    // Footer Buttons (Tasks)
    @FXML
    private Button btnShowDone; // "› Erledigt X"
    @FXML
    private Button btnBack; // "‹ Zurueck"
    @FXML
    private Button btnClearDone; // "Alle erledigten löschen"

    private final TodoService service = new TodoService();
    private boolean showingDone = false;

    // ------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------
    @FXML
    private void openSecondary(ActionEvent e) throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("secondary.fxml"));
        Scene scene = new Scene(loader.load(), 700, 400);

        Stage stage = new Stage();
        stage.setTitle("Secondary");
        stage.setScene(scene);
        stage.show();
    }

    // ------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------
    @FXML
    private void initialize() {
        setupCategoryCells();
        setupTodoCells();

        loadCategories();

        listsView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            showingDone = false;
            refreshTasks();
        });

        if (!listsView.getItems().isEmpty()) {
            listsView.getSelectionModel().selectFirst();
        }

        refreshTasks();
    }

    // ------------------------------------------------------------
    // Kategorien: Name + Edit-Button pro Zeile
    // ------------------------------------------------------------
    private void setupCategoryCells() {
        listsView.setCellFactory(lv -> new ListCell<>() {

            private final Label name = new Label();
            private final Button btnEdit = new Button("✎");
            private final Region spacer = new Region();
            private final HBox root = new HBox(8, name, spacer, btnEdit);

            {
                root.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(spacer, Priority.ALWAYS);

                btnEdit.getStyleClass().add("category-edit-btn");

                btnEdit.setOnAction(e -> {
                    Category c = getItem();
                    if (c == null)
                        return;
                    showEditCategoryDialog(c);
                });
            }

            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    name.setText(item.getName());
                    setText(null);
                    setGraphic(root);
                }
            }
        });
    }

    private void showEditCategoryDialog(Category category) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Liste bearbeiten");
        dialog.setHeaderText(null);

        ButtonType btnRename = new ButtonType("Umbenennen", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnDelete = new ButtonType("Löschen", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(btnRename, btnDelete, ButtonType.CANCEL);

        TextField txtName = new TextField(category.getName());
        txtName.setPromptText("Listenname");

        HBox content = new HBox(8, new Label("Name:"), txtName);
        content.setAlignment(Pos.CENTER_LEFT);
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() == ButtonType.CANCEL)
            return;

        try {
            if (res.get() == btnRename) {
                String newName = txtName.getText() == null ? "" : txtName.getText().trim();
                if (newName.isEmpty())
                    return;

                int id = category.getId();
                service.renameCategory(id, newName);

                loadCategories();
                reselectCategoryById(id);

                showingDone = false;
                refreshTasks();
            } else if (res.get() == btnDelete) {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                a.setTitle("Liste löschen");
                a.setHeaderText(null);
                a.setContentText("Liste \"" + category.getName() + "\" wirklich löschen?");

                Optional<ButtonType> confirm = a.showAndWait();
                if (confirm.isEmpty() || confirm.get() != ButtonType.OK)
                    return;

                service.deleteCategory(category.getId());

                loadCategories();
                if (!listsView.getItems().isEmpty()) {
                    listsView.getSelectionModel().selectFirst();
                }

                showingDone = false;
                refreshTasks();
            }
        } catch (Exception ex) {
            showError("Bearbeiten fehlgeschlagen: " + ex.getMessage(), ex);
        }
    }

    @FXML
    private void onNewList() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Liste erstellen");
        d.setHeaderText(null);
        d.setContentText("Name:");

        Optional<String> r = d.showAndWait();
        if (r.isEmpty())
            return;

        String name = r.get().trim();
        if (name.isEmpty())
            return;

        try {
            int newId = service.createCategory(name);
            loadCategories();
            reselectCategoryById(newId);

            showingDone = false;
            refreshTasks();
        } catch (Exception ex) {
            showError("Liste konnte nicht erstellt werden: " + ex.getMessage(), ex);
        }
    }

    private void loadCategories() {
        List<Category> cats = service.getCategories();
        listsView.getItems().setAll(cats);
    }

    private void reselectCategoryById(int id) {
        listsView.getItems().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .ifPresent(c -> listsView.getSelectionModel().select(c));
    }

    // ------------------------------------------------------------
    // Todos: Checkbox (DONE <-> OPEN) + Inline Edit (Titel)
    // ------------------------------------------------------------
    private void setupTodoCells() {
        tasksView.setEditable(true);

        tasksView.setCellFactory(lv -> new ListCell<>() {

            private final CheckBox cb = new CheckBox();
            private final Label lbl = new Label();
            private final TextField editor = new TextField();

            private final Region spacer = new Region();
            private final HBox root = new HBox(8, cb, lbl, spacer);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                root.setAlignment(Pos.CENTER_LEFT);
                lbl.setWrapText(true);

                // DONE <-> OPEN
                cb.setOnAction(e -> {
                    TodoItem item = getItem();
                    if (item == null)
                        return;

                    try {
                        if (item.getStatus() == TodoStatus.DONE) {
                            service.markOpen(item.getId());
                        } else {
                            service.markDone(item.getId());
                        }
                        refreshTasks();
                    } catch (Exception ex) {
                        cb.setSelected(item.getStatus() == TodoStatus.DONE);
                        showError("Status konnte nicht geändert werden: " + ex.getMessage(), ex);
                    }
                });

                // Enter speichert
                editor.setOnAction(e -> commitEditTitle());

                // Esc bricht ab
                editor.setOnKeyPressed(e -> {
                    if (e.getCode() == KeyCode.ESCAPE)
                        cancelEdit();
                });

                // Focus lost => speichern
                editor.focusedProperty().addListener((obs, was, is) -> {
                    if (!is && isEditing())
                        commitEditTitle();
                });

                // Doppelklick => edit (nur offene Tasks)
                setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2 && !isEmpty())
                        startEdit();
                });
            }

            @Override
            public void startEdit() {
                TodoItem item = getItem();
                if (item == null)
                    return;

                // DONE nicht editieren
                if (item.getStatus() == TodoStatus.DONE)
                    return;

                super.startEdit();
                editor.setText(item.getTitle());

                root.getChildren().set(1, editor);
                editor.requestFocus();
                editor.selectAll();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                root.getChildren().set(1, lbl);
            }

            @Override
            protected void updateItem(TodoItem item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                cb.setSelected(item.getStatus() == TodoStatus.DONE);

                String due = (item.getDueDate() == null) ? "" : " (" + item.getDueDate() + ")";
                lbl.setText(item.getTitle() + due);

                if (isEditing()) {
                    root.getChildren().set(1, editor);
                } else {
                    root.getChildren().set(1, lbl);
                }

                setText(null);
                setGraphic(root);
            }

            private void commitEditTitle() {
                TodoItem item = getItem();
                if (item == null)
                    return;

                String newTitle = editor.getText() == null ? "" : editor.getText().trim();
                if (newTitle.isEmpty()) {
                    cancelEdit();
                    return;
                }

                try {
                    // DueDate beibehalten (nicht ueberschreiben)
                    LocalDate due = item.getDueDate();
                    service.updateTodo(item.getId(), newTitle, due);

                    super.cancelEdit();
                    refreshTasks();
                } catch (Exception ex) {
                    showError("Todo konnte nicht umbenannt werden: " + ex.getMessage(), ex);
                    cancelEdit();
                }
            }
        });
    }

    @FXML
    private void onAddTask() {
        Category cat = listsView.getSelectionModel().getSelectedItem();
        if (cat == null)
            return;

        String title = txtNewTaskTitle.getText() == null ? "" : txtNewTaskTitle.getText().trim();
        if (title.isEmpty())
            return;

        try {
            service.addTodo(cat.getId(), title, dpNewTaskDueDate.getValue());
            txtNewTaskTitle.clear();
            dpNewTaskDueDate.setValue(null);

            showingDone = false;
            refreshTasks();
        } catch (Exception ex) {
            showError("Todo konnte nicht hinzugefügt werden: " + ex.getMessage(), ex);
        }
    }

    // ------------------------------------------------------------
    // Historie Buttons
    // ------------------------------------------------------------
    @FXML
    private void onShowHistory() {
        showingDone = true;
        refreshTasks();
    }

    @FXML
    private void onBackFromHistory() {
        showingDone = false;
        refreshTasks();
    }

    @FXML
    private void onClearDone() {
        Category cat = listsView.getSelectionModel().getSelectedItem();
        if (cat == null)
            return;

        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Erledigte löschen");
        a.setHeaderText(null);
        a.setContentText("Alle erledigten Aufgaben in dieser Liste löschen?");

        Optional<ButtonType> r = a.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK)
            return;

        try {
            service.deleteDoneTodosByCategory(cat.getId());
            refreshTasks();
        } catch (Exception ex) {
            showError("Erledigte Aufgaben konnten nicht gelöscht werden: " + ex.getMessage(), ex);
        }
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

    // ------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------
    private void refreshTasks() {
        Category cat = listsView.getSelectionModel().getSelectedItem();
        tasksView.getItems().clear();

        if (cat == null) {
            updateHistoryButtons(0);
            return;
        }

        int doneCount = service.getDoneTodosForCategory(cat.getId()).size();

        List<TodoItem> items = showingDone
                ? service.getDoneTodosForCategory(cat.getId())
                : service.getOpenTodosForCategory(cat.getId());

        Comparator<TodoItem> byDueDateAsc = Comparator
                // Tasks ohne DueDate immer nach hinten
                .comparing(TodoItem::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                // Tie-Breaker: Titel
                .thenComparing(TodoItem::getTitle, String.CASE_INSENSITIVE_ORDER);

        items.sort(byDueDateAsc);

        tasksView.getItems().setAll(items);
        updateHistoryButtons(doneCount);

    }

    // ------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------
    private void showError(String msg, Exception ex) {
        ex.printStackTrace();

        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Fehler");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
