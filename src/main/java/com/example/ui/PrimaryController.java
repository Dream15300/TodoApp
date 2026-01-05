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
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.Comparator;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class PrimaryController {

    @FXML // Kennzeichnet Feld wird per fx:id aus FXML gesetzt
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

    private final TodoService service = new TodoService(); // Instanzierung der Service-Schicht
    private boolean showingDone = false; // false = offene Tasks anzeigen, true = erledigte anzeigen

    private static final DateTimeFormatter DUE_FMT = DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN);

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
    private void initialize() { // vom FXMLLoader automatisch nach Injection aufgerufen
        setupCategoryCells(); // Konfiguriert Darstellung/Interaktion der Kategorie-ListView (Custom Cells)
        setupTodoCells(); // Konfiguriert Darstellung/Interaktion der Todo-ListView (Checkbox +
                          // Inline-Edit)

        loadCategories(); // Lädt Kategorien aus Service und setzt sie in listsView

        listsView.getSelectionModel().selectedItemProperty().addListener((observableValue, oldV, newV) -> { // Listener
                                                                                                            // fuer
            // Auswahlwechsel:
            // reagiert, wenn eine
            // andere Kategorie
            // selektiert wird
            showingDone = false;
            refreshTasks();
        });

        if (!listsView.getItems().isEmpty()) { // Falls Liste vorhanden, erste Kategorie automatisch auswählen
            listsView.getSelectionModel().selectFirst();
        }

        refreshTasks();
    }

    // ------------------------------------------------------------
    // Kategorien: Name + Edit-Button pro Zeile
    // ------------------------------------------------------------
    private void setupCategoryCells() { // Setzt eine CellFactory, damit jede Kategoriezeile ein Layout (Name +
                                        // Edit-Button) bekommt
        listsView.setCellFactory(listsView -> new ListCell<>() {

            private final Label name = new Label();
            private final Button btnEdit = new Button("✎");
            private final Region spacer = new Region();
            private final HBox root = new HBox(8, name, spacer, btnEdit);

            {
                root.setAlignment(Pos.CENTER_LEFT); // Box-Ausrichtung
                HBox.setHgrow(spacer, Priority.ALWAYS); // drückt Button nach Rechts

                btnEdit.getStyleClass().add("category-edit-btn");

                btnEdit.setOnAction(e -> { // Edit-Button Aktion
                    Category c = getItem();
                    if (c == null)
                        return;
                    showEditCategoryDialog(c); // Öffnet Dialog zum Bearbeiten der Kategorie
                });
            }

            @Override
            protected void updateItem(Category item, boolean empty) { // Aktualisiert die Darstellung der Zelle
                super.updateItem(item, empty); // Basis-Implementierung aufrufen
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

    private void showEditCategoryDialog(Category category) { // Dialog zum Bearbeiten (Umbenennen/Löschen) einer
                                                             // Kategorie
        Dialog<ButtonType> dialog = new Dialog<>(); // JavaFX-Dialog --> Ergebnis ist ein ButtonType
        dialog.setTitle("Liste bearbeiten");
        dialog.setHeaderText(null);

        ButtonType btnRename = new ButtonType("Umbenennen", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnDelete = new ButtonType("Löschen", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(btnRename, btnDelete, ButtonType.CANCEL);

        TextField txtName = new TextField(category.getName());
        txtName.setPromptText("Listenname"); // Platzhaltertext

        HBox content = new HBox(8, new Label("Name:"), txtName);
        content.setAlignment(Pos.CENTER_LEFT);
        dialog.getDialogPane().setContent(content); // Setzt Inhalt des Dialogs

        Optional<ButtonType> res = dialog.showAndWait();
        if (res.isEmpty() || res.get() == ButtonType.CANCEL)
            return;

        try {
            if (res.get() == btnRename) {
                String newName = txtName.getText() == null ? "" : txtName.getText().trim();
                if (newName.isEmpty())
                    return;

                int id = category.getId(); // Merkt sich die ID vor der Umbenennung
                service.renameCategory(id, newName); // Ruft TodoService zum Umbenennen auf

                loadCategories();
                reselectCategoryById(id);

                showingDone = false;
                refreshTasks();
            } else if (res.get() == btnDelete) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Liste löschen");
                alert.setHeaderText(null);
                alert.setContentText("Liste \"" + category.getName() + "\" wirklich löschen?");

                Optional<ButtonType> confirm = alert.showAndWait();
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
        } catch (Exception exception) {
            showError("Bearbeiten fehlgeschlagen: " + exception.getMessage(), exception);
        }
    }

    @FXML
    private void onNewList() { // Handler für "Neue Liste"-Button
        TextInputDialog textInputDialog = new TextInputDialog();
        textInputDialog.setTitle("Liste erstellen");
        textInputDialog.setHeaderText(null);
        textInputDialog.setContentText("Name:");

        Optional<String> showDialog = textInputDialog.showAndWait();
        if (showDialog.isEmpty())
            return;

        String name = showDialog.get().trim();
        if (name.isEmpty())
            return;

        try {
            int newId = service.createCategory(name); // Ruft TodoService zum Erstellen der Kategorie auf
            loadCategories();
            reselectCategoryById(newId);

            showingDone = false;
            refreshTasks();
        } catch (Exception exception) {
            showError("Liste konnte nicht erstellt werden: " + exception.getMessage(), exception);
        }
    }

    private void loadCategories() { // Lädt Kategorien aus Service und setzt sie in listsView
        List<Category> categories = service.getCategories();
        listsView.getItems().setAll(categories);
    }

    private void reselectCategoryById(int id) { // Selektiert eine Kategorie in der Listenansicht anhand ihrer ID
        listsView.getItems().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .ifPresent(c -> listsView.getSelectionModel().select(c));
    }

    // ------------------------------------------------------------
    // Todos: Checkbox (DONE <-> OPEN) + Inline Edit (Titel)
    // ------------------------------------------------------------
    private void setupTodoCells() { // Setzt eine CellFactory, damit jede Todo-Zeile eine Checkbox + Editierbarkeit
                                    // bekommt
        tasksView.setEditable(true);

        tasksView.setCellFactory(listsView -> new ListCell<>() {

            private final CheckBox checkBox = new CheckBox();
            private final Label title = new Label();
            private final Label due = new Label();
            private final VBox textBox = new VBox(2, title, due);
            private final TextField editor = new TextField();

            private final Region spacer = new Region();
            private final HBox root = new HBox(8, checkBox, textBox, spacer);

            {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                root.setAlignment(Pos.CENTER_LEFT);
                title.setWrapText(true);
                title.getStyleClass().add("todo-title");

                due.getStyleClass().add("todo-due");
                due.setManaged(false);
                due.setVisible(false);

                textBox.getStyleClass().add("todo-textbox");

                // DONE <-> OPEN
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
                        refreshTasks();
                    } catch (Exception exception) {
                        checkBox.setSelected(item.getStatus() == TodoStatus.DONE); // Rückgängig
                        showError("Status konnte nicht geändert werden: " + exception.getMessage(), exception);
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
                editor.focusedProperty().addListener((observableValue, was, is) -> {
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

                root.getChildren().set(1, editor); // Ersetzt an Position 1 (Label) durch TextField
                editor.requestFocus();
                editor.selectAll();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                root.getChildren().set(1, textBox);
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

                title.setText(item.getTitle());
                if (item.getDueDate() != null) {
                    due.setText("📅 " + item.getDueDate().format(DUE_FMT));
                    due.setManaged(true);
                    due.setVisible(true);
                } else {
                    due.setManaged(false);
                    due.setVisible(false);
                }

                if (isEditing()) {
                    root.getChildren().set(1, editor);
                } else {
                    root.getChildren().set(1, textBox);
                }

                setText(null);
                setGraphic(root);
            }

            private void commitEditTitle() { // Speichert den neuen Titel nach dem Editieren in DB
                TodoItem item = getItem();
                if (item == null)
                    return;

                String newTitle = editor.getText() == null ? "" : editor.getText().trim();
                if (newTitle.isEmpty()) {
                    cancelEdit();
                    return;
                }

                try {
                    // DueDate beibehalten (nicht überschreiben)
                    LocalDate dueDate = item.getDueDate();
                    service.updateTodo(item.getId(), newTitle, dueDate);

                    super.cancelEdit();
                    refreshTasks();
                } catch (Exception exception) {
                    showError("Todo konnte nicht umbenannt werden: " + exception.getMessage(), exception);
                    cancelEdit();
                }
            }
        });
    }

    @FXML
    private void onAddTask() { // Handler für "Hinzufügen"-Button
        Category category = listsView.getSelectionModel().getSelectedItem(); // Ausgewählte Kategorie holen
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
            refreshTasks();
        } catch (Exception exception) {
            showError("Todo konnte nicht hinzugefügt werden: " + exception.getMessage(), exception);
        }
    }

    // ------------------------------------------------------------
    // Historie Buttons
    // ------------------------------------------------------------
    @FXML
    private void onShowHistory() { // Wechsel zu erledigten Tasks
        showingDone = true;
        refreshTasks();
    }

    @FXML
    private void onBackFromHistory() { // Wechsel zu offenen Tasks
        showingDone = false;
        refreshTasks();
    }

    @FXML
    private void onClearDone() { // Löschen aller erledigten Tasks der aktuellen Kategorie
        Category category = listsView.getSelectionModel().getSelectedItem();
        if (category == null)
            return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION); // Bestätigungsdialog
        alert.setTitle("Erledigte löschen");
        alert.setHeaderText(null);
        alert.setContentText("Alle erledigten Aufgaben in dieser Liste löschen?");

        Optional<ButtonType> showDialog = alert.showAndWait();
        if (showDialog.isEmpty() || showDialog.get() != ButtonType.OK)
            return;

        try {
            service.deleteDoneTodosByCategory(category.getId()); // Löscht erledigte Tasks via Service
            refreshTasks();
        } catch (Exception exception) {
            showError("Erledigte Aufgaben konnten nicht gelöscht werden: " + exception.getMessage(), exception);
        }
    }

    private void updateHistoryButtons(int doneCount) { // Aktualisiert Sichtbarkeit und Texte der Footer-Buttons
        boolean inHistory = showingDone;

        if (btnShowDone != null) { // "› Erledigt X"
            btnShowDone.setText("› Erledigt " + doneCount);
            btnShowDone.setVisible(!inHistory);
            btnShowDone.setManaged(!inHistory);
        }
        if (btnBack != null) { // "‹ Zurueck"
            btnBack.setVisible(inHistory);
            btnBack.setManaged(inHistory);
        }
        if (btnClearDone != null) { // "Alle erledigten löschen"
            btnClearDone.setVisible(inHistory);
            btnClearDone.setManaged(inHistory);
        }
    }

    // ------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------
    private void refreshTasks() { // Lädt Tasks der selektierten Kategorie (offen/erledigt je nach Zustand)
        Category category = listsView.getSelectionModel().getSelectedItem();
        tasksView.getItems().clear();

        if (category == null) {
            updateHistoryButtons(0);
            return;
        }

        int doneCount = service.getDoneTodosForCategory(category.getId()).size(); // Anzahl erledigter Tasks ermitteln

        List<TodoItem> items = showingDone // Je nach Zustand erledigte/offene Tasks laden
                ? service.getDoneTodosForCategory(category.getId()) // Wenn
                : service.getOpenTodosForCategory(category.getId()); // sonst offene Tasks

        Comparator<TodoItem> byDueDateAsc = Comparator // Sortierlogik definieren
                // Tasks ohne DueDate immer nach hinten, dann aufsteigend nach DueDate
                .comparing(TodoItem::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                // Tie-Breaker: Titel alphabetisch
                .thenComparing(TodoItem::getTitle, String.CASE_INSENSITIVE_ORDER);

        items.sort(byDueDateAsc);

        tasksView.getItems().setAll(items);
        updateHistoryButtons(doneCount);

    }

    // ------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------
    private void showError(String msg, Exception exception) {
        exception.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Fehler");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
