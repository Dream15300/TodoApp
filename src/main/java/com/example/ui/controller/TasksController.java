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
import javafx.concurrent.Task;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Verantwortlichkeiten:
 * - Laden/Anzeige von offenen oder erledigten Todos (showingDone)
 * - Hinzufügen neuer Todos (Titel + optional DueDate)
 * - Umschalten Status (Checkbox)
 * - History-UI steuern ("Erledigt", "Zurück", "Alle löschen")
 * - stabiler Refresh inkl. Wiederherstellung der Selektion (per ID)
 *
 * Technische Schwerpunkte:
 * - ListCell-Rendering mit WrapText und dynamischen Elementen
 * (DueDate/NotesIcon)
 * - suppressSelection verhindert Side-Effects bei programatischer Selektion
 * (Details öffnen)
 */
public class TasksController {

    private final ListView<TodoItem> tasksView;
    private final TextField txtNewTaskTitle;
    private final DatePicker dpNewTaskDueDate;

    private final Button btnShowDone;
    private final Button btnBack;
    private final Button btnClearDone;

    private final TodoService service;

    /*
     * Supplier statt direkter Referenz:
     * - entkoppelt TasksController vom "State Holder" (z. B. PrimaryController),
     * - liefert immer aktuelle Selektion.
     */
    private final Supplier<Category> selectedCategorySupplier;

    // Datumsformat für DueDate-Anzeige (Deutsch)
    private final DateTimeFormatter dueFmt = DateTimeFormatter.ofPattern("EEE, d. MMM", Locale.GERMAN);

    // false = offene Todos, true = erledigte Todos
    private boolean showingDone = false;

    // Bestätigungs-Popup für "alle erledigten löschen"
    private ConfirmPopupController deleteDoneConfirmPopup;

    // verhindert, dass programatische Selektion (Refresh) Details öffnet
    private boolean suppressSelection = false;

    // Token für nebenläufige Refreshes (nur letzter Effektiv)
    private volatile long refreshToken = 0;

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

    /**
     * Initialisiert CellFactory und Popup-Controller.
     *
     * Hinweis:
     * - deleteDoneConfirmPopup wird hier bereits erstellt; zusätzlich gibt es
     * später
     * noch einen defensiven Null-Check in onClearDone().
     */
    public void init() {
        setupTodoCells();

        deleteDoneConfirmPopup = new ConfirmPopupController(tasksView);
        deleteDoneConfirmPopup.init();
    }

    /**
     * Registriert einen Listener, der nur auf echte User-Selektion reagiert.
     *
     * Umsetzung:
     * - Listener wird immer an selectedItemProperty gehängt
     * - suppressSelection unterdrückt Callbacks während
     * refresh()/clearSelectionProgrammatically()
     */
    public void setOnUserSelection(javafx.beans.value.ChangeListener<TodoItem> listener) {
        tasksView.getSelectionModel().selectedItemProperty().addListener((observableValue, oldV, newV) -> {
            if (suppressSelection)
                return;
            listener.changed(observableValue, oldV, newV);
        });
    }

    /**
     * Löscht Selektion ohne Nebenwirkungen (kein Details-open).
     */
    public void clearSelectionProgrammatically() {
        suppressSelection = true;
        try {
            tasksView.getSelectionModel().clearSelection();
        } finally {
            suppressSelection = false;
        }
    }

    /**
     * Setzt View auf offene Todos.
     * Hinweis: UI wird erst mit refresh() aktualisiert.
     */
    public void showOpen() {
        showingDone = false;
    }

    /**
     * Setzt View auf erledigte Todos.
     * Hinweis: UI wird erst mit refresh() aktualisiert.
     */
    public void showDone() {
        showingDone = true;
    }

    public boolean isShowingDone() {
        return showingDone;
    }

    /**
     * Lädt Todos neu basierend auf ausgewählter Kategorie und showingDone.
     *
     * Wichtige Punkte:
     * - merkt aktuelle Selektion per ID (weil neue Instanzen geladen werden)
     * - suppressSelection verhindert Details-Trigger beim select(...) nach Refresh
     * - aktualisiert History-Buttons anhand doneCount
     */
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

        final int categoryId = category.getId();
        final boolean loadDone = showingDone;
        final Integer keepSelectedId = selectedId;

        // Token: nur letzter Refresh darf UI setzen
        final long token = ++refreshToken;

