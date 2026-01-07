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
    private VBox listsPane;

    @FXML
    private ToggleButton tglTheme;

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

        // Initialzustand: Details geschlossen
        detailsController.close();

        // Kategorie-Wechsel
        listsView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
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

        tasksController.refresh();

        if (tglTheme != null) {
            ThemeManager.Theme saved = ThemeManager.loadThemeOrDefault();
            boolean dim = (saved == ThemeManager.Theme.DIM);

            tglTheme.setSelected(dim);
            tglTheme.setText(dim ? "Dim" : "Light");
        }
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
        }
    }

    @FXML
    private void onToggleTheme() {
        if (tglTheme == null)
            return;
        if (tglTheme.getScene() == null)
            return;

        boolean dim = tglTheme.isSelected();
        ThemeManager.Theme theme = dim ? ThemeManager.Theme.DIM : ThemeManager.Theme.LIGHT;

        ThemeManager.apply(tglTheme.getScene(), theme);
        tglTheme.getScene().getRoot().applyCss();
        tglTheme.getScene().getRoot().layout();
        ThemeManager.saveTheme(theme);

        tglTheme.setText(dim ? "Dim" : "Light");
    }
}
