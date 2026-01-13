package com.example.ui;

import com.example.domain.Category;
import com.example.domain.TodoItem;
import com.example.service.TodoService;
import com.example.ui.controller.CategoriesController;
import com.example.ui.controller.DetailsController;
import com.example.ui.controller.NewListPopupController;
import com.example.ui.controller.TasksController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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

    // Compact UI: Dropdown fuer Listen
    @FXML
    private Button btnListMenu;
    private ContextMenu listMenu;

    private final TodoService service = new TodoService();

    private CategoriesController categoriesController;
    private TasksController tasksController;
    private DetailsController detailsController;
    private NewListPopupController newListPopupController;

    private static final double COMPACT_BREAKPOINT = 640;
    private boolean compactMode = false;

    // Originalbreiten (aus FXML)
    private double listsPrefW;
    private double listsMinW;
    private double listsMaxW;

    // SplitPane Divider restore
    private double splitDividerPos = 0.28;

    @FXML
    private void initialize() {
        SplitPane.setResizableWithParent(listsPane, false);

        // Originalbreiten merken (aus FXML)
        listsPrefW = listsPane.getPrefWidth();
        listsMinW = listsPane.getMinWidth();
        listsMaxW = listsPane.getMaxWidth();

        if (rootSplit != null && rootSplit.getDividers().size() > 0) {
            splitDividerPos = rootSplit.getDividerPositions()[0];
        }

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
            applyDetailsWidthBinding();

            tasksController.showOpen();
            tasksController.refresh();
            updateHeaderTexts();

            // wichtig: Dropdown neu aufbauen (neue Liste sichtbar)
            if (listMenu != null && listMenu.isShowing()) {
                rebuildListMenuItems();
            }
        });

        categoriesController.init();
        tasksController.init();
        newListPopupController.init();

        setupListMenu();

        // Initialzustand
        detailsController.close();
        applyDetailsWidthBinding();
        updateHeaderTexts();

        // Kategorie-Wechsel
        listsView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            updateHeaderTexts();
            tasksController.showOpen();
            detailsController.close();
            applyDetailsWidthBinding();
            tasksController.refresh();

            if (listMenu != null && listMenu.isShowing()) {
                rebuildListMenuItems();
            }
        });

        // Task-Selektion -> Details
        tasksController.setOnUserSelection((obs, oldV, newV) -> {
            if (newV == null)
                detailsController.close();
            else
                detailsController.open(newV);
            applyDetailsWidthBinding();
        });

        tasksController.refresh();

        if (tglTheme != null) {
            setupThemeMenu();
        }

        // Responsive erst, wenn Scene existiert
        Platform.runLater(() -> {
            var scene = listsPane.getScene();
            if (scene == null)
                return;

            applyCompactMode(scene.getWidth());
            scene.widthProperty().addListener((o, oldW, newW) -> applyCompactMode(newW.doubleValue()));
        });
    }

    private void applyCompactMode(double width) {
        boolean shouldCompact = width < COMPACT_BREAKPOINT;
        if (shouldCompact == compactMode)
            return;
        compactMode = shouldCompact;

        // Links ein/aus
        listsPane.setVisible(!compactMode);
        listsPane.setManaged(!compactMode);

        if (!compactMode) {
            listsPane.setPrefWidth(listsPrefW);
            listsPane.setMinWidth(listsMinW);
            listsPane.setMaxWidth(listsMaxW);
        } else {
            listsPane.setPrefWidth(0);
            listsPane.setMinWidth(0);
            listsPane.setMaxWidth(0);
        }

        // SplitPane Divider/linie entfernen in compact
        if (rootSplit != null) {
            if (compactMode) {
                rootSplit.setDividerPositions(0.0);
                if (!rootSplit.getStyleClass().contains("compact")) {
                    rootSplit.getStyleClass().add("compact");
                }
            } else {
                rootSplit.setDividerPositions(splitDividerPos);
                rootSplit.getStyleClass().remove("compact");
            }
        }

        // Header: in compact verschwindet Label, Button zeigt aktuelle Liste
        if (tasksTitleLabel != null) {
            tasksTitleLabel.setVisible(!compactMode);
            tasksTitleLabel.setManaged(!compactMode);
        }
        if (btnListMenu != null) {
            btnListMenu.setVisible(compactMode);
            btnListMenu.setManaged(compactMode);
        }

        if (!compactMode && listMenu != null) {
            listMenu.hide();
        }

        updateHeaderTexts();
    }

    private void updateHeaderTexts() {
        Category selected = listsView.getSelectionModel().getSelectedItem();
        String title = selected != null ? (iconFor(selected) + selected.getName()) : "Aufgaben";

        if (tasksTitleLabel != null)
            tasksTitleLabel.setText(title);
        if (btnListMenu != null)
            btnListMenu.setText(title);
    }

    /**
     * - Details offen: Tasks 50%, Details 50%
     * - Details zu: Tasks 100%
     */
    private void applyDetailsWidthBinding() {
        if (tasksAndDetailsContainer == null || tasksView == null || detailsPane == null)
            return;

        boolean detailsOpen = detailsPane.isManaged() && detailsPane.isVisible();

        if (detailsOpen) {
            if (!detailsPane.prefWidthProperty().isBound()) {
                detailsPane.prefWidthProperty().bind(tasksAndDetailsContainer.widthProperty().multiply(0.5));
            }
            if (!tasksView.prefWidthProperty().isBound()) {
                tasksView.prefWidthProperty().bind(tasksAndDetailsContainer.widthProperty().multiply(0.5));
            }
        } else {
            if (detailsPane.prefWidthProperty().isBound())
                detailsPane.prefWidthProperty().unbind();
            if (tasksView.prefWidthProperty().isBound())
                tasksView.prefWidthProperty().unbind();

            tasksView.setPrefWidth(Region.USE_COMPUTED_SIZE);
            detailsPane.setPrefWidth(Region.USE_COMPUTED_SIZE);
        }
    }

    // ---------- Listen-Dropdown (compact) ----------

    private void setupListMenu() {
        if (btnListMenu == null)
            return;

        listMenu = new ContextMenu();
        listMenu.getStyleClass().add("theme-menu");

        btnListMenu.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY)
                return;

            if (listMenu.isShowing()) {
                listMenu.hide();
            } else {
                rebuildListMenuItems();
                listMenu.show(btnListMenu, Side.BOTTOM, 0, 6);
            }
        });
    }

    private void rebuildListMenuItems() {
        if (listMenu == null)
            return;

        listMenu.getItems().clear();

        Category selected = listsView.getSelectionModel().getSelectedItem();

        // damit Edit-Buttons rechtsbuendig sind: fixe Menuebreite
        final double MENU_W = 260;
        final double EDIT_W = 36;

        for (Category c : listsView.getItems()) {

            Label lbl = new Label(iconFor(c) + c.getName());
            lbl.setMinWidth(0);
            lbl.setMaxWidth(Double.MAX_VALUE);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnEdit = new Button("⋯");
            btnEdit.getStyleClass().add("category-edit-btn");
            btnEdit.setFocusTraversable(false);
            btnEdit.setMinWidth(EDIT_W);
            btnEdit.setPrefWidth(EDIT_W);
            btnEdit.setMaxWidth(EDIT_W);

            HBox row = new HBox(10, lbl, spacer, btnEdit);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPrefWidth(MENU_W);
            row.setMinWidth(MENU_W);
            row.setMaxWidth(MENU_W);

            // optische Markierung (optional, falls CSS genutzt)
            row.getStyleClass().add("list-menu-row");
            if (selected != null && c.getId() == selected.getId()) {
                row.getStyleClass().add("list-menu-row-selected");
            }

            // Klick auf Zeile (nicht auf Edit) -> selektieren
            row.setOnMouseClicked(ev -> {
                if (ev.getButton() != MouseButton.PRIMARY)
                    return;
                // wenn Edit geklickt -> ignorieren
                if (isInParentChain((javafx.scene.Node) ev.getTarget(), btnEdit))
                    return;

                listsView.getSelectionModel().select(c);
                listMenu.hide();
            });

            // Edit -> Menu schliessen, Edit-Popup oeffnen (stabiler Anchor!)
            btnEdit.setOnAction(ev -> {
                ev.consume();
                listMenu.hide();
                categoriesController.showEditFor(c, tasksView); // stabiler Anchor, nicht Menu-Button
            });

            CustomMenuItem item = new CustomMenuItem(row, false);
            item.setHideOnClick(false); // wichtig: Button bleibt klickbar
            item.setMnemonicParsing(false);

            listMenu.getItems().add(item);
        }

        listMenu.getItems().add(new SeparatorMenuItem());

        MenuItem add = new MenuItem("+ Neue Liste");
        add.setOnAction(e -> {
            listMenu.hide();
            onNewList();
        });
        listMenu.getItems().add(add);
    }

    private boolean isInParentChain(javafx.scene.Node target, javafx.scene.Node expected) {
        javafx.scene.Node n = target;
        while (n != null) {
            if (n == expected)
                return true;
            n = n.getParent();
        }
        return false;
    }

    // ------- Icon fuer Kategorien-Titel -------

    private String iconFor(Category c) {
        if (c == null)
            return "";

        String dbIcon = c.getIcon();
        if (dbIcon != null && !dbIcon.isBlank()) {
            return dbIcon.trim() + " ";
        }

        return switch (c.getName()) {
            case "Arbeit" -> "💼 ";
            case "Schule" -> "🎓 ";
            case "Privat" -> "🏠 ";
            default -> "📁 ";
        };
    }

    // ------- FXML Actions -------

    @FXML
    private void onNewList() {
        newListPopupController.toggleShowCentered();
    }

    @FXML
    private void onAddTask() {
        detailsController.close();
        applyDetailsWidthBinding();
        tasksController.onAddTask();
    }

    @FXML
    private void onShowHistory() {
        tasksController.showDone();
        detailsController.close();
        applyDetailsWidthBinding();
        tasksController.refresh();
    }

    @FXML
    private void onBackFromHistory() {
        tasksController.showOpen();
        detailsController.close();
        applyDetailsWidthBinding();
        tasksController.refresh();
    }

    @FXML
    private void onClearDone() {
        detailsController.close();
        applyDetailsWidthBinding();
        tasksController.onClearDone();
    }

    @FXML
    private void onCloseDetails() {
        detailsController.close();
        applyDetailsWidthBinding();
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
            applyDetailsWidthBinding();
            tasksController.refresh();
            tasksController.clearSelectionProgrammatically();
            tasksView.requestFocus();
        }
    }

    // ------- Theme Menu -------

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
                    setThemeButtonLabel(selected);
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

        setThemeButtonLabel(current);
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

    private void setThemeButtonLabel(ThemeManager.Theme theme) {
        tglTheme.setText(prettyThemeName(theme));
    }

    private String prettyThemeName(ThemeManager.Theme thema) {
        return switch (thema) {
            case LIGHT -> "Light";
            case DIM -> "Dim";
            case BLUE -> "Blue";
            case GREEN -> "Green";
            case PURPLE -> "Purple";
            case HIGH_CONTRAST -> "High Contrast";
        };
    }
}