        Task<RefreshResult> task = new Task<>() {
            @Override
            protected RefreshResult call() {
                int doneCount = service.countDoneTodosForCategory(categoryId);

                List<TodoItem> items = loadDone
                        ? service.getDoneTodosForCategory(categoryId)
                        : service.getOpenTodosForCategory(categoryId);

                return new RefreshResult(doneCount, items);
            }
        };

        task.setOnSucceeded(e -> {
            if (token != refreshToken) {
                return; // veraltet
            }

            RefreshResult result = task.getValue();

            suppressSelection = true;
            try {
                tasksView.getItems().setAll(result.items);

                // Selektion wiederherstellen (per ID)
                if (keepSelectedId != null) {
                    int idx = indexOfId(result.items, keepSelectedId);
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

            updateHistoryButtons(result.doneCount);
        });

        task.setOnFailed(e -> {
            if (token != refreshToken) {
                return;
            }
            Throwable ex = task.getException();
            UiDialogs.error(
                    "Todos laden fehlgeschlagen: " + (ex == null ? "" : ex.getMessage()),
                    ex instanceof Exception ? (Exception) ex : new Exception(ex));
        });

        Thread t = new Thread(task, "load-todos");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Hilfsfunktion: Index eines TodoItem anhand ID in einer Liste.
     *
     * Laufzeit:
     * - O(n) pro Refresh; bei typischer Todo-Listen-Grösse ok.
     */
    private int indexOfId(List<TodoItem> items, int id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == id)
                return i;
        }
        return -1;
    }

    /**
     * Handler: neues Todo hinzufügen.
     *
     * Validierung:
     * - Titel darf nicht leer sein
     *
     * Ablauf:
     * - service.addTodo(...)
     * - Eingabefelder zurücksetzen
     * - showingDone=false (nach Insert wieder in offene Ansicht)
     * - refresh()
     */
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

    /**
     * Handler: "Alle erledigten löschen" (nur im History-Mode sichtbar).
     *
     * Ablauf:
     * - Bestätigungs-Popup anzeigen
     * - bei Confirm: service.deleteDoneTodosByCategory(...)
     * - refresh()
     */
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

    /**
     * Steuert Sichtbarkeit/Managed der History-Buttons.
     *
     * Logik:
     * - inHistory = showingDone
     * - btnShowDone: nur sichtbar wenn NICHT inHistory; Text enthält doneCount
     * - btnBack + btnClearDone: nur sichtbar wenn inHistory
     */
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

    /**
     * Konfiguriert die ListCell für TodoItems.
     *
     * Aufbau:
     * - CheckBox links (Status)
     * - VBox: Title (wrap) + optional Due-Date
     * - Spacer
     * - Notes-Icon "✎" nur sichtbar, wenn Notes vorhanden
     *
     * Wichtige Cell-Reuse Punkte:
     * - Due-Style "overdue" immer resetten, bevor neu gesetzt wird
     * - managed/visible für Due und NotesIcon jedes Mal korrekt setzen
     */
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
                /*
                 * Breite der ListCell an ListView koppeln:
                 * - setPrefWidth(0) + binding auf lv.widthProperty()-18 wirkt wie ein
                 * "fit-to-width"
                 */
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

                /*
                 * Statuswechsel:
                 * - Checkbox toggelt DONE/OPEN
                 * - refresh() lädt Liste neu und setzt Checkbox-Status konsistent
                 *
                 * Hinweis:
                 * - Bei refresh() wird die Liste neu gesetzt; ListCell wird recycelt.
                 */
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
                        // UI zurücksetzen auf tatsächlichen Status
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

                /*
                 * TodoUiText.breakAnywhere:
                 * - verhindert Layout-Probleme bei sehr langen Wörtern/Strings ohne Leerzeichen
                 * - typischer Ansatz: Zero-Width-Spaces oder Soft-Hyphen einfügen
                 */
                title.setText(TodoUiText.breakAnywhere(item.getTitle()));

                // Reset (wichtig wegen Cell-Reuse)
                due.getStyleClass().remove("overdue");

                if (item.getDueDate() != null) {
                    due.setText("📅 " + item.getDueDate().format(dueFmt));
                    due.setManaged(true);
                    due.setVisible(true);

                    boolean isDone = item.getStatus() == TodoStatus.DONE;
                    boolean isOverdue = !isDone && item.getDueDate().isBefore(java.time.LocalDate.now());

                    // Overdue nur bei offenen Tasks (nicht bei erledigten)
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

    private static final class RefreshResult {
        final int doneCount;
        final List<TodoItem> items;

        RefreshResult(int doneCount, List<TodoItem> items) {
            this.doneCount = doneCount;
            this.items = items;
        }
    }

}
