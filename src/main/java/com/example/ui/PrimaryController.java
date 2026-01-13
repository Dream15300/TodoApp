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

/**
 * Verantwortlichkeiten:
 * - Initialisierung der Subcontroller (CategoriesController, TasksController,
 * DetailsController, Popups)
 * - Verdrahtung der UI-Events (Buttons/Selection/Theme)
 * - Koordination zwischen Bereichen (Listenwechsel → Tasks refresh, Details
 * schliessen, Header aktualisieren)
 *
 * Architektur:
 * - UI wird in kleinere Controller zerlegt; PrimaryController ist der
 * Orchestrator.
 * - TodoService wird als zentrale Service-Instanz hier gehalten und
 * weitergereicht.
 */
public class PrimaryController {

    // ===== FXML References (Categories + Tasks) =====
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

    // ===== FXML References (History Buttons) =====
    @FXML
    private Button btnShowDone;
    @FXML
    private Button btnBack;
    @FXML
    private Button btnClearDone;

    // ===== FXML References (Details Pane) =====
    @FXML
    private VBox detailsPane;
    @FXML
    private TextField detailsTitle;
    @FXML
    private DatePicker detailsDueDate;
    @FXML
    private TextArea detailsNotes;

    // ===== Layout References =====
    @FXML
    private HBox tasksAndDetailsContainer;
    @FXML
    private VBox listsPane;
    @FXML
    private SplitPane rootSplit;

    // ===== Theme UI =====
    @FXML
    private ToggleButton tglTheme;
    private ContextMenu themeMenu;

    // ===== Compact Mode Header Button =====
    @FXML
    private Button btnListMenu;

    /*
     * Service-Instanz (Shared State):
     * - Wird von allen Subcontrollern genutzt.
     */
    private final TodoService service = new TodoService();

    // ===== Subcontroller =====
    private CategoriesController categoriesController;
    private TasksController tasksController;
    private DetailsController detailsController;
    private NewListPopupController newListPopupController;

    private PrimaryLayoutController layout;
    private PrimaryDetailsSizingController sizing;
    private PrimaryListMenuController listMenuCtl;

    // Responsive Breakpoint für Compact Mode
    private static final double COMPACT_BREAKPOINT = 640;

