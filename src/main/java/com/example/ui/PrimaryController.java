package com.example.ui;

import com.example.domain.Category;
import com.example.domain.TodoItem;
import com.example.service.TodoService;
import com.example.ui.controller.CategoriesController;
import com.example.ui.controller.DetailsController;
import com.example.ui.controller.NewListPopupController;
import com.example.ui.controller.TasksController;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javafx.geometry.Side;

public class PrimaryController {

    @FXML
    private ListView<Category> listsView;
    @FXML
    private ListView<TodoItem> tasksView;
    @FXML
    private TextField txtNewTaskTitle;
    @FXML
    private DatePicker dpNewTaskDueDate;

    @FXML
    private Label tasksTitleLabel;

    @FXML
    private Button btnShowDone;
    @FXML
    private Button btnBack;
    @FXML
    private Button btnClearDone;

    @FXML
    private VBox detailsPane;
    @FXML
    private TextField detailsTitle;
    @FXML
    private DatePicker detailsDueDate;
    @FXML
    private TextArea detailsNotes;
    @FXML
    private HBox tasksAndDetailsContainer;

    @FXML
    private VBox listsPane;

    @FXML
    private ToggleButton tglTheme;
    private ContextMenu themeMenu;

    private final TodoService service = new TodoService();

    private CategoriesController categoriesController;
    private TasksController tasksController;
    private DetailsController detailsController;
    private NewListPopupController newListPopupController;

    @FXML
    private void initialize() {
        SplitPane.setResizableWithParent(listsPane, false);

        categoriesController = new CategoriesController(listsView, service);
        detailsController = new DetailsController(detailsPane, detailsTitle, detailsDueDate, detailsNotes, service);

        tasksController = new TasksController(
                tasksView,
                txtNewTaskTitle,
                dpNewTaskDueDate,
                btnShowDone,
                btnBack,
                btnClearDone,
                service,
                () -> listsView.getSelectionModel().getSelectedItem());

        newListPopupController = new NewListPopupController(listsView, service, newId -> {
            categoriesController.loadCategories();
            categoriesController.reselectById(newId);

            detailsController.close();
            tasksController.showOpen();
            tasksController.refresh();
        });

        categoriesController.init();
        tasksController.init();
        newListPopupController.init();

        // Initialen Titel setzen
        Category selected = listsView.getSelectionModel().getSelectedItem();
        if (tasksTitleLabel != null) {
            tasksTitleLabel.setText(selected != null ? (iconFor(selected) + selected.getName()) : "Aufgaben");
        }

        // Initialzustand: Details geschlossen
        detailsController.close();

        // Kategorie-Wechsel
        listsView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (tasksTitleLabel != null) {
                tasksTitleLabel.setText(newV != null ? (iconFor(newV) + newV.getName()) : "Aufgaben");
            }
            tasksController.showOpen();
            detailsController.close();
            tasksController.refresh();
        });

        // Task-Selektion -> Details
        tasksController.setOnUserSelection((obs, oldV, newV) -> {
            if (newV == null)
                detailsController.close();
            else
                detailsController.open(newV);
        });

        detailsPane.prefWidthProperty().bind(
                tasksAndDetailsContainer.widthProperty().multiply(0.5));

        tasksView.prefWidthProperty().bind(
                tasksAndDetailsContainer.widthProperty().multiply(0.5));

        tasksController.refresh();

        if (tglTheme != null) {
            setupThemeMenu();
        }
    }

    // ------- Icon für Kategorien-Titel in der rechten Spalte -------

    private String iconFor(Category c) {
        if (c == null)
            return "";

        // 1) Icon aus DB bevorzugen
        String dbIcon = c.getIcon();
        if (dbIcon != null && !dbIcon.isBlank()) {
            return dbIcon.trim() + " ";
        }

        // 2) Fallback (falls alte Kategorien noch kein Icon haben)
        return switch (c.getName()) {
            case "Arbeit" -> "💼 ";
            case "Schule" -> "🎓 ";
            case "Privat" -> "🏠 ";
            default -> "📁 ";
        };
    }

    // ------- FXML Actions: delegieren -------

    @FXML
    private void onNewList() {
        newListPopupController.toggleShowCentered();
    }

    @FXML
    private void onAddTask() {
        detailsController.close();
        tasksController.onAddTask();
    }

    @FXML
    private void onShowHistory() {
        tasksController.showDone();
        detailsController.close();
        tasksController.refresh();
    }

    @FXML
    private void onBackFromHistory() {
        tasksController.showOpen();
        detailsController.close();
        tasksController.refresh();
    }

    @FXML
    private void onClearDone() {
        detailsController.close();
        tasksController.onClearDone();
    }

    @FXML
    private void onCloseDetails() {
        detailsController.close();
        tasksController.clearSelectionProgrammatically();
    }

    @FXML
    private void onClearDetailsDueDate() {
        detailsController.clearDueDate();
    }

    @FXML
    private void onSaveDetails() {
        boolean ok = detailsController.save();
        if (ok) {
            detailsController.close();
            tasksController.refresh();
            tasksController.clearSelectionProgrammatically();
            tasksView.requestFocus(); // optional: Fokus in Liste statt im Details-Panel

        }
    }

    // ------- Theme Menu -------

    private void setupThemeMenu() {
        themeMenu = new ContextMenu();
        themeMenu.getStyleClass().add("theme-menu"); // CSS Hook

        ToggleGroup group = new ToggleGroup();

        // MenuItems fuer alle Themes aus ThemeManager
        for (ThemeManager.Theme t : ThemeManager.Theme.values()) {
            RadioMenuItem item = new RadioMenuItem(prettyThemeName(t));
            item.setToggleGroup(group);
            item.setUserData(t);

            item.setOnAction(e -> {
                ThemeManager.Theme selected = (ThemeManager.Theme) item.getUserData();
                if (tglTheme.getScene() != null) {
                    ThemeManager.apply(tglTheme.getScene(), selected);
                    ThemeManager.saveTheme(selected);
                    setThemeButtonLabel(selected);
                }
                themeMenu.hide();
            });

            themeMenu.getItems().add(item);
        }

        // Aktuelles Theme selektieren
        ThemeManager.Theme current = ThemeManager.loadThemeOrDefault();
        for (var mi : themeMenu.getItems()) {
            if (mi instanceof RadioMenuItem rmi && rmi.getUserData() == current) {
                rmi.setSelected(true);
                break;
            }
        }

        // Button-Label setzen
        setThemeButtonLabel(current);

        // ToggleButton als "Menu-Button" verwenden (kein Toggle-Status noetig)
        tglTheme.setSelected(false);

        // Klick-Handler: Popup oeffnen/schliessen
        tglTheme.setOnAction(e -> {
            if (themeMenu.isShowing()) {
                themeMenu.hide();
            } else {
                // neben Button anzeigen
                themeMenu.show(tglTheme, Side.BOTTOM, 0, 6);
            }
            tglTheme.setSelected(false); // verhindert "eingedrueckt"
        });

        // Wenn Popup schliesst: Toggle reset
        themeMenu.setOnHidden(e -> tglTheme.setSelected(false));
    }

    private void setThemeButtonLabel(ThemeManager.Theme theme) {
        // Kurzer, typischer Label-Text
        tglTheme.setText(prettyThemeName(theme));
    }

    private String prettyThemeName(ThemeManager.Theme t) {
        return switch (t) {
            case LIGHT -> "Light";
            case DIM -> "Dim";
            case BLUE -> "Blue";
            case GREEN -> "Green";
            case PURPLE -> "Purple";
            case HIGH_CONTRAST -> "High Contrast";
        };
    }

}
