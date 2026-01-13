package com.example.ui;

import com.example.domain.Category;
import com.example.domain.TodoItem;
import com.example.service.TodoService;
import com.example.ui.controller.*;

import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
    private SplitPane rootSplit;

    @FXML
    private ToggleButton tglTheme;
    private ContextMenu themeMenu;

    @FXML
    private Button btnListMenu;

    private final TodoService service = new TodoService();

    private CategoriesController categoriesController;
    private TasksController tasksController;
    private DetailsController detailsController;
    private NewListPopupController newListPopupController;

    private PrimaryLayoutController layout;
    private PrimaryDetailsSizingController sizing;
    private PrimaryListMenuController listMenuCtl;

    private static final double COMPACT_BREAKPOINT = 640;

    @FXML
    private void initialize() {
        categoriesController = new CategoriesController(listsView, service);
        detailsController = new DetailsController(detailsPane, detailsTitle, detailsDueDate, detailsNotes, service);

        tasksController = new TasksController(
                tasksView, txtNewTaskTitle, dpNewTaskDueDate,
                btnShowDone, btnBack, btnClearDone,
                service, () -> listsView.getSelectionModel().getSelectedItem());

        newListPopupController = new NewListPopupController(listsView, service, newId -> {
            categoriesController.loadCategories();
            categoriesController.reselectById(newId);

            detailsController.close();
            sizing.apply();

            tasksController.showOpen();
            tasksController.refresh();
            updateHeaderTexts();

            if (listMenuCtl != null && listMenuCtl.isShowing())
                listMenuCtl.rebuild();
        });

        categoriesController.init();
        tasksController.init();
        newListPopupController.init();

        layout = new PrimaryLayoutController(listsPane, rootSplit, tasksTitleLabel, btnListMenu, COMPACT_BREAKPOINT);
        layout.init();

        sizing = new PrimaryDetailsSizingController(tasksAndDetailsContainer, tasksView, detailsPane);

        listMenuCtl = new PrimaryListMenuController(
                btnListMenu,
                listsView,
                categoriesController,
                tasksView,
                () -> "",
                this::iconFor,
                this::onNewList);
        listMenuCtl.init();

        detailsController.close();
        sizing.apply();
        updateHeaderTexts();

        listsView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            updateHeaderTexts();
            tasksController.showOpen();
            detailsController.close();
            sizing.apply();
            tasksController.refresh();
            if (listMenuCtl.isShowing())
                listMenuCtl.rebuild();
        });

        tasksController.setOnUserSelection((obs, oldV, newV) -> {
            if (newV == null)
                detailsController.close();
            else
                detailsController.open(newV);
            sizing.apply();
        });

        tasksController.refresh();

        if (tglTheme != null)
            setupThemeMenu();
    }

    private void updateHeaderTexts() {
        Category selected = listsView.getSelectionModel().getSelectedItem();
        String title = selected != null ? (iconFor(selected) + selected.getName()) : "Aufgaben";
        layout.updateHeaderTexts(selected, title);
    }

    private String iconFor(Category c) {
        if (c == null)
            return "";
        String dbIcon = c.getIcon();
        if (dbIcon != null && !dbIcon.isBlank())
            return dbIcon.trim() + " ";
        return switch (c.getName()) {
            case "Arbeit" -> "💼 ";
            case "Schule" -> "🎓 ";
            case "Privat" -> "🏠 ";
            default -> "📁 ";
        };
    }

    @FXML
    private void onNewList() {
        newListPopupController.toggleShowCentered();
    }

    @FXML
    private void onAddTask() {
        detailsController.close();
        sizing.apply();
        tasksController.onAddTask();
    }

    @FXML
    private void onShowHistory() {
        tasksController.showDone();
        detailsController.close();
        sizing.apply();
        tasksController.refresh();
    }

    @FXML
    private void onBackFromHistory() {
        tasksController.showOpen();
        detailsController.close();
        sizing.apply();
        tasksController.refresh();
    }

    @FXML
    private void onClearDone() {
        detailsController.close();
        sizing.apply();
        tasksController.onClearDone();
    }

    @FXML
    private void onCloseDetails() {
        detailsController.close();
        sizing.apply();
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
            sizing.apply();
            tasksController.refresh();
            tasksController.clearSelectionProgrammatically();
            tasksView.requestFocus();
        }
    }

    private void setupThemeMenu() {
        themeMenu = new ContextMenu();
        themeMenu.getStyleClass().add("theme-menu");

        ToggleGroup group = new ToggleGroup();
        for (ThemeManager.Theme t : ThemeManager.Theme.values()) {
            RadioMenuItem item = new RadioMenuItem(prettyThemeName(t));
            item.setToggleGroup(group);
            item.setUserData(t);
            item.setOnAction(e -> {
                ThemeManager.Theme selected = (ThemeManager.Theme) item.getUserData();
                if (tglTheme.getScene() != null) {
                    ThemeManager.apply(tglTheme.getScene(), selected);
                    ThemeManager.saveTheme(selected);
                    tglTheme.setText(prettyThemeName(selected));
                }
                themeMenu.hide();
            });
            themeMenu.getItems().add(item);
        }

        ThemeManager.Theme current = ThemeManager.loadThemeOrDefault();
        for (var mi : themeMenu.getItems()) {
            if (mi instanceof RadioMenuItem rmi && rmi.getUserData() == current) {
                rmi.setSelected(true);
                break;
            }
        }

        tglTheme.setText(prettyThemeName(current));
        tglTheme.setSelected(false);

        tglTheme.setOnAction(e -> {
            if (themeMenu.isShowing())
                themeMenu.hide();
            else
                themeMenu.show(tglTheme, Side.BOTTOM, 0, 6);
            tglTheme.setSelected(false);
        });

        themeMenu.setOnHidden(e -> tglTheme.setSelected(false));
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