    /**
     * FXML initialize(): wird von JavaFX nach dem Laden des FXML aufgerufen.
     *
     * Ablauf (high-level):
     * - Subcontroller erstellen und init() aufrufen
     * - Responsive-Layout Controller initialisieren
     * - Listener für Selektion und Buttons registrieren
     * - Initialzustand setzen (Details zu, Sizing anwenden, Header setzen, Tasks
     * refresh)
     */
    @FXML
    private void initialize() {
        // Controller für Kategorienliste (links)
        categoriesController = new CategoriesController(listsView, service);

        // Controller für Detailansicht (rechts)
        detailsController = new DetailsController(detailsPane, detailsTitle, detailsDueDate, detailsNotes, service);

        // Controller für Taskliste (mittig); ausgewählte Kategorie kommt über Supplier
        // aus listsView
        tasksController = new TasksController(
                tasksView, txtNewTaskTitle, dpNewTaskDueDate,
                btnShowDone, btnBack, btnClearDone,
                service, () -> listsView.getSelectionModel().getSelectedItem());

        /*
         * Popup: Neue Liste
         * Callback-Flow nach Erstellung:
         * - Kategorien reload + neue selektieren
         * - Details schliessen + Sizing anwenden
         * - Tasks auf "open" setzen + refresh
         * - Header aktualisieren
         * - Falls compact listMenu offen: rebuild
         */
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

        // Responsive Layout (Compact Mode)
        layout = new PrimaryLayoutController(listsPane, rootSplit, tasksTitleLabel, btnListMenu, COMPACT_BREAKPOINT);
        layout.init();

        // Sizing zwischen tasksView und detailsPane (50/50 wenn Details offen)
        sizing = new PrimaryDetailsSizingController(tasksAndDetailsContainer, tasksView, detailsPane);

        /*
         * Compact List Menu:
         * - stableAnchorForEditPopup = tasksView (konstant sichtbar)
         * - iconForSelectedDummy wird übergeben, wird im Controller aktuell nicht
         * genutzt
         * - iconForCategory ist iconFor(...)
         * - onNewList ist PrimaryController.onNewList (öffnet NewListPopupController)
         */
        listMenuCtl = new PrimaryListMenuController(
                btnListMenu,
                listsView,
                categoriesController,
                tasksView,
                () -> "",
                this::iconFor,
                this::onNewList);
        listMenuCtl.init();

        // Initialzustand: Details geschlossen
        detailsController.close();
        sizing.apply();
        updateHeaderTexts();

        /*
         * Kategorie-Selektion:
         * - Header aktualisieren
         * - Tasks zurück auf Open-Ansicht
         * - Details schliessen + Sizing anwenden
         * - Tasks refresh
         * - Falls compact Menü offen: rebuild (Selection-Highlight)
         */
        listsView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            updateHeaderTexts();
            tasksController.showOpen();
            detailsController.close();
            sizing.apply();
            tasksController.refresh();
            if (listMenuCtl.isShowing())
                listMenuCtl.rebuild();
        });

        /*
         * Task-Selektion (nur User-Selektion):
         * - Details öffnen/schliessen
         * - Sizing anpassen
         */
        tasksController.setOnUserSelection((obs, oldV, newV) -> {
            if (newV == null)
                detailsController.close();
            else
                detailsController.open(newV);
            sizing.apply();
        });

        // Initiale Tasks laden (abhängig von initial selektierter Kategorie in
        // categoriesController.init())
        tasksController.refresh();

        // Theme-Menü nur konfigurieren, wenn Toggle existiert (FXML optional)
        if (tglTheme != null)
            setupThemeMenu();
    }

    /**
     * Aktualisiert Header-Text in Normal/Compact Mode.
     *
     * Text:
     * - wenn Kategorie selektiert: "<Icon> <Name>"
     * - sonst: "Aufgaben"
     *
     * Delegation:
     * - layout.updateHeaderTexts(...) setzt tasksTitleLabel und btnListMenu.
     */
    private void updateHeaderTexts() {
        Category selected = listsView.getSelectionModel().getSelectedItem();
        String title = selected != null ? (iconFor(selected) + selected.getName()) : "Aufgaben";
        layout.updateHeaderTexts(selected, title);
    }

    /**
     * Liefert Anzeige-Icon für eine Kategorie.
     *
     * Priorität:
     * 1) Icon aus DB (Category.icon), wenn gesetzt
     * 2) Fallback anhand Name (Arbeit/Schule/Privat)
     * 3) Default "📁 "
     */
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

    /**
     * FXML-Handler: "Neue Liste" Button/Menu.
     * Öffnet/Schliesst das NewList-Popup.
     */
    @FXML
    private void onNewList() {
        newListPopupController.toggleShowCentered();
    }

    /**
     * FXML-Handler: neues Todo hinzufügen.
     *
     * Verhalten:
     * - Details schliessen und Sizing anwenden, damit nach Insert keine Details
     * offen bleiben
     * - TasksController übernimmt Validierung + Insert + Refresh
     */
    @FXML
    private void onAddTask() {
        detailsController.close();
        sizing.apply();
        tasksController.onAddTask();
    }

    /**
     * FXML-Handler: "Erledigt" (History) anzeigen.
     */
    @FXML
    private void onShowHistory() {
        tasksController.showDone();
        detailsController.close();
        sizing.apply();
        tasksController.refresh();
    }

    /**
     * FXML-Handler: zurück von History zu offenen Tasks.
     */
    @FXML
    private void onBackFromHistory() {
        tasksController.showOpen();
        detailsController.close();
        sizing.apply();
        tasksController.refresh();
    }

    /**
     * FXML-Handler: alle erledigten Tasks löschen (History-Modus).
     */
    @FXML
    private void onClearDone() {
        detailsController.close();
        sizing.apply();
        tasksController.onClearDone();
    }

    /**
     * FXML-Handler: Details schliessen.
     *
     * Zusatz:
     * - Programatische Selektion löschen, damit in tasksView nichts mehr ausgewählt
     * ist.
     */
    @FXML
    private void onCloseDetails() {
        detailsController.close();
        sizing.apply();
        tasksController.clearSelectionProgrammatically();
    }

    /**
     * FXML-Handler: DueDate im Detailbereich löschen (UI).
     * Persistiert erst nach "Save".
     */
    @FXML
    private void onClearDetailsDueDate() {
        detailsController.clearDueDate();
    }

    /**
     * FXML-Handler: Details speichern.
     *
     * Ablauf:
     * - detailsController.save() führt Update aus (inkl. Validierung)
     * - bei Erfolg: Details schliessen, refresh, Selektion löschen, Fokus zurück
     * auf tasksView
     */
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

    /**
     * Baut das Theme-Auswahlmenü (ContextMenu) am ToggleButton.
     *
     * Inhalte:
     * - RadioMenuItems für alle ThemeManager.Theme Werte
     * - Auswahl wird angewendet und persistent gespeichert
     *
     * UI-Verhalten:
     * - ToggleButton öffnet/schliesst Menü
     * - ToggleButton bleibt nach Klick nicht "gedrückt" (setSelected(false))
     */
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

        // aktuelle Auswahl aus Persistenz laden und im Menü markieren
        ThemeManager.Theme current = ThemeManager.loadThemeOrDefault();
        for (var mi : themeMenu.getItems()) {
            if (mi instanceof RadioMenuItem rmi && rmi.getUserData() == current) {
                rmi.setSelected(true);
                break;
            }
        }

        // ToggleButton initial beschriften und "nicht gedrückt" setzen
        tglTheme.setText(prettyThemeName(current));
        tglTheme.setSelected(false);

        // ToggleButton öffnet/schliesst Menü, bleibt danach unselected
        tglTheme.setOnAction(e -> {
            if (themeMenu.isShowing())
                themeMenu.hide();
            else
                themeMenu.show(tglTheme, Side.BOTTOM, 0, 6);
            tglTheme.setSelected(false);
        });

        // Beim Schliessen immer unselected (auch wenn ausserhalb geklickt)
        themeMenu.setOnHidden(e -> tglTheme.setSelected(false));
    }

    /**
     * Mappt Theme-Enum auf Anzeigenamen.
     */
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
